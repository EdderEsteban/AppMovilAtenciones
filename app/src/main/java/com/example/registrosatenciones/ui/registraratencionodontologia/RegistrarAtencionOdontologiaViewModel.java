package com.example.registrosatenciones.ui.registraratencionodontologia;

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
import com.example.registrosatenciones.db.dao.AtencionOdontologiaDao;
import com.example.registrosatenciones.db.dao.DiagnosticoDao;
import com.example.registrosatenciones.db.dao.PacienteDao;
import com.example.registrosatenciones.db.dao.TipoPrestacionOdontologiaDao;
import com.example.registrosatenciones.db.entity.AtencionOdontologiaEntity;
import com.example.registrosatenciones.db.entity.DiagnosticoEntity;
import com.example.registrosatenciones.db.entity.OdontogramaEstadoEntity;
import com.example.registrosatenciones.db.entity.PacienteEntity;
import com.example.registrosatenciones.db.entity.PrestacionOdontologiaEntity;
import com.example.registrosatenciones.db.entity.TipoPrestacionOdontologiaEntity;
import com.example.registrosatenciones.ui.odontologia.model.OdontogramaItem;
import com.example.registrosatenciones.ui.odontologia.model.OdontogramaModelo;
import com.example.registrosatenciones.ui.odontologia.model.ValoracionLocal;
import com.example.registrosatenciones.util.AppExecutors;
import com.example.registrosatenciones.util.PreferenciasUsuario;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RegistrarAtencionOdontologiaViewModel extends AndroidViewModel {

    private final Context context;
    private final PacienteDao pacienteDao;
    private final AtencionOdontologiaDao atencionDao;
    private final TipoPrestacionOdontologiaDao tipoPrestacionDao;
    private final DiagnosticoDao diagnosticoDao;

    private final OdontogramaModelo modelo = new OdontogramaModelo();
    private final MutableLiveData<ValoracionLocal> cpo = new MutableLiveData<>(new ValoracionLocal());
    private final MutableLiveData<Boolean> odontogramaActualizado = new MutableLiveData<>();
    private final MutableLiveData<List<DiagnosticoEntity>> diagnosticos = new MutableLiveData<>();
    private final MutableLiveData<Boolean> guardadoExitoso = new MutableLiveData<>();

    public RegistrarAtencionOdontologiaViewModel(@NonNull Application application) {
        super(application);
        context = application.getApplicationContext();
        AppDatabase db = AppDatabase.getInstancia(context);
        pacienteDao = db.pacienteDao();
        atencionDao = db.atencionOdontologiaDao();
        tipoPrestacionDao = db.tipoPrestacionOdontologiaDao();
        diagnosticoDao = db.diagnosticoDao();

        AppExecutors.io().execute(() -> {
            List<DiagnosticoEntity> lista = diagnosticoDao.listar();
            AppExecutors.ejecutarEnUI(() -> diagnosticos.setValue(lista));
        });
    }

    public LiveData<PacienteEntity> observarPaciente(long pacienteLocalId) {
        return pacienteDao.observar(pacienteLocalId);
    }

    public LiveData<List<TipoPrestacionOdontologiaEntity>> observarCatalogoPrestaciones() {
        return tipoPrestacionDao.observar();
    }

    public LiveData<List<DiagnosticoEntity>> getDiagnosticos() {
        return diagnosticos;
    }

    public OdontogramaModelo getModelo() {
        return modelo;
    }

    public LiveData<ValoracionLocal> getCpo() {
        return cpo;
    }

    public LiveData<Boolean> getOdontogramaActualizado() {
        return odontogramaActualizado;
    }

    public LiveData<Boolean> getGuardadoExitoso() {
        return guardadoExitoso;
    }

    // Llamado al volver del EditorCuadranteActivity con el odontograma modificado.
    public void actualizarCuadrante(List<OdontogramaItem> nuevosEstados) {
        modelo.cargar(nuevosEstados);
        cpo.setValue(modelo.calcularCpo());
        odontogramaActualizado.setValue(true);
    }

    public void guardarAtencion(long pacienteLocalId, int tipoConsulta, Integer tipoTurno, Integer diagnosticoId,
                                 boolean embarazada, boolean sinObraSocial, String nuevaObraSocial,
                                 String observaciones, List<PrestacionOdontologiaEntity> prestaciones) {
        if (tipoTurno == null) {
            Toast.makeText(context, "Seleccioná el tipo de turno", Toast.LENGTH_SHORT).show();
            return;
        }
        if (diagnosticoId == null) {
            Toast.makeText(context, "Seleccioná un diagnóstico", Toast.LENGTH_SHORT).show();
            return;
        }
        if (prestaciones == null || prestaciones.isEmpty()) {
            Toast.makeText(context, "Elegí al menos una prestación", Toast.LENGTH_SHORT).show();
            return;
        }

        String nuevaObraSocialLimpia = TextUtils.isEmpty(nuevaObraSocial) ? null : nuevaObraSocial.trim();
        ValoracionLocal valoracion = modelo.calcularCpo();

        AtencionOdontologiaEntity atencion = new AtencionOdontologiaEntity();
        atencion.setPacienteLocalId(pacienteLocalId);
        atencion.setTipoConsulta(tipoConsulta);
        atencion.setTipoTurno(tipoTurno);
        atencion.setDiagnosticoId(diagnosticoId);
        atencion.setEmbarazada(embarazada);
        atencion.setSinObraSocial(sinObraSocial);
        atencion.setNuevaObraSocial(sinObraSocial ? null : nuevaObraSocialLimpia);
        atencion.setObservaciones(TextUtils.isEmpty(observaciones) ? null : observaciones.trim());
        atencion.setFechaRegistroLocal(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT).format(new Date()));
        atencion.setInstitucionIdCaptura(PreferenciasUsuario.getInstitucionActivaId(context));
        atencion.setSyncState(SyncEstado.PENDIENTE);
        atencion.setCariesPerm(valoracion.cariesPerm);
        atencion.setPerdidosPerm(valoracion.perdidosPerm);
        atencion.setObturadosPerm(valoracion.obturadosPerm);
        atencion.setCariesTemp(valoracion.cariesTemp);
        atencion.setExtraccionTemp(valoracion.extraccionTemp);
        atencion.setObturadosTemp(valoracion.obturadosTemp);

        List<OdontogramaEstadoEntity> estados = new ArrayList<>();
        for (OdontogramaItem item : modelo.aplanar()) {
            OdontogramaEstadoEntity e = new OdontogramaEstadoEntity();
            e.setNumeroDiente(item.getNumeroDiente());
            e.setSuperficie(item.getSuperficie());
            e.setEstado(item.getEstado());
            estados.add(e);
        }

        AppExecutors.io().execute(() -> {
            atencionDao.guardarConDetalle(atencion, prestaciones, estados);

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
}
