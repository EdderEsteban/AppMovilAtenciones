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
import com.example.registrosatenciones.db.relation.AtencionOdontologiaConDetalle;
import com.example.registrosatenciones.ui.odontologia.model.OdontogramaItem;
import com.example.registrosatenciones.ui.odontologia.model.OdontogramaModelo;
import com.example.registrosatenciones.ui.odontologia.model.ValoracionLocal;
import com.example.registrosatenciones.util.AppExecutors;
import com.example.registrosatenciones.util.PreferenciasUsuario;
import com.example.registrosatenciones.util.VentanaEdicion;

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
    private final MutableLiveData<AtencionOdontologiaConDetalle> atencionEnEdicion = new MutableLiveData<>();

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

    public LiveData<AtencionOdontologiaConDetalle> getAtencionEnEdicion() {
        return atencionEnEdicion;
    }

    // Llamado al volver del EditorCuadranteActivity con el odontograma modificado.
    public void actualizarCuadrante(List<OdontogramaItem> nuevosEstados) {
        modelo.cargar(nuevosEstados);
        cpo.setValue(modelo.calcularCpo());
        odontogramaActualizado.setValue(true);
    }

    // Precarga el odontograma de la última atención del paciente al abrir una atención nueva
    // (igual que el GET Create de la web): el odontólogo modifica sobre el histórico.
    public void precargarUltimoOdontograma(long pacienteLocalId) {
        AppExecutors.io().execute(() -> {
            List<OdontogramaEstadoEntity> estados = atencionDao.estadosDelUltimoOdontograma(pacienteLocalId);
            List<OdontogramaItem> items = new ArrayList<>();
            for (OdontogramaEstadoEntity e : estados) {
                items.add(new OdontogramaItem(e.getNumeroDiente(), e.getSuperficie(), e.getEstado()));
            }
            AppExecutors.ejecutarEnUI(() -> {
                // No pisar si no hay histórico o si el usuario ya empezó a editar.
                if (items.isEmpty() || !modelo.aplanar().isEmpty()) return;
                modelo.cargar(items);
                cpo.setValue(modelo.calcularCpo());
                odontogramaActualizado.setValue(true);
            });
        });
    }

    // Carga la atención a editar CON SU PROPIO odontograma. Ojo: no sirve
    // precargarUltimoOdontograma, que trae el de la última atención del paciente y
    // puede no ser esta.
    public void cargarParaEditar(long atencionLocalId) {
        AppExecutors.io().execute(() -> {
            AtencionOdontologiaConDetalle cargada = atencionDao.obtenerConDetalle(atencionLocalId);
            if (cargada == null) return;

            List<OdontogramaItem> items = new ArrayList<>();
            for (OdontogramaEstadoEntity e : cargada.getEstados()) {
                items.add(new OdontogramaItem(e.getNumeroDiente(), e.getSuperficie(), e.getEstado()));
            }

            AppExecutors.ejecutarEnUI(() -> {
                modelo.cargar(items);
                cpo.setValue(modelo.calcularCpo());
                odontogramaActualizado.setValue(true);
                atencionEnEdicion.setValue(cargada);
            });
        });
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

    public void actualizarAtencion(long atencionLocalId, int tipoConsulta, Integer tipoTurno,
                                   Integer diagnosticoId, boolean embarazada, boolean sinObraSocial,
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

        ValoracionLocal valoracion = modelo.calcularCpo();
        List<OdontogramaEstadoEntity> estados = new ArrayList<>();
        for (OdontogramaItem item : modelo.aplanar()) {
            OdontogramaEstadoEntity e = new OdontogramaEstadoEntity();
            e.setNumeroDiente(item.getNumeroDiente());
            e.setSuperficie(item.getSuperficie());
            e.setEstado(item.getEstado());
            estados.add(e);
        }

        AppExecutors.io().execute(() -> {
            AtencionOdontologiaConDetalle actual = atencionDao.obtenerConDetalle(atencionLocalId);
            if (actual == null) return;
            AtencionOdontologiaEntity atencion = actual.getAtencion();

            // La ventana se vuelve a comprobar acá, contra el dato y no contra la
            // pantalla: entre que se abrió el formulario y se presionó Guardar
            // pueden haber pasado los 15 minutos.
            if (!VentanaEdicion.estaAbierta(atencion.getFechaRegistroLocal())) {
                AppExecutors.ejecutarEnUI(() -> Toast.makeText(context,
                        "Pasaron los 15 minutos: la atención ya no se puede editar.",
                        Toast.LENGTH_LONG).show());
                return;
            }

            atencion.setTipoConsulta(tipoConsulta);
            atencion.setTipoTurno(tipoTurno);
            atencion.setDiagnosticoId(diagnosticoId);
            atencion.setEmbarazada(embarazada);
            atencion.setSinObraSocial(sinObraSocial);
            atencion.setObservaciones(TextUtils.isEmpty(observaciones) ? null : observaciones.trim());
            atencion.setCariesPerm(valoracion.cariesPerm);
            atencion.setPerdidosPerm(valoracion.perdidosPerm);
            atencion.setObturadosPerm(valoracion.obturadosPerm);
            atencion.setCariesTemp(valoracion.cariesTemp);
            atencion.setExtraccionTemp(valoracion.extraccionTemp);
            atencion.setObturadosTemp(valoracion.obturadosTemp);
            // fechaRegistroLocal NO se toca: si se actualizara, cada corrección
            // reabriría la ventana y nunca cerraría.

            atencionDao.actualizarConDetalle(atencion, prestaciones, estados);

            AppExecutors.ejecutarEnUI(() -> {
                Toast.makeText(context, "Atención actualizada.", Toast.LENGTH_SHORT).show();
                guardadoExitoso.setValue(true);
            });
        });
    }
}
