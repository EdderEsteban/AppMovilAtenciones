package com.example.registrosatenciones.ui.detalleatencionodontologia;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.registrosatenciones.db.AppDatabase;
import com.example.registrosatenciones.db.dao.AtencionOdontologiaDao;
import com.example.registrosatenciones.db.dao.DiagnosticoDao;
import com.example.registrosatenciones.db.dao.PacienteDao;
import com.example.registrosatenciones.db.dao.TipoPrestacionOdontologiaDao;
import com.example.registrosatenciones.db.entity.DiagnosticoEntity;
import com.example.registrosatenciones.db.entity.PacienteEntity;
import com.example.registrosatenciones.db.entity.TipoPrestacionOdontologiaEntity;
import com.example.registrosatenciones.db.relation.AtencionOdontologiaConDetalle;
import com.example.registrosatenciones.util.AppExecutors;

import java.util.List;

public class DetalleAtencionOdontologiaViewModel extends AndroidViewModel {

    private final PacienteDao pacienteDao;
    private final AtencionOdontologiaDao atencionDao;
    private final TipoPrestacionOdontologiaDao tipoPrestacionDao;
    private final DiagnosticoDao diagnosticoDao;

    private final MutableLiveData<List<TipoPrestacionOdontologiaEntity>> tiposPrestacion = new MutableLiveData<>();
    private final MutableLiveData<List<DiagnosticoEntity>> diagnosticos = new MutableLiveData<>();

    public DetalleAtencionOdontologiaViewModel(@NonNull Application application) {
        super(application);
        Context context = application.getApplicationContext();
        AppDatabase db = AppDatabase.getInstancia(context);
        pacienteDao = db.pacienteDao();
        atencionDao = db.atencionOdontologiaDao();
        tipoPrestacionDao = db.tipoPrestacionOdontologiaDao();
        diagnosticoDao = db.diagnosticoDao();

        AppExecutors.io().execute(() -> {
            List<TipoPrestacionOdontologiaEntity> tp = tipoPrestacionDao.listar();
            List<DiagnosticoEntity> dg = diagnosticoDao.listar();
            AppExecutors.ejecutarEnUI(() -> {
                tiposPrestacion.setValue(tp);
                diagnosticos.setValue(dg);
            });
        });
    }

    public LiveData<PacienteEntity> observarPaciente(long pacienteLocalId) {
        return pacienteDao.observar(pacienteLocalId);
    }

    public LiveData<AtencionOdontologiaConDetalle> observarDetalle(long atencionLocalId) {
        return atencionDao.observarPorLocalId(atencionLocalId);
    }

    public LiveData<List<TipoPrestacionOdontologiaEntity>> getTiposPrestacion() {
        return tiposPrestacion;
    }

    public LiveData<List<DiagnosticoEntity>> getDiagnosticos() {
        return diagnosticos;
    }
}
