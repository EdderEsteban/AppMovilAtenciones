package com.example.registrosatenciones.ui.registraratencion;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.registrosatenciones.R;
import com.example.registrosatenciones.databinding.ActivityRegistrarAtencionBinding;
import com.example.registrosatenciones.db.entity.PrestacionEnfermeriaEntity;
import com.example.registrosatenciones.db.entity.TipoPrestacionEnfermeriaEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegistrarAtencionActivity extends AppCompatActivity {

    public static final String EXTRA_PACIENTE_LOCAL_ID = "pacienteLocalId";

    private ActivityRegistrarAtencionBinding binding;
    private RegistrarAtencionViewModel viewModel;
    private long pacienteLocalId;

    private final Map<Integer, TextView> cantidadPorTipoId = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegistrarAtencionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        pacienteLocalId = getIntent().getLongExtra(EXTRA_PACIENTE_LOCAL_ID, -1);

        viewModel = new ViewModelProvider(this).get(RegistrarAtencionViewModel.class);

        viewModel.observarPaciente(pacienteLocalId).observe(this, paciente -> {
            if (paciente == null) return;
            binding.tvNombrePaciente.setText(paciente.getApellido() + ", " + paciente.getNombre());

            StringBuilder detalle = new StringBuilder("DNI ").append(paciente.getDni());
            if (paciente.getEdad() != null) detalle.append(" · ").append(paciente.getEdad()).append(" años");
            binding.tvDetallePaciente.setText(detalle.toString());
        });

        viewModel.observarCatalogo().observe(this, this::renderizarPrestaciones);

        viewModel.getGuardadoExitoso().observe(this, guardado -> {
            if (guardado != null && guardado) finish();
        });

        binding.btnGuardar.setOnClickListener(v -> {
            int tipoAtencion = binding.btnInternado.isChecked() ? 2 : 1;
            boolean embarazada = binding.switchEmbarazada.isChecked();
            boolean sinObraSocial = binding.switchSinObraSocial.isChecked();
            String observaciones = binding.etObservaciones.getText().toString();

            viewModel.guardarAtencion(pacienteLocalId, tipoAtencion, embarazada, sinObraSocial,
                    observaciones, obtenerPrestacionesSeleccionadas());
        });
    }

    private void renderizarPrestaciones(List<TipoPrestacionEnfermeriaEntity> catalogo) {
        binding.containerPrestaciones.removeAllViews();
        cantidadPorTipoId.clear();
        if (catalogo == null) return;

        String grupoActual = null;
        for (TipoPrestacionEnfermeriaEntity tipo : catalogo) {
            if (!tipo.getGrupo().equals(grupoActual)) {
                grupoActual = tipo.getGrupo();
                binding.containerPrestaciones.addView(crearEncabezadoGrupo(grupoActual));
            }
            binding.containerPrestaciones.addView(crearFilaPrestacion(tipo));
        }
    }

    private TextView crearEncabezadoGrupo(String nombreGrupo) {
        TextView tv = new TextView(this);
        tv.setText(nombreGrupo.toUpperCase());
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
        tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        tv.setTextColor(getColor(R.color.color_brand));
        int padTop = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics());
        tv.setPadding(0, padTop, 0, 4);
        return tv;
    }

    private View crearFilaPrestacion(TipoPrestacionEnfermeriaEntity tipo) {
        View fila = getLayoutInflater().inflate(R.layout.item_prestacion_stepper, binding.containerPrestaciones, false);
        TextView tvNombre = fila.findViewById(R.id.tvNombrePrestacion);
        TextView tvCantidad = fila.findViewById(R.id.tvCantidad);
        View btnMenos = fila.findViewById(R.id.btnMenos);
        View btnMas = fila.findViewById(R.id.btnMas);

        tvNombre.setText(tipo.getNombrePrestacion());
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

    private List<PrestacionEnfermeriaEntity> obtenerPrestacionesSeleccionadas() {
        List<PrestacionEnfermeriaEntity> seleccionadas = new ArrayList<>();
        for (Map.Entry<Integer, TextView> entry : cantidadPorTipoId.entrySet()) {
            int cantidad = Integer.parseInt(entry.getValue().getText().toString());
            if (cantidad > 0) {
                PrestacionEnfermeriaEntity p = new PrestacionEnfermeriaEntity();
                p.setTipoPrestacionId(entry.getKey());
                p.setCantidad(cantidad);
                seleccionadas.add(p);
            }
        }
        return seleccionadas;
    }
}