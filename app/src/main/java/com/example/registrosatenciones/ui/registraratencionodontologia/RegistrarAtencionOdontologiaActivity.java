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
import com.example.registrosatenciones.adapters.ObraSocialAdapter;
import com.example.registrosatenciones.databinding.ActivityRegistrarAtencionOdontologiaBinding;
import com.example.registrosatenciones.databinding.DialogSeleccionarDiagnosticoBinding;
import com.example.registrosatenciones.databinding.DialogSeleccionarObraSocialBinding;
import com.example.registrosatenciones.db.entity.AtencionOdontologiaEntity;
import com.example.registrosatenciones.db.entity.DiagnosticoEntity;
import com.example.registrosatenciones.db.entity.ObraSocialEntity;
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
    private List<DiagnosticoEntity> listaDiagnosticos = new ArrayList<>();
    private List<ObraSocialEntity> listaObrasSociales = new ArrayList<>();

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
            ocultarBloqueObraSocial();
            viewModel.getAtencionEnEdicion().observe(this, cargada -> {
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
                viewModel.setTurnoSeleccionado(RegistrarAtencionOdontologiaViewModel.TURNO_CODIGOS[position]));

        viewModel.observarPaciente(pacienteLocalId).observe(this, paciente -> {
            if (paciente == null) return;
            binding.tvNombrePaciente.setText(paciente.getApellido() + ", " + paciente.getNombre());

            StringBuilder detalle = new StringBuilder("DNI ").append(paciente.getDni());
            if (paciente.getEdad() != null) detalle.append(" · ").append(paciente.getEdad()).append(" años");
            binding.tvDetallePaciente.setText(detalle.toString());

            boolean esVaron = "M".equals(paciente.getSexo());
            binding.rowEmbarazada.setVisibility(esVaron ? View.GONE : View.VISIBLE);
            // El divisor separa Embarazada de Sin obra social; en edición ese bloque no está.
            binding.divisorEmbarazada.setVisibility(esVaron || modoEdicion ? View.GONE : View.VISIBLE);

            if (modoEdicion) return;
            pacienteTieneObraSocial = paciente.tieneObraSocial();
            if (pacienteTieneObraSocial) {
                binding.tvObraSocialActual.setText("Obra social: " + paciente.getObraSocialNombre());
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
            viewModel.marcarDiagnosticosListos();
            intentarPrecargarFormulario();
        });

        viewModel.getObrasSociales().observe(this, lista ->
                listaObrasSociales = lista != null ? lista : new ArrayList<>());

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
        binding.etNuevaObraSocial.setOnClickListener(v -> mostrarSelectorObraSocial());

        binding.btnCuadrante1.setOnClickListener(v -> abrirEditorCuadrante(1));
        binding.btnCuadrante2.setOnClickListener(v -> abrirEditorCuadrante(2));
        binding.btnCuadrante3.setOnClickListener(v -> abrirEditorCuadrante(3));
        binding.btnCuadrante4.setOnClickListener(v -> abrirEditorCuadrante(4));

        binding.btnGuardar.setOnClickListener(v -> {
            int tipoConsulta = binding.btnUlterior.isChecked() ? 2 : 1;
            boolean embarazada = binding.switchEmbarazada.isChecked();
            String observaciones = binding.etObservaciones.getText().toString();

            if (modoEdicion) {
                // La obra social no se manda: en edición no se edita.
                viewModel.actualizarAtencion(atencionLocalId, tipoConsulta, viewModel.getTurnoSeleccionado(),
                        viewModel.getDiagnosticoSeleccionadoId(), embarazada, observaciones,
                        obtenerPrestacionesSeleccionadas());
            } else {
                viewModel.guardarAtencion(pacienteLocalId, tipoConsulta, viewModel.getTurnoSeleccionado(),
                        viewModel.getDiagnosticoSeleccionadoId(), embarazada,
                        binding.switchSinObraSocial.isChecked(),
                        viewModel.getNuevaObraSocialSeleccionadaId(),
                        viewModel.getNuevaObraSocialSeleccionadaNombre(),
                        observaciones, obtenerPrestacionesSeleccionadas());
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
            viewModel.setDiagnosticoSeleccionadoId(diagnostico.getId());
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

    private void mostrarSelectorObraSocial() {
        DialogSeleccionarObraSocialBinding dialogBinding = DialogSeleccionarObraSocialBinding.inflate(getLayoutInflater());
        RecyclerView rv = dialogBinding.rvObrasSociales;
        rv.setLayoutManager(new LinearLayoutManager(this));

        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogBinding.getRoot()).create();

        ObraSocialAdapter adapter = new ObraSocialAdapter(this, obraSocial -> {
            viewModel.setNuevaObraSocialSeleccionada(obraSocial.getId(), obraSocial.getNombre());
            binding.etNuevaObraSocial.setText(obraSocial.getNombre());
            dialog.dismiss();
        });
        adapter.setObrasSociales(listaObrasSociales);
        rv.setAdapter(adapter);

        dialogBinding.etBuscarObraSocial.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filtrar(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        dialog.show();
    }

    // En edición la obra social no se toca: al guardar el alta ya quedó escrita en
    // el paciente. Se oculta el bloque entero en vez de dejarlo editable y
    // descartar en silencio lo que el usuario escriba.
    private void ocultarBloqueObraSocial() {
        binding.rowSinObraSocial.setVisibility(View.GONE);
        binding.tvObraSocialActual.setVisibility(View.GONE);
        binding.tilNuevaObraSocial.setVisibility(View.GONE);
    }

    private void actualizarVisibilidadNuevaObraSocial() {
        if (modoEdicion) return;
        boolean sinObraSocial = binding.switchSinObraSocial.isChecked();
        if (sinObraSocial) {
            binding.tilNuevaObraSocial.setVisibility(View.GONE);
            binding.etNuevaObraSocial.setText("");
            viewModel.setNuevaObraSocialSeleccionada(null, null);
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

        viewModel.marcarCatalogoListo();
        intentarPrecargarFormulario();
        aplicarCantidadesEnEdicion();
    }

    // La atención, el catálogo y los diagnósticos llegan por LiveData
    // independientes: solo dispara cuando las tres fuentes ya llegaron y todavía
    // no se precargó, porque reaplicar la precarga pisaría sin avisar lo que el
    // usuario ya tocó. El estado vive en el ViewModel y no acá, así que una
    // rotación tampoco la vuelve a aplicar: los campos con id los restaura
    // Android solo, y el turno y el diagnóstico elegidos los guarda el ViewModel.
    private void intentarPrecargarFormulario() {
        if (!viewModel.debePrecargarFormulario()) return;
        precargarFormulario(viewModel.getAtencionEnEdicion().getValue());
        viewModel.marcarFormularioPrecargado();
    }

    private void precargarFormulario(AtencionOdontologiaConDetalle cargada) {
        if (cargada == null) return;
        AtencionOdontologiaEntity a = cargada.getAtencion();

        // Tipo de consulta: 1 = primera vez, 2 = ulterior
        if (a.getTipoConsulta() == 2) binding.btnUlterior.setChecked(true);
        else binding.btnPrimeraVez.setChecked(true);

        // Tipo de turno: además de mostrarlo hay que dejar seteado el turno en el
        // ViewModel, que es lo que se lee al guardar. El texto ya viene resuelto de ahí.
        viewModel.setTurnoSeleccionado(a.getTipoTurno());
        String turnoTexto = viewModel.turnoTexto(a.getTipoTurno());
        if (turnoTexto != null) binding.actvTipoTurno.setText(turnoTexto, false);

        // Diagnóstico: igual, hay que dejar seteado el id en el ViewModel y mostrar su
        // texto, ya resuelto por el ViewModel contra la lista de diagnósticos.
        viewModel.setDiagnosticoSeleccionadoId(a.getDiagnosticoId());
        String diagnosticoTexto = viewModel.diagnosticoTexto(a.getDiagnosticoId());
        if (diagnosticoTexto != null) binding.etDiagnostico.setText(diagnosticoTexto);

        binding.switchEmbarazada.setChecked(a.isEmbarazada());
        binding.etObservaciones.setText(a.getObservaciones() != null ? a.getObservaciones() : "");
    }

    // Las cantidades van aparte de la precarga: viven en las vistas que
    // renderizarPrestaciones destruye y reconstruye en cero cada vez que el
    // catálogo emite. Por eso se reaplican en cada reconstrucción, y no una
    // sola vez como los campos de arriba.
    private void aplicarCantidadesEnEdicion() {
        AtencionOdontologiaConDetalle cargada = viewModel.getAtencionEnEdicion().getValue();
        if (cargada == null) return;
        viewModel.sembrarCantidadesSiVacio(cargada.getPrestaciones());
        for (Map.Entry<Integer, TextView> entry : cantidadPorTipoId.entrySet()) {
            entry.getValue().setText(String.valueOf(viewModel.getCantidad(entry.getKey())));
        }
    }

    private View crearFilaPrestacion(TipoPrestacionOdontologiaEntity tipo) {
        View fila = getLayoutInflater().inflate(R.layout.item_prestacion_stepper, binding.containerPrestaciones, false);
        TextView tvNombre = fila.findViewById(R.id.tvNombrePrestacion);
        TextView tvCantidad = fila.findViewById(R.id.tvCantidad);
        View btnMenos = fila.findViewById(R.id.btnMenos);
        View btnMas = fila.findViewById(R.id.btnMas);

        tvNombre.setText(tipo.getNombre());
        // El valor sale del ViewModel, que es lo que sobrevive a la rotación.
        tvCantidad.setText(String.valueOf(viewModel.getCantidad(tipo.getId())));
        cantidadPorTipoId.put(tipo.getId(), tvCantidad);

        btnMenos.setOnClickListener(v -> {
            int actual = Integer.parseInt(tvCantidad.getText().toString());
            if (actual > 0) {
                tvCantidad.setText(String.valueOf(actual - 1));
                viewModel.setCantidad(tipo.getId(), actual - 1);
            }
        });
        btnMas.setOnClickListener(v -> {
            int actual = Integer.parseInt(tvCantidad.getText().toString());
            viewModel.setCantidad(tipo.getId(), actual + 1);
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
