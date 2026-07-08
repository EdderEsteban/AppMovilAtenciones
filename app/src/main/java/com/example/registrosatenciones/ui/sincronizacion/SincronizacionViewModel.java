package com.example.registrosatenciones.ui.sincronizacion;

import android.app.Application;
import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.registrosatenciones.db.AppDatabase;
import com.example.registrosatenciones.db.SyncEstado;
import com.example.registrosatenciones.db.dao.AtencionEnfermeriaDao;
import com.example.registrosatenciones.db.dao.PacienteDao;
import com.example.registrosatenciones.db.entity.AtencionEnfermeriaEntity;
import com.example.registrosatenciones.db.entity.PacienteEntity;
import com.example.registrosatenciones.db.entity.PrestacionEnfermeriaEntity;
import com.example.registrosatenciones.request.ApiClient;
import com.example.registrosatenciones.request.CrearAtencionEnfermeriaRequest;
import com.example.registrosatenciones.request.CrearPacienteRequest;
import com.example.registrosatenciones.request.PrestacionItemRequest;
import com.example.registrosatenciones.response.CrearAtencionResponse;
import com.example.registrosatenciones.response.CrearPacienteResponse;
import com.example.registrosatenciones.response.PacienteResponse;
import com.example.registrosatenciones.util.AppExecutors;
import com.example.registrosatenciones.util.Conectividad;
import com.example.registrosatenciones.util.PreferenciasUsuario;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Response;

public class SincronizacionViewModel extends AndroidViewModel {

    private final Context context;
    private final PacienteDao pacienteDao;
    private final AtencionEnfermeriaDao atencionDao;

    private final MutableLiveData<Boolean> sincronizando = new MutableLiveData<>(false);

    public SincronizacionViewModel(@NonNull Application application) {
        super(application);
        context = application.getApplicationContext();
        AppDatabase db = AppDatabase.getInstancia(context);
        pacienteDao = db.pacienteDao();
        atencionDao = db.atencionEnfermeriaDao();
    }

    public LiveData<List<PacienteEntity>> observarPacientesPendientes() {
        return pacienteDao.observarPorEstado(SyncEstado.PENDIENTE);
    }

    public LiveData<List<AtencionEnfermeriaEntity>> observarAtencionesPendientes() {
        int institucionActiva = PreferenciasUsuario.getInstitucionActivaId(context);
        return atencionDao.observarPorEstadoEInstitucion(SyncEstado.PENDIENTE, institucionActiva);
    }

    public LiveData<Boolean> getSincronizando() {
        return sincronizando;
    }

    public void sincronizarAhora() {
        if (!Conectividad.hayConexion(context)) {
            Toast.makeText(context, "Sin conexión — no se puede sincronizar ahora", Toast.LENGTH_SHORT).show();
            return;
        }

        sincronizando.setValue(true);
        AppExecutors.io().execute(this::ejecutarSincronizacion);
    }

    private void ejecutarSincronizacion() {
        String token = PreferenciasUsuario.getAuthHeader(context);
        int institucionActiva = PreferenciasUsuario.getInstitucionActivaId(context);

        List<PacienteEntity> pacientesPendientes = pacienteDao.listarPorEstado(SyncEstado.PENDIENTE);
        for (PacienteEntity paciente : pacientesPendientes) {
            sincronizarPaciente(paciente, token);
        }

        List<AtencionEnfermeriaEntity> atencionesPendientes = atencionDao.listarPorEstado(SyncEstado.PENDIENTE);
        for (AtencionEnfermeriaEntity atencion : atencionesPendientes) {
            if (atencion.getInstitucionIdCaptura() != institucionActiva) {
                continue; // corresponde a otra institución; se sincroniza cuando el usuario vuelva ahí
            }
            sincronizarAtencion(atencion, token);
        }

        AppExecutors.ejecutarEnUI(() -> {
            sincronizando.setValue(false);
            Toast.makeText(context, "Sincronización finalizada", Toast.LENGTH_SHORT).show();
        });
    }

    private void sincronizarPaciente(PacienteEntity paciente, String token) {
        try {
            Response<List<PacienteResponse>> busqueda =
                    ApiClient.getApiAtenciones().buscarPacientes(token, paciente.getDni()).execute();

            if (!busqueda.isSuccessful() || busqueda.body() == null) return; // se reintenta la próxima vez

            Integer serverId = null;
            for (PacienteResponse candidato : busqueda.body()) {
                if (candidato.getDni().equals(paciente.getDni())) {
                    serverId = candidato.getId();
                    break;
                }
            }

            if (serverId == null) {
                CrearPacienteRequest request = new CrearPacienteRequest(
                        paciente.getDni(), paciente.getApellido(), paciente.getNombre(),
                        paciente.getFechaNacimiento(), paciente.getSexo(),
                        paciente.getDomicilio(), paciente.getTelefono(), paciente.getObraSocial());

                Response<CrearPacienteResponse> creacion =
                        ApiClient.getApiAtenciones().crearPaciente(token, request).execute();

                if (!creacion.isSuccessful() || creacion.body() == null) return; // se reintenta la próxima vez
                serverId = creacion.body().getId();
            }

            paciente.setServerId(serverId);
            paciente.setSyncState(SyncEstado.SINCRONIZADO);
            pacienteDao.actualizar(paciente);

        } catch (IOException e) {
            // se cortó la conexión a mitad de camino: queda PENDIENTE, se reintenta la próxima vez
        }
    }

    private void sincronizarAtencion(AtencionEnfermeriaEntity atencion, String token) {
        PacienteEntity paciente = pacienteDao.obtenerPorLocalId(atencion.getPacienteLocalId());
        if (paciente == null || paciente.getServerId() == null) {
            return; // el paciente todavía no se sincronizó; se reintenta la próxima vez
        }

        List<PrestacionEnfermeriaEntity> prestacionesLocales = atencionDao.prestacionesDe(atencion.getLocalId());
        List<PrestacionItemRequest> prestaciones = new ArrayList<>();
        for (PrestacionEnfermeriaEntity p : prestacionesLocales) {
            prestaciones.add(new PrestacionItemRequest(p.getTipoPrestacionId(), p.getCantidad()));
        }

        CrearAtencionEnfermeriaRequest request = new CrearAtencionEnfermeriaRequest();
        request.setPacienteId(paciente.getServerId());
        request.setTipoAtencion(atencion.getTipoAtencion());
        request.setEmbarazada(atencion.isEmbarazada());
        request.setSinObraSocial(atencion.isSinObraSocial());
        request.setNuevaObraSocial(atencion.getNuevaObraSocial());
        request.setObservaciones(atencion.getObservaciones());
        request.setPrestaciones(prestaciones);

        try {
            Response<CrearAtencionResponse> respuesta =
                    ApiClient.getApiAtenciones().crearAtencionEnfermeria(token, request).execute();

            if (respuesta.isSuccessful() && respuesta.body() != null) {
                atencion.setServerId(respuesta.body().getId());
                atencion.setSyncState(SyncEstado.SINCRONIZADO);
                atencionDao.actualizar(atencion);
            }
            // si no fue exitosa, queda PENDIENTE y se reintenta la próxima vez

        } catch (IOException e) {
            // sin conexión a mitad de camino: queda PENDIENTE
        }
    }
}
