package com.example.registrosatenciones.ui.fichapaciente;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.registrosatenciones.db.AppDatabase;
import com.example.registrosatenciones.db.dao.AtencionEnfermeriaDao;
import com.example.registrosatenciones.db.dao.AtencionOdontologiaDao;
import com.example.registrosatenciones.db.dao.DiagnosticoDao;
import com.example.registrosatenciones.db.dao.PacienteDao;
import com.example.registrosatenciones.db.dao.TipoPrestacionEnfermeriaDao;
import com.example.registrosatenciones.db.dao.TipoPrestacionOdontologiaDao;
import com.example.registrosatenciones.db.entity.DiagnosticoEntity;
import com.example.registrosatenciones.db.entity.PacienteEntity;
import com.example.registrosatenciones.db.entity.TipoPrestacionEnfermeriaEntity;
import com.example.registrosatenciones.db.entity.TipoPrestacionOdontologiaEntity;
import com.example.registrosatenciones.db.relation.AtencionConPrestaciones;
import com.example.registrosatenciones.db.relation.AtencionOdontologiaConDetalle;
import com.example.registrosatenciones.util.AppExecutors;
import com.example.registrosatenciones.util.PreferenciasUsuario;

import java.util.List;

public class FichaPacienteViewModel extends AndroidViewModel {

    private final Context context;
    private final PacienteDao pacienteDao;
    private final AtencionEnfermeriaDao atencionDao;
    private final TipoPrestacionEnfermeriaDao tipoPrestacionDao;
    private final AtencionOdontologiaDao atencionOdoDao;
    private final TipoPrestacionOdontologiaDao tipoPrestacionOdoDao;
    private final DiagnosticoDao diagnosticoDao;

    private final MutableLiveData<List<DiagnosticoEntity>> diagnosticos = new MutableLiveData<>();

    public FichaPacienteViewModel(@NonNull Application application) {
        super(application);
        context = application.getApplicationContext();
        AppDatabase db = AppDatabase.getInstancia(context);
        pacienteDao = db.pacienteDao();
        atencionDao = db.atencionEnfermeriaDao();
        tipoPrestacionDao = db.tipoPrestacionEnfermeriaDao();
        atencionOdoDao = db.atencionOdontologiaDao();
        tipoPrestacionOdoDao = db.tipoPrestacionOdontologiaDao();
        diagnosticoDao = db.diagnosticoDao();

        AppExecutors.io().execute(() -> {
            List<DiagnosticoEntity> lista = diagnosticoDao.listar();
            AppExecutors.ejecutarEnUI(() -> diagnosticos.setValue(lista));
        });
    }

    public boolean esOdontologo() {
        return "Odontólogo".equals(PreferenciasUsuario.getRol(context));
    }

    public LiveData<PacienteEntity> observarPaciente(long pacienteLocalId) {
        return pacienteDao.observar(pacienteLocalId);
    }

    public LiveData<List<AtencionConPrestaciones>> observarAtenciones(long pacienteLocalId) {
        return atencionDao.observarPorPaciente(pacienteLocalId);
    }

    public LiveData<List<TipoPrestacionEnfermeriaEntity>> observarCatalogo() {
        return tipoPrestacionDao.observarCatalogo();
    }

    public LiveData<List<AtencionOdontologiaConDetalle>> observarAtencionesOdo(long pacienteLocalId) {
        return atencionOdoDao.observarPorPaciente(pacienteLocalId);
    }

    public LiveData<List<TipoPrestacionOdontologiaEntity>> observarCatalogoOdo() {
        return tipoPrestacionOdoDao.observar();
    }

    public LiveData<List<DiagnosticoEntity>> getDiagnosticos() {
        return diagnosticos;
    }
}
