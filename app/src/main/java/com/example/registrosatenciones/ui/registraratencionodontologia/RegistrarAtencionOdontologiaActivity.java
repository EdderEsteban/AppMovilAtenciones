package com.example.registrosatenciones.ui.registraratencionodontologia;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.registrosatenciones.R;
import com.example.registrosatenciones.adapters.DiagnosticoAdapter;
import com.example.registrosatenciones.databinding.ActivityRegistrarAtencionOdontologiaBinding;
import com.example.registrosatenciones.databinding.DialogSeleccionarDiagnosticoBinding;
import com.example.registrosatenciones.db.entity.AtencionOdontologiaEntity;
import com.example.registrosatenciones.db.entity.DiagnosticoEntity;
import com.example.registrosatenciones.db.entity.PrestacionOdontologiaEntity;
import com.example.registrosatenciones.db.entity.TipoPrestacionOdontologiaEntity;
import com.example.registrosatenciones.db.relation.AtencionOdontologiaConDetalle;
import com.example.registrosatenciones.ui.odontologia.EditorCuadranteActivity;
import com.example.registrosatenciones.ui.odontologia.model.DientesFdi;
import com.example.registrosatenciones.ui.odontologia.model.OdontogramaItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegistrarAtencionOdontologiaActivity extends AppCompatActivity {

    public static final String EXTRA_PACIENTE_LOCAL_ID = "pacienteLocalId";
    public static final String EXTRA_ATENCION_LOCAL_ID = "atencionLocalId";

    private ActivityRegistrarAtencionOdontologiaBinding binding;
    private RegistrarAtencionOdontologiaViewModel viewModel;
    private long pacienteLocalId;
    private long atencionLocalId = -1;
    private boolean modoEdicion;
    private boolean pacienteTieneObraSocial;
    private Integer turnoSeleccionado;
    private Integer diagnosticoSeleccionadoId;
    private List<DiagnosticoEntity> listaDiagnosticos = new ArrayList<>();

    // La atención a editar, el catálogo de prestaciones y la lista de
    // diagnósticos llegan por LiveData independientes. Se precarga UNA sola
    // vez, cuando las tres fuentes están listas: reaplicar la precarga en cada
    // llegada pisaría sin avisar lo que el usuario ya haya tocado en el
    // formulario entre medio.
    private AtencionOdontologiaConDetalle atencionCargada;
    private boolean catalogoListo;
    private boolean diagnosticosListos;
    private boolean formularioPrecargado;

    private final Map<Integer, TextView> cantidadPorTipoId = new HashMap<>();

    private final ActivityResultLauncher<Intent> editorCuadranteLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), resultado -> {
                if (resultado.getResultCode() == RESULT_OK && resultado.getData() != null) {
                    @SuppressWarnings("unchecked")
                    ArrayList<OdontogramaItem> estados =
                            (ArrayList<OdontogramaItem>) resultado.getData().getSerializableExtra(EditorCuadranteActivity.EXTRA_ESTADOS);
                    viewModel.actualizarCuadrante(estados);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegistrarAtencionOdontologiaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        pacienteLocalId = getIntent().getLongExtra(EXTRA_PACIENTE_LOCAL_ID, -1);
        atencionLocalId = getIntent().getLongExtra(EXTRA_ATENCION_LOCAL_ID, -1);
        modoEdicion = atencionLocalId != -1;

        viewModel = new ViewModelProvider(this).get(RegistrarAtencionOdontologiaViewModel.class);

        binding.odontogramaResumen.setModelo(viewModel.getModelo());
        binding.odontogramaResumen.setFilas(Arrays.asList(
                DientesFdi.TEMPORARIOS_SUPERIOR, DientesFdi.TEMPORARIOS_INFERIOR,
                DientesFdi.PERMANENTES_SUPERIOR, DientesFdi.PERMANENTES_INFERIOR));

        if (modoEdicion) {
            binding.btnGuardar.setText("Guardar cambios");
            viewModel.getAtencionEnEdicion().observe(this, cargada -> {
                atencionCargada = cargada;
                intentarPrecargarFormulario();
                aplicarCantidadesEnEdicion();
            });
            viewModel.cargarParaEditar(atencionLocalId);
        } else {
            // Arranca la atención nueva con el último odontograma del paciente (no en blanco).
            viewModel.precargarUltimoOdontograma(pacienteLocalId);
        }

        ArrayAdapter<String> turnoAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                RegistrarAtencionOdontologiaViewModel.TURNO_LABELS);
        binding.actvTipoTurno.setAdapter(turnoAdapter);
        binding.actvTipoTurno.setOnItemClickListener((parent, v, position, id) ->
                turnoSeleccionado = RegistrarAtencionOdontologiaViewModel.TURNO_CODIGOS[position]);

        viewModel.observarPaciente(pacienteLocalId).observe(this, paciente -> {
            if (paciente == null) return;
            binding.tvNombrePaciente.setText(paciente.getApellido() + ", " + paciente.getNombre());

            StringBuilder detalle = new StringBuilder("DNI ").append(paciente.getDni());
            if (paciente.getEdad() != null) detalle.append(" · ").append(paciente.getEdad()).append(" años");
            binding.tvDetallePaciente.setText(detalle.toString());

            boolean esVaron = "M".equals(paciente.getSexo());
            binding.rowEmbarazada.setVisibility(esVaron ? View.GONE : View.VISIBLE);
            binding.divisorEmbarazada.setVisibility(esVaron ? View.GONE : View.VISIBLE);

            pacienteTieneObraSocial = paciente.getObraSocial() != null && !paciente.getObraSocial().isEmpty();
            if (pacienteTieneObraSocial) {
                binding.tvObraSocialActual.setText("Obra social: " + paciente.getObraSocial());
                binding.tvObraSocialActual.setVisibility(View.VISIBLE);
                binding.switchSinObraSocial.setChecked(false);
                binding.switchSinObraSocial.setEnabled(false);
                binding.tilNuevaObraSocial.setVisibility(View.GONE);
            } else {
                binding.tvObraSocialActual.setVisibility(View.GONE);
                binding.switchSinObraSocial.setEnabled(true);
                actualizarVisibilidadNuevaObraSocial();
            }
        });

        viewModel.observarCatalogoPrestaciones().observe(this, this::renderizarPrestaciones);

        viewModel.getDiagnosticos().observe(this, lista -> {
            listaDiagnosticos = lista != null ? lista : new ArrayList<>();
            diagnosticosListos = true;
            intentarPrecargarFormulario();
        });

        viewModel.getCpo().observe(this, v -> {
            binding.tvCariesPerm.setText(String.valueOf(v.cariesPerm));
            binding.tvPerdidosPerm.setText(String.valueOf(v.perdidosPerm));
            binding.tvObturadosPerm.setText(String.valueOf(v.obturadosPerm));
            binding.tvCariesTemp.setText(String.valueOf(v.cariesTemp));
            binding.tvExtraccionTemp.setText(String.valueOf(v.extraccionTemp));
            binding.tvObturadosTemp.setText(String.valueOf(v.obturadosTemp));
        });

        viewModel.getOdontogramaActualizado().observe(this, actualizado -> {
            if (actualizado != null && actualizado) binding.odontogramaResumen.redibujar();
        });

        viewModel.getGuardadoExitoso().observe(this, guardado -> {
            if (guardado != null && guardado) finish();
        });

        binding.switchSinObraSocial.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (pacienteTieneObraSocial) return;
            actualizarVisibilidadNuevaObraSocial();
        });

        binding.etDiagnostico.setOnClickListener(v -> mostrarSelectorDiagnostico());

        binding.btnCuadrante1.setOnClickListener(v -> abrirEditorCuadrante(1));
        binding.btnCuadrante2.setOnClickListener(v -> abrirEditorCuadrante(2));
        binding.btnCuadrante3.setOnClickListener(v -> abrirEditorCuadrante(3));
        binding.btnCuadrante4.setOnClickListener(v -> abrirEditorCuadrante(4));

        binding.btnGuardar.setOnClickListener(v -> {
            int tipoConsulta = binding.btnUlterior.isChecked() ? 2 : 1;
            boolean embarazada = binding.switchEmbarazada.isChecked();
            boolean sinObraSocial = binding.switchSinObraSocial.isChecked();
            String nuevaObraSocial = binding.etNuevaObraSocial.getText().toString();
            String observaciones = binding.etObservaciones.getText().toString();

            if (modoEdicion) {
                viewModel.actualizarAtencion(atencionLocalId, tipoConsulta, turnoSeleccionado, diagnosticoSeleccionadoId,
                        embarazada, sinObraSocial, observaciones, obtenerPrestacionesSeleccionadas());
            } else {
                viewModel.guardarAtencion(pacienteLocalId, tipoConsulta, turnoSeleccionado, diagnosticoSeleccionadoId,
                        embarazada, sinObraSocial, nuevaObraSocial, observaciones, obtenerPrestacionesSeleccionadas());
            }
        });

        binding.btnCancelar.setOnClickListener(v -> confirmarCancelar());
    }

    private void confirmarCancelar() {
        String mensaje = modoEdicion
                ? "Se van a descartar los cambios que hiciste en esta atención, incluido el odontograma."
                : "Se va a perder lo que cargaste en esta atención, incluido el odontograma.";
        new AlertDialog.Builder(this)
                .setTitle("¿Cancelar el registro?")
                .setMessage(mensaje)
                .setPositiveButton("Sí, cancelar", (dialog, which) -> finish())
                .setNegativeButton("Seguir cargando", null)
                .show();
    }

    private void abrirEditorCuadrante(int cuadrante) {
        Intent intent = new Intent(this, EditorCuadranteActivity.class);
        intent.putExtra(EditorCuadranteActivity.EXTRA_CUADRANTE, cuadrante);
        intent.putExtra(EditorCuadranteActivity.EXTRA_ESTADOS, new ArrayList<>(viewModel.getModelo().aplanar()));
        editorCuadranteLauncher.launch(intent);
    }

    private void mostrarSelectorDiagnostico() {
        DialogSeleccionarDiagnosticoBinding dialogBinding = DialogSeleccionarDiagnosticoBinding.inflate(getLayoutInflater());
        RecyclerView rv = dialogBinding.rvDiagnosticos;
        rv.setLayoutManager(new LinearLayoutManager(this));

        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogBinding.getRoot()).create();

        DiagnosticoAdapter adapter = new DiagnosticoAdapter(this, diagnostico -> {
            diagnosticoSeleccionadoId = diagnostico.getId();
            binding.etDiagnostico.setText(diagnostico.getCodigo() + " — " + diagnostico.getDescripcion());
            dialog.dismiss();
        });
        adapter.setDiagnosticos(listaDiagnosticos);
        rv.setAdapter(adapter);

        dialogBinding.etBuscarDiagnostico.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filtrar(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        dialog.show();
    }

    private void actualizarVisibilidadNuevaObraSocial() {
        boolean sinObraSocial = binding.switchSinObraSocial.isChecked();
        if (sinObraSocial) {
            binding.tilNuevaObraSocial.setVisibility(View.GONE);
            binding.etNuevaObraSocial.setText("");
        } else {
            binding.tilNuevaObraSocial.setVisibility(View.VISIBLE);
        }
    }

    private void renderizarPrestaciones(List<TipoPrestacionOdontologiaEntity> catalogo) {
        binding.containerPrestaciones.removeAllViews();
        cantidadPorTipoId.clear();
        if (catalogo == null) return;

        for (TipoPrestacionOdontologiaEntity tipo : catalogo) {
            binding.containerPrestaciones.addView(crearFilaPrestacion(tipo));
        }

        catalogoListo = true;
        intentarPrecargarFormulario();
        aplicarCantidadesEnEdicion();
    }

    // Solo dispara cuando las tres fuentes (atención, catálogo y diagnósticos)
    // ya llegaron y todavía no se aplicó la precarga. Se ejecuta una única vez.
    private void intentarPrecargarFormulario() {
        if (formularioPrecargado) return;
        if (atencionCargada == null || !catalogoListo || !diagnosticosListos) return;
        precargarFormulario(atencionCargada);
        formularioPrecargado = true;
    }

    private void precargarFormulario(AtencionOdontologiaConDetalle cargada) {
        if (cargada == null) return;
        AtencionOdontologiaEntity a = cargada.getAtencion();

        // Tipo de consulta: 1 = primera vez, 2 = ulterior
        if (a.getTipoConsulta() == 2) binding.btnUlterior.setChecked(true);
        else binding.btnPrimeraVez.setChecked(true);

        // Tipo de turno: además de mostrarlo hay que dejar seteado turnoSeleccionado,
        // que es lo que se lee al guardar. El texto ya viene resuelto del ViewModel.
        turnoSeleccionado = a.getTipoTurno();
        String turnoTexto = viewModel.turnoTexto(a.getTipoTurno());
        if (turnoTexto != null) binding.actvTipoTurno.setText(turnoTexto, false);

        // Diagnóstico: igual, hay que setear diagnosticoSeleccionadoId y mostrar su
        // texto, ya resuelto por el ViewModel contra la lista de diagnósticos.
        diagnosticoSeleccionadoId = a.getDiagnosticoId();
        String diagnosticoTexto = viewModel.diagnosticoTexto(a.getDiagnosticoId());
        if (diagnosticoTexto != null) binding.etDiagnostico.setText(diagnosticoTexto);

        binding.switchEmbarazada.setChecked(a.isEmbarazada());
        binding.switchSinObraSocial.setChecked(a.isSinObraSocial());
        binding.etObservaciones.setText(a.getObservaciones() != null ? a.getObservaciones() : "");
    }

    // Las cantidades van aparte de la precarga: viven en las vistas que
    // renderizarPrestaciones destruye y reconstruye en cero cada vez que el
    // catálogo emite. Por eso se reaplican en cada reconstrucción, y no una
    // sola vez como los campos de arriba.
    private void aplicarCantidadesEnEdicion() {
        if (atencionCargada == null) return;
        for (PrestacionOdontologiaEntity p : atencionCargada.getPrestaciones()) {
            TextView tv = cantidadPorTipoId.get(p.getTipoPrestacionId());
            if (tv != null) tv.setText(String.valueOf(p.getCantidad()));
        }
    }

    private View crearFilaPrestacion(TipoPrestacionOdontologiaEntity tipo) {
        View fila = getLayoutInflater().inflate(R.layout.item_prestacion_stepper, binding.containerPrestaciones, false);
        TextView tvNombre = fila.findViewById(R.id.tvNombrePrestacion);
        TextView tvCantidad = fila.findViewById(R.id.tvCantidad);
        View btnMenos = fila.findViewById(R.id.btnMenos);
        View btnMas = fila.findViewById(R.id.btnMas);

        tvNombre.setText(tipo.getNombre());
        tvCantidad.setText("0");
        cantidadPorTipoId.put(tipo.getId(), tvCantidad);

        btnMenos.setOnClickListener(v -> {
            int actual = Integer.parseInt(tvCantidad.getText().toString());
            if (actual > 0) tvCantidad.setText(String.valueOf(actual - 1));
        });
        btnMas.setOnClickListener(v -> {
            int actual = Integer.parseInt(tvCantidad.getText().toString());
            tvCantidad.setText(String.valueOf(actual + 1));
        });

        return fila;
    }

    private List<PrestacionOdontologiaEntity> obtenerPrestacionesSeleccionadas() {
        List<PrestacionOdontologiaEntity> seleccionadas = new ArrayList<>();
        for (Map.Entry<Integer, TextView> entry : cantidadPorTipoId.entrySet()) {
            int cantidad = Integer.parseInt(entry.getValue().getText().toString());
            if (cantidad > 0) {
                PrestacionOdontologiaEntity p = new PrestacionOdontologiaEntity();
                p.setTipoPrestacionId(entry.getKey());
                p.setCantidad(cantidad);
                seleccionadas.add(p);
            }
        }
        return seleccionadas;
    }
}
