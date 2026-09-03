package com.example.registrosatenciones.ui.registraratencion;

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.registrosatenciones.db.AppDatabase;
import com.example.registrosatenciones.db.SyncEstado;
import com.example.registrosatenciones.db.dao.AtencionEnfermeriaDao;
import com.example.registrosatenciones.db.dao.PacienteDao;
import com.example.registrosatenciones.db.dao.TipoPrestacionEnfermeriaDao;
import com.example.registrosatenciones.db.entity.AtencionEnfermeriaEntity;
import com.example.registrosatenciones.db.entity.PacienteEntity;
import com.example.registrosatenciones.db.entity.PrestacionEnfermeriaEntity;
import com.example.registrosatenciones.db.entity.TipoPrestacionEnfermeriaEntity;
import com.example.registrosatenciones.db.relation.AtencionConPrestaciones;
import com.example.registrosatenciones.util.AppExecutors;
import com.example.registrosatenciones.util.PreferenciasUsuario;
import com.example.registrosatenciones.util.VentanaEdicion;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RegistrarAtencionViewModel extends AndroidViewModel {

    private final Context context;
    private final PacienteDao pacienteDao;
    private final AtencionEnfermeriaDao atencionDao;
    private final TipoPrestacionEnfermeriaDao tipoPrestacionDao;

    private final MutableLiveData<Boolean> guardadoExitoso = new MutableLiveData<>();
    private final MutableLiveData<AtencionConPrestaciones> atencionEnEdicion = new MutableLiveData<>();

    public RegistrarAtencionViewModel(@NonNull Application application) {
        super(application);
        context = application.getApplicationContext();
        AppDatabase db = AppDatabase.getInstancia(context);
        pacienteDao = db.pacienteDao();
        atencionDao = db.atencionEnfermeriaDao();
        tipoPrestacionDao = db.tipoPrestacionEnfermeriaDao();
    }

    public LiveData<PacienteEntity> observarPaciente(long pacienteLocalId) {
        return pacienteDao.observar(pacienteLocalId);
    }

    public LiveData<List<TipoPrestacionEnfermeriaEntity>> observarCatalogo() {
        return tipoPrestacionDao.observarCatalogo();
    }

    public LiveData<Boolean> getGuardadoExitoso() {
        return guardadoExitoso;
    }

    public LiveData<AtencionConPrestaciones> getAtencionEnEdicion() {
        return atencionEnEdicion;
    }

    // Carga la atención a editar. Si la ventana ya venció, no la publica: la
    // pantalla queda como un alta normal y el llamador ya avisó al usuario.
    public void cargarParaEditar(long atencionLocalId) {
        AppExecutors.io().execute(() -> {
            AtencionConPrestaciones cargada = atencionDao.obtenerConPrestaciones(atencionLocalId);
            AppExecutors.ejecutarEnUI(() -> atencionEnEdicion.setValue(cargada));
        });
    }

    public void guardarAtencion(long pacienteLocalId, int tipoAtencion, boolean embarazada,
                                boolean sinObraSocial, String observaciones, String nuevaObraSocial,
                                List<PrestacionEnfermeriaEntity> prestaciones) {
        if (prestaciones == null || prestaciones.isEmpty()) {
            Toast.makeText(context, "Elegí al menos una prestación", Toast.LENGTH_SHORT).show();
            return;
        }

        String nuevaObraSocialLimpia = TextUtils.isEmpty(nuevaObraSocial) ? null : nuevaObraSocial.trim();

        AtencionEnfermeriaEntity atencion = new AtencionEnfermeriaEntity();
        atencion.setPacienteLocalId(pacienteLocalId);
        atencion.setTipoAtencion(tipoAtencion);
        atencion.setEmbarazada(embarazada);
        atencion.setSinObraSocial(sinObraSocial);
        atencion.setNuevaObraSocial(sinObraSocial ? null : nuevaObraSocialLimpia);
        atencion.setObservaciones(TextUtils.isEmpty(observaciones) ? null : observaciones.trim());
        atencion.setFechaRegistroLocal(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT).format(new Date()));
        atencion.setInstitucionIdCaptura(PreferenciasUsuario.getInstitucionActivaId(context));
        atencion.setSyncState(SyncEstado.PENDIENTE);

        AppExecutors.io().execute(() -> {
            atencionDao.guardarConPrestaciones(atencion, prestaciones);

            // Igual que el backend: si se cargó una obra social nueva, queda en el registro del paciente
            // para que la próxima atención ya no la vuelva a pedir.
            if (!sinObraSocial && nuevaObraSocialLimpia != null) {
                PacienteEntity paciente = pacienteDao.obtenerPorLocalId(pacienteLocalId);
                if (paciente != null && (paciente.getObraSocial() == null || paciente.getObraSocial().isEmpty())) {
                    paciente.setObraSocial(nuevaObraSocialLimpia);
                    pacienteDao.actualizar(paciente);
                }
            }

            AppExecutors.ejecutarEnUI(() -> {
                Toast.makeText(context, "Atención guardada. Se sincronizará cuando haya conexión.", Toast.LENGTH_LONG).show();
                guardadoExitoso.setValue(true);
            });
        });
    }

    public void actualizarAtencion(long atencionLocalId, int tipoAtencion, boolean embarazada,
                                   boolean sinObraSocial, String observaciones,
                                   List<PrestacionEnfermeriaEntity> prestaciones) {
        if (prestaciones == null || prestaciones.isEmpty()) {
            Toast.makeText(context, "Elegí al menos una prestación", Toast.LENGTH_SHORT).show();
            return;
        }

        AppExecutors.io().execute(() -> {
            AtencionConPrestaciones actual = atencionDao.obtenerConPrestaciones(atencionLocalId);
            if (actual == null) return;
            AtencionEnfermeriaEntity atencion = actual.getAtencion();

            // La ventana se vuelve a comprobar acá, contra el dato y no contra la
            // pantalla: entre que se abrió el formulario y se presionó Guardar
            // pueden haber pasado los 15 minutos.
            if (!VentanaEdicion.estaAbierta(atencion.getFechaRegistroLocal())) {
                AppExecutors.ejecutarEnUI(() -> Toast.makeText(context,
                        "Pasaron los 15 minutos: la atención ya no se puede editar.",
                        Toast.LENGTH_LONG).show());
                return;
            }

            atencion.setTipoAtencion(tipoAtencion);
            atencion.setEmbarazada(embarazada);
            atencion.setSinObraSocial(sinObraSocial);
            atencion.setObservaciones(TextUtils.isEmpty(observaciones) ? null : observaciones.trim());
            // fechaRegistroLocal NO se toca: si se actualizara, cada corrección
            // reabriría la ventana y nunca cerraría.

            atencionDao.actualizarConPrestaciones(atencion, prestaciones);

            AppExecutors.ejecutarEnUI(() -> {
                Toast.makeText(context, "Atención actualizada.", Toast.LENGTH_SHORT).show();
                guardadoExitoso.setValue(true);
            });
        });
    }
}
