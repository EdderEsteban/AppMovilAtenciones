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
import com.example.registrosatenciones.db.dao.ObraSocialDao;
import com.example.registrosatenciones.db.dao.PacienteDao;
import com.example.registrosatenciones.db.dao.TipoPrestacionOdontologiaDao;
import com.example.registrosatenciones.db.entity.AtencionOdontologiaEntity;
import com.example.registrosatenciones.db.entity.DiagnosticoEntity;
import com.example.registrosatenciones.db.entity.ObraSocialEntity;
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
import java.util.HashMap;
import java.util.Map;

public class RegistrarAtencionOdontologiaViewModel extends AndroidViewModel {

    // Catálogo de turnos: única fuente de verdad para código <-> etiqueta. La
    // Activity lo usa solo para armar el adapter del dropdown; la resolución
    // (código -> etiqueta) es responsabilidad de este ViewModel.
    public static final String[] TURNO_LABELS = {"Ventanilla", "Profesional", "Demanda espontánea", "Interdisciplinario"};
    public static final int[] TURNO_CODIGOS = {1, 2, 3, 4};

    private final Context context;
    private final PacienteDao pacienteDao;
    private final AtencionOdontologiaDao atencionDao;
    private final TipoPrestacionOdontologiaDao tipoPrestacionDao;
    private final DiagnosticoDao diagnosticoDao;
    private final ObraSocialDao obraSocialDao;

    private final OdontogramaModelo modelo = new OdontogramaModelo();
    private final MutableLiveData<ValoracionLocal> cpo = new MutableLiveData<>(new ValoracionLocal());
    private final MutableLiveData<Boolean> odontogramaActualizado = new MutableLiveData<>();
    private final MutableLiveData<List<DiagnosticoEntity>> diagnosticos = new MutableLiveData<>();
    private final MutableLiveData<List<ObraSocialEntity>> obrasSociales = new MutableLiveData<>();
    private final MutableLiveData<Boolean> guardadoExitoso = new MutableLiveData<>();

    // Selección hecha en el diálogo de obra social. Vive acá y no en la
    // Activity porque la Activity se recrea al rotar; el mismo motivo por el
    // que turnoSeleccionado/diagnosticoSeleccionadoId viven acá.
    private Integer nuevaObraSocialSeleccionadaId;
    private String nuevaObraSocialSeleccionadaNombre;

    public Integer getNuevaObraSocialSeleccionadaId() { return nuevaObraSocialSeleccionadaId; }
    public String getNuevaObraSocialSeleccionadaNombre() { return nuevaObraSocialSeleccionadaNombre; }

    public void setNuevaObraSocialSeleccionada(Integer id, String nombre) {
        this.nuevaObraSocialSeleccionadaId = id;
        this.nuevaObraSocialSeleccionadaNombre = nombre;
    }

    // Las cantidades viven acá y no en la Activity porque los TextView que las
    // muestran se destruyen y se vuelven a crear en cada rotación. Si el estado
    // viviera allá, rotar durante una edición devolvería las cantidades al valor
    // guardado y se perdería lo que el usuario acababa de cambiar.
    private final Map<Integer, Integer> cantidades = new HashMap<>();

    public int getCantidad(int tipoPrestacionId) {
        Integer valor = cantidades.get(tipoPrestacionId);
        return valor != null ? valor : 0;
    }

    public void setCantidad(int tipoPrestacionId, int cantidad) {
        cantidades.put(tipoPrestacionId, cantidad);
    }

    // Siembra las cantidades desde la atención que se está editando, una sola
    // vez: si el mapa ya tiene algo, es porque el usuario ya estuvo tocando.
    public void sembrarCantidadesSiVacio(List<PrestacionOdontologiaEntity> prestaciones) {
        if (!cantidades.isEmpty() || prestaciones == null) return;
        for (PrestacionOdontologiaEntity p : prestaciones) {
            cantidades.put(p.getTipoPrestacionId(), p.getCantidad());
        }
    }
    private final MutableLiveData<AtencionOdontologiaConDetalle> atencionEnEdicion = new MutableLiveData<>();

    // Estado de la precarga en edición y selecciones que se leen al guardar.
    // Viven acá y no en la Activity porque la Activity se recrea al rotar: si
    // estas banderas volvieran a cero, la precarga se aplicaría de nuevo encima
    // de lo que el usuario ya cambió, y turno/diagnóstico quedarían en null con
    // su texto todavía en pantalla.
    private boolean catalogoListo;
    private boolean diagnosticosListos;
    private boolean formularioPrecargado;
    private Integer turnoSeleccionado;
    private Integer diagnosticoSeleccionadoId;

