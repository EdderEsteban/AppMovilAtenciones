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
import com.example.registrosatenciones.db.entity.DiagnosticoEntity;
import com.example.registrosatenciones.db.entity.PrestacionOdontologiaEntity;
import com.example.registrosatenciones.db.entity.TipoPrestacionOdontologiaEntity;
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

    private static final String[] TURNO_LABELS = {"Ventanilla", "Profesional", "Demanda espontánea", "Interdisciplinario"};
    private static final int[] TURNO_CODIGOS = {1, 2, 3, 4};

    private ActivityRegistrarAtencionOdontologiaBinding binding;
    private RegistrarAtencionOdontologiaViewModel viewModel;
    private long pacienteLocalId;
    private boolean pacienteTieneObraSocial;
    private Integer turnoSeleccionado;
    private Integer diagnosticoSeleccionadoId;
    private List<DiagnosticoEntity> listaDiagnosticos = new ArrayList<>();

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

        viewModel = new ViewModelProvider(this).get(RegistrarAtencionOdontologiaViewModel.class);

        binding.odontogramaResumen.setModelo(viewModel.getModelo());
        binding.odontogramaResumen.setFilas(Arrays.asList(
                DientesFdi.TEMPORARIOS_SUPERIOR, DientesFdi.TEMPORARIOS_INFERIOR,
                DientesFdi.PERMANENTES_SUPERIOR, DientesFdi.PERMANENTES_INFERIOR));

        ArrayAdapter<String> turnoAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, TURNO_LABELS);
        binding.actvTipoTurno.setAdapter(turnoAdapter);
        binding.actvTipoTurno.setOnItemClickListener((parent, v, position, id) -> turnoSeleccionado = TURNO_CODIGOS[position]);

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

        viewModel.getDiagnosticos().observe(this, lista -> listaDiagnosticos = lista != null ? lista : new ArrayList<>());

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

            viewModel.guardarAtencion(pacienteLocalId, tipoConsulta, turnoSeleccionado, diagnosticoSeleccionadoId,
                    embarazada, sinObraSocial, nuevaObraSocial, observaciones, obtenerPrestacionesSeleccionadas());
        });

        binding.btnCancelar.setOnClickListener(v -> confirmarCancelar());
    }

    private void confirmarCancelar() {
        new AlertDialog.Builder(this)
                .setTitle("¿Cancelar el registro?")
                .setMessage("Se va a perder lo que cargaste en esta atención, incluido el odontograma.")
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
