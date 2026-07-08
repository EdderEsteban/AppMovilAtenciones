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
import com.example.registrosatenciones.util.AppExecutors;
import com.example.registrosatenciones.util.PreferenciasUsuario;

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

    public void guardarAtencion(long pacienteLocalId, int tipoAtencion, boolean embarazada,
                                boolean sinObraSocial, String observaciones,
                                List<PrestacionEnfermeriaEntity> prestaciones) {
        if (prestaciones == null || prestaciones.isEmpty()) {
            Toast.makeText(context, "Elegí al menos una prestación", Toast.LENGTH_SHORT).show();
            return;
        }

        AtencionEnfermeriaEntity atencion = new AtencionEnfermeriaEntity();
        atencion.setPacienteLocalId(pacienteLocalId);
        atencion.setTipoAtencion(tipoAtencion);
        atencion.setEmbarazada(embarazada);
        atencion.setSinObraSocial(sinObraSocial);
        atencion.setObservaciones(TextUtils.isEmpty(observaciones) ? null : observaciones.trim());
        atencion.setFechaRegistroLocal(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT).format(new Date()));
        atencion.setInstitucionIdCaptura(PreferenciasUsuario.getInstitucionActivaId(context));
        atencion.setSyncState(SyncEstado.PENDIENTE);

        AppExecutors.io().execute(() -> {
            atencionDao.guardarConPrestaciones(atencion, prestaciones);
            AppExecutors.ejecutarEnUI(() -> {
                Toast.makeText(context, "Atención guardada. Se sincronizará cuando haya conexión.", Toast.LENGTH_LONG).show();
                guardadoExitoso.setValue(true);
            });
        });
    }
}