    public RegistrarAtencionOdontologiaViewModel(@NonNull Application application) {
        super(application);
        context = application.getApplicationContext();
        AppDatabase db = AppDatabase.getInstancia(context);
        pacienteDao = db.pacienteDao();
        atencionDao = db.atencionOdontologiaDao();
        tipoPrestacionDao = db.tipoPrestacionOdontologiaDao();
        diagnosticoDao = db.diagnosticoDao();
        obraSocialDao = db.obraSocialDao();

        AppExecutors.io().execute(() -> {
            List<DiagnosticoEntity> lista = diagnosticoDao.listar();
            AppExecutors.ejecutarEnUI(() -> diagnosticos.setValue(lista));
        });
        AppExecutors.io().execute(() -> {
            List<ObraSocialEntity> lista = obraSocialDao.listar();
            AppExecutors.ejecutarEnUI(() -> obrasSociales.setValue(lista));
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

    public LiveData<List<ObraSocialEntity>> getObrasSociales() {
        return obrasSociales;
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

    public void marcarCatalogoListo() {
        catalogoListo = true;
    }

    public void marcarDiagnosticosListos() {
        diagnosticosListos = true;
    }

    // true solo cuando las tres fuentes (atención, catálogo y diagnósticos) ya
    // llegaron y la precarga todavía no se aplicó en toda la vida del ViewModel.
    public boolean debePrecargarFormulario() {
        return !formularioPrecargado && catalogoListo && diagnosticosListos
                && atencionEnEdicion.getValue() != null;
    }

    public void marcarFormularioPrecargado() {
        formularioPrecargado = true;
    }

    public Integer getTurnoSeleccionado() {
        return turnoSeleccionado;
    }

    public void setTurnoSeleccionado(Integer turnoSeleccionado) {
        this.turnoSeleccionado = turnoSeleccionado;
    }

    public Integer getDiagnosticoSeleccionadoId() {
        return diagnosticoSeleccionadoId;
    }

    public void setDiagnosticoSeleccionadoId(Integer diagnosticoSeleccionadoId) {
        this.diagnosticoSeleccionadoId = diagnosticoSeleccionadoId;
    }

    // Resuelve el código de turno a su etiqueta para mostrar. null si el
    // código no está en el catálogo (no debería pasar con datos válidos).
    public String turnoTexto(int tipoTurno) {
        for (int i = 0; i < TURNO_CODIGOS.length; i++) {
            if (TURNO_CODIGOS[i] == tipoTurno) return TURNO_LABELS[i];
        }
        return null;
    }

    // Resuelve el id de diagnóstico contra la lista ya cargada y arma el texto
    // a mostrar. null si la lista todavía no llegó o el id no está en ella.
    public String diagnosticoTexto(int diagnosticoId) {
        List<DiagnosticoEntity> lista = diagnosticos.getValue();
        if (lista == null) return null;
        for (DiagnosticoEntity d : lista) {
            if (d.getId() == diagnosticoId) return d.getCodigo() + " — " + d.getDescripcion();
        }
        return null;
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
                // Al rotar, onCreate vuelve a llamar acá y el ViewModel sobrevive con el
                // odontograma que el odontólogo venía marcando. Por eso solo se carga
                // desde la base cuando el modelo en memoria está vacío: si ya tiene
                // marcas son las del usuario y modelo.cargar() las borraría sin aviso.
                // A diferencia de precargarUltimoOdontograma, acá no se mira si los
                // items están vacíos: una atención puede tener el odontograma en blanco
                // y esa carga (que no cambia nada) es válida.
                if (modelo.aplanar().isEmpty()) {
                    modelo.cargar(items);
                    cpo.setValue(modelo.calcularCpo());
                    odontogramaActualizado.setValue(true);
                }
                // La atención sí se vuelve a publicar: es el mismo dato de la base y la
                // pantalla recién creada lo necesita para precargarse.
                atencionEnEdicion.setValue(cargada);
            });
        });
    }

    public void guardarAtencion(long pacienteLocalId, int tipoConsulta, Integer tipoTurno, Integer diagnosticoId,
                                 boolean embarazada, boolean sinObraSocial,
                                 Integer nuevaObraSocialId, String nuevaObraSocialNombre,
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

        Integer nuevaObraSocialIdFinal = sinObraSocial ? null : nuevaObraSocialId;
        ValoracionLocal valoracion = modelo.calcularCpo();

        AtencionOdontologiaEntity atencion = new AtencionOdontologiaEntity();
        atencion.setPacienteLocalId(pacienteLocalId);
        atencion.setTipoConsulta(tipoConsulta);
        atencion.setTipoTurno(tipoTurno);
        atencion.setDiagnosticoId(diagnosticoId);
        atencion.setEmbarazada(embarazada);
        atencion.setSinObraSocial(sinObraSocial);
        atencion.setNuevaObraSocialId(nuevaObraSocialIdFinal);
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
            if (!sinObraSocial && nuevaObraSocialIdFinal != null) {
                PacienteEntity paciente = pacienteDao.obtenerPorLocalId(pacienteLocalId);
                if (paciente != null && !paciente.tieneObraSocial()) {
                    paciente.setObraSocialId(nuevaObraSocialIdFinal);
                    paciente.setObraSocialNombre(nuevaObraSocialNombre);
                    pacienteDao.actualizar(paciente);
                }
            }

            AppExecutors.ejecutarEnUI(() -> {
                Toast.makeText(context, "Atención guardada. Se sincronizará cuando haya conexión.", Toast.LENGTH_LONG).show();
                guardadoExitoso.setValue(true);
            });
        });
    }

    // La obra social no entra: al guardar el alta quedó escrita en el paciente y
    // el par (sinObraSocial, nuevaObraSocial) ya se armó ahí con su invariante.
    // Tocar solo una de las dos mitades acá fabricaría pares inconsistentes que
    // el sincronizador mandaría tal cual al servidor.
    public void actualizarAtencion(long atencionLocalId, int tipoConsulta, Integer tipoTurno,
                                   Integer diagnosticoId, boolean embarazada,
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
