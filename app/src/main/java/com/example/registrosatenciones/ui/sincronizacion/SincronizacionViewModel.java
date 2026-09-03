package com.example.registrosatenciones.ui.sincronizacion;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.registrosatenciones.db.AppDatabase;
import com.example.registrosatenciones.db.SyncEstado;
import com.example.registrosatenciones.db.dao.AtencionEnfermeriaDao;
import com.example.registrosatenciones.db.dao.AtencionOdontologiaDao;
import com.example.registrosatenciones.db.dao.PacienteDao;
import com.example.registrosatenciones.db.entity.AtencionEnfermeriaEntity;
import com.example.registrosatenciones.db.entity.AtencionOdontologiaEntity;
import com.example.registrosatenciones.db.entity.OdontogramaEstadoEntity;
import com.example.registrosatenciones.db.entity.PacienteEntity;
import com.example.registrosatenciones.db.entity.PrestacionEnfermeriaEntity;
import com.example.registrosatenciones.db.entity.PrestacionOdontologiaEntity;
import com.example.registrosatenciones.request.ApiClient;
import com.example.registrosatenciones.request.CrearAtencionEnfermeriaRequest;
import com.example.registrosatenciones.request.CrearAtencionOdontologiaRequest;
import com.example.registrosatenciones.request.CrearPacienteRequest;
import com.example.registrosatenciones.request.OdontogramaEstadoItemRequest;
import com.example.registrosatenciones.request.PrestacionItemRequest;
import com.example.registrosatenciones.response.CrearAtencionResponse;
import com.example.registrosatenciones.response.CrearPacienteResponse;
import com.example.registrosatenciones.response.PacienteResponse;
import com.example.registrosatenciones.ui.login.LoginActivity;
import com.example.registrosatenciones.util.AppExecutors;
import com.example.registrosatenciones.util.Conectividad;
import com.example.registrosatenciones.util.PreferenciasUsuario;
import com.example.registrosatenciones.util.VentanaEdicion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Response;

public class SincronizacionViewModel extends AndroidViewModel {

    private final Context context;
    private final PacienteDao pacienteDao;
    private final AtencionEnfermeriaDao atencionDao;
    private final AtencionOdontologiaDao atencionOdoDao;

    private final MutableLiveData<Boolean> sincronizando = new MutableLiveData<>(false);

    public SincronizacionViewModel(@NonNull Application application) {
        super(application);
        context = application.getApplicationContext();
        AppDatabase db = AppDatabase.getInstancia(context);
        pacienteDao = db.pacienteDao();
        atencionDao = db.atencionEnfermeriaDao();
        atencionOdoDao = db.atencionOdontologiaDao();
    }

    public LiveData<List<PacienteEntity>> observarPacientesPendientes() {
        return pacienteDao.observarPorEstado(SyncEstado.PENDIENTE);
    }

    public LiveData<List<PacienteEntity>> observarPacientesConError() {
        return pacienteDao.observarPorEstado(SyncEstado.ERROR);
    }

    public LiveData<List<AtencionEnfermeriaEntity>> observarAtencionesPendientes() {
        int institucionActiva = PreferenciasUsuario.getInstitucionActivaId(context);
        return atencionDao.observarPorEstadoEInstitucion(SyncEstado.PENDIENTE, institucionActiva);
    }

    public LiveData<List<AtencionEnfermeriaEntity>> observarAtencionesConError() {
        int institucionActiva = PreferenciasUsuario.getInstitucionActivaId(context);
        return atencionDao.observarPorEstadoEInstitucion(SyncEstado.ERROR, institucionActiva);
    }

    public LiveData<List<AtencionOdontologiaEntity>> observarAtencionesOdoPendientes() {
        int institucionActiva = PreferenciasUsuario.getInstitucionActivaId(context);
        return atencionOdoDao.observarPorEstadoEInstitucion(SyncEstado.PENDIENTE, institucionActiva);
    }

    public LiveData<List<AtencionOdontologiaEntity>> observarAtencionesOdoConError() {
        int institucionActiva = PreferenciasUsuario.getInstitucionActivaId(context);
        return atencionOdoDao.observarPorEstadoEInstitucion(SyncEstado.ERROR, institucionActiva);
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
        String fechaCorte = VentanaEdicion.fechaCorte();

        List<PacienteEntity> pacientesPendientes = pacienteDao.listarPorEstado(SyncEstado.PENDIENTE);
        for (PacienteEntity paciente : pacientesPendientes) {
            boolean sesionValida = sincronizarPaciente(paciente, token);
            if (!sesionValida) {
                finalizarSincronizacion();
                return;
            }
        }

        List<AtencionEnfermeriaEntity> atencionesPendientes =
                atencionDao.listarEnviables(SyncEstado.PENDIENTE, fechaCorte);
        for (AtencionEnfermeriaEntity atencion : atencionesPendientes) {
            if (atencion.getInstitucionIdCaptura() != institucionActiva) {
                continue; // corresponde a otra institución; se sincroniza cuando el usuario vuelva ahí
            }
            boolean sesionValida = sincronizarAtencion(atencion, token);
            if (!sesionValida) {
                finalizarSincronizacion();
                return;
            }
        }

        List<AtencionOdontologiaEntity> atencionesOdoPendientes =
                atencionOdoDao.listarEnviables(SyncEstado.PENDIENTE, fechaCorte);
        for (AtencionOdontologiaEntity atencion : atencionesOdoPendientes) {
            if (atencion.getInstitucionIdCaptura() != institucionActiva) {
                continue; // corresponde a otra institución; se sincroniza cuando el usuario vuelva ahí
            }
            boolean sesionValida = sincronizarAtencionOdo(atencion, token);
            if (!sesionValida) {
                finalizarSincronizacion();
                return;
            }
        }

        finalizarSincronizacion();
    }

    private void finalizarSincronizacion() {
        AppExecutors.ejecutarEnUI(() -> {
            sincronizando.setValue(false);
            Toast.makeText(context, "Sincronización finalizada", Toast.LENGTH_SHORT).show();
        });
    }

    // Devuelve false si la sesión perdió la institución (409): no tiene sentido seguir intentando.
    private boolean sincronizarPaciente(PacienteEntity paciente, String token) {
        try {
            Response<List<PacienteResponse>> busqueda =
                    ApiClient.getApiAtenciones().buscarPacientes(token, paciente.getDni()).execute();

            if (!busqueda.isSuccessful() || busqueda.body() == null) return true; // se reintenta la próxima vez

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

                if (creacion.isSuccessful() && creacion.body() != null) {
                    serverId = creacion.body().getId();
                } else if (creacion.code() == 400) {
                    marcarError(pacienteDao, paciente); // ej: DNI duplicado — no se resuelve reintentando
                    return true;
                } else {
                    return true; // falla transitoria: se reintenta la próxima vez
                }
            }

            paciente.setServerId(serverId);
            paciente.setSyncState(SyncEstado.SINCRONIZADO);
            pacienteDao.actualizar(paciente);
            return true;

        } catch (IOException e) {
            return true; // se cortó la conexión a mitad de camino: queda PENDIENTE, se reintenta
        }
    }

    // Devuelve false si la sesión perdió la institución (409): no tiene sentido seguir intentando.
    private boolean sincronizarAtencion(AtencionEnfermeriaEntity atencion, String token) {
        PacienteEntity paciente = pacienteDao.obtenerPorLocalId(atencion.getPacienteLocalId());
        if (paciente == null || paciente.getServerId() == null) {
            return true; // el paciente todavía no se sincronizó; se reintenta la próxima vez
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
            } else if (respuesta.code() == 409) {
                AppExecutors.ejecutarEnUI(this::irALoginPorConflicto);
                return false;
            } else if (respuesta.code() == 400) {
                marcarError(atencionDao, atencion); // error de negocio — no se resuelve reintentando
            }
            // otros códigos: queda PENDIENTE y se reintenta la próxima vez

            return true;

        } catch (IOException e) {
            return true; // sin conexión a mitad de camino: queda PENDIENTE
        }
    }

    // Devuelve false si la sesión perdió la institución (409): no tiene sentido seguir intentando.
    private boolean sincronizarAtencionOdo(AtencionOdontologiaEntity atencion, String token) {
        PacienteEntity paciente = pacienteDao.obtenerPorLocalId(atencion.getPacienteLocalId());
        if (paciente == null || paciente.getServerId() == null) {
            return true; // el paciente todavía no se sincronizó; se reintenta la próxima vez
        }

        List<PrestacionOdontologiaEntity> prestacionesLocales = atencionOdoDao.prestacionesDe(atencion.getLocalId());
        List<PrestacionItemRequest> prestaciones = new ArrayList<>();
        for (PrestacionOdontologiaEntity p : prestacionesLocales) {
            prestaciones.add(new PrestacionItemRequest(p.getTipoPrestacionId(), p.getCantidad()));
        }

        List<OdontogramaEstadoEntity> estadosLocales = atencionOdoDao.estadosDe(atencion.getLocalId());
        List<OdontogramaEstadoItemRequest> odontograma = new ArrayList<>();
        for (OdontogramaEstadoEntity e : estadosLocales) {
            odontograma.add(new OdontogramaEstadoItemRequest(e.getNumeroDiente(), e.getSuperficie(), e.getEstado()));
        }

        CrearAtencionOdontologiaRequest request = new CrearAtencionOdontologiaRequest();
        request.setPacienteId(paciente.getServerId());
        request.setTipoConsulta(atencion.getTipoConsulta());
        request.setTipoTurno(atencion.getTipoTurno());
        request.setDiagnosticoId(atencion.getDiagnosticoId());
        request.setEmbarazada(atencion.isEmbarazada());
        request.setSinObraSocial(atencion.isSinObraSocial());
        request.setNuevaObraSocial(atencion.getNuevaObraSocial());
        request.setObservaciones(atencion.getObservaciones());
        request.setPrestaciones(prestaciones);
        request.setOdontograma(odontograma);

        try {
            Response<CrearAtencionResponse> respuesta =
                    ApiClient.getApiAtenciones().crearAtencionOdontologia(token, request).execute();

            if (respuesta.isSuccessful() && respuesta.body() != null) {
                atencion.setServerId(respuesta.body().getId());
                atencion.setSyncState(SyncEstado.SINCRONIZADO);
                atencionOdoDao.actualizar(atencion);
            } else if (respuesta.code() == 409) {
                AppExecutors.ejecutarEnUI(this::irALoginPorConflicto);
                return false;
            } else if (respuesta.code() == 400) {
                marcarError(atencionOdoDao, atencion); // error de negocio — no se resuelve reintentando
            }
            // otros códigos: queda PENDIENTE y se reintenta la próxima vez

            return true;

        } catch (IOException e) {
            return true; // sin conexión a mitad de camino: queda PENDIENTE
        }
    }

    private void marcarError(PacienteDao dao, PacienteEntity paciente) {
        paciente.setSyncState(SyncEstado.ERROR);
        dao.actualizar(paciente);
    }

    private void marcarError(AtencionEnfermeriaDao dao, AtencionEnfermeriaEntity atencion) {
        atencion.setSyncState(SyncEstado.ERROR);
        dao.actualizar(atencion);
    }

    private void marcarError(AtencionOdontologiaDao dao, AtencionOdontologiaEntity atencion) {
        atencion.setSyncState(SyncEstado.ERROR);
        dao.actualizar(atencion);
    }

    private void irALoginPorConflicto() {
        Toast.makeText(context, "Tu sesión perdió la institución activa. Volvé a iniciar sesión.", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}
