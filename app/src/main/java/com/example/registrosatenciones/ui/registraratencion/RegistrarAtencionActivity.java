package com.example.registrosatenciones.ui.registraratencion;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.registrosatenciones.R;
import com.example.registrosatenciones.databinding.ActivityRegistrarAtencionBinding;
import com.example.registrosatenciones.db.entity.AtencionEnfermeriaEntity;
import com.example.registrosatenciones.db.entity.PrestacionEnfermeriaEntity;
import com.example.registrosatenciones.db.entity.TipoPrestacionEnfermeriaEntity;
import com.example.registrosatenciones.db.relation.AtencionConPrestaciones;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegistrarAtencionActivity extends AppCompatActivity {

    public static final String EXTRA_PACIENTE_LOCAL_ID = "pacienteLocalId";
    public static final String EXTRA_ATENCION_LOCAL_ID = "atencionLocalId";

    private ActivityRegistrarAtencionBinding binding;
    private RegistrarAtencionViewModel viewModel;
    private long pacienteLocalId;
    private long atencionLocalId = -1;
    private boolean modoEdicion;
    private boolean pacienteTieneObraSocial;

    private final Map<Integer, TextView> cantidadPorTipoId = new HashMap<>();

    // Última atención recibida por LiveData, para reaplicarla si el catálogo
    // (otra fuente asíncrona) todavía no había terminado de dibujarse.
    private AtencionConPrestaciones atencionPendienteDePrecargar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegistrarAtencionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        pacienteLocalId = getIntent().getLongExtra(EXTRA_PACIENTE_LOCAL_ID, -1);
        atencionLocalId = getIntent().getLongExtra(EXTRA_ATENCION_LOCAL_ID, -1);
        modoEdicion = atencionLocalId != -1;

        viewModel = new ViewModelProvider(this).get(RegistrarAtencionViewModel.class);

        viewModel.observarPaciente(pacienteLocalId).observe(this, paciente -> {
            if (paciente == null) return;
            binding.tvNombrePaciente.setText(paciente.getApellido() + ", " + paciente.getNombre());

            StringBuilder detalle = new StringBuilder("DNI ").append(paciente.getDni());
            if (paciente.getEdad() != null) detalle.append(" · ").append(paciente.getEdad()).append(" años");
            binding.tvDetallePaciente.setText(detalle.toString());

            // Embarazada: no aplica a pacientes varones (igual que el panel .NET)
            boolean esVaron = "M".equals(paciente.getSexo());
            binding.rowEmbarazada.setVisibility(esVaron ? View.GONE : View.VISIBLE);
            binding.divisorEmbarazada.setVisibility(esVaron ? View.GONE : View.VISIBLE);

            // Obra social: si ya tiene una cargada, se muestra de solo lectura y no se pide de nuevo
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

        viewModel.observarCatalogo().observe(this, this::renderizarPrestaciones);

        viewModel.getGuardadoExitoso().observe(this, guardado -> {
            if (guardado != null && guardado) finish();
        });

        if (modoEdicion) {
            binding.btnGuardar.setText("Guardar cambios");
            viewModel.getAtencionEnEdicion().observe(this, this::precargarFormulario);
            viewModel.cargarParaEditar(atencionLocalId);
        }

        binding.switchSinObraSocial.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (pacienteTieneObraSocial) return;
            actualizarVisibilidadNuevaObraSocial();
        });

        binding.btnGuardar.setOnClickListener(v -> {
            int tipoAtencion = binding.btnInternado.isChecked() ? 2 : 1;
            boolean embarazada = binding.switchEmbarazada.isChecked();
            boolean sinObraSocial = binding.switchSinObraSocial.isChecked();
            String observaciones = binding.etObservaciones.getText().toString();
            String nuevaObraSocial = binding.etNuevaObraSocial.getText().toString();

            if (modoEdicion) {
                viewModel.actualizarAtencion(atencionLocalId, tipoAtencion, embarazada,
                        sinObraSocial, observaciones, obtenerPrestacionesSeleccionadas());
            } else {
                viewModel.guardarAtencion(pacienteLocalId, tipoAtencion, embarazada, sinObraSocial,
                        observaciones, nuevaObraSocial, obtenerPrestacionesSeleccionadas());
            }
        });

        binding.btnCancelar.setOnClickListener(v -> confirmarCancelar());
    }

    private void confirmarCancelar() {
        String mensaje = modoEdicion
                ? "Se van a descartar los cambios que hiciste en esta atención."
                : "Se va a perder lo que cargaste en esta atención.";
        new AlertDialog.Builder(this)
                .setTitle("¿Cancelar el registro?")
                .setMessage(mensaje)
                .setPositiveButton("Sí, cancelar", (dialog, which) -> finish())
                .setNegativeButton("Seguir cargando", null)
                .show();
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

        // El catálogo y la atención a editar llegan por LiveData independientes.
        // Si la atención llegó primero, cantidadPorTipoId todavía estaba vacío y
        // esa precarga no tuvo efecto: se reintenta acá, que es el único punto
        // donde ya sabemos que ambas fuentes están disponibles.
        if (atencionPendienteDePrecargar != null) {
            precargarFormulario(atencionPendienteDePrecargar);
        }
    }

    private void precargarFormulario(AtencionConPrestaciones cargada) {
        if (cargada == null) return;
        atencionPendienteDePrecargar = cargada;
        AtencionEnfermeriaEntity a = cargada.getAtencion();

        if (a.getTipoAtencion() == 2) binding.btnInternado.setChecked(true);
        else binding.btnAmbulatorio.setChecked(true);

        binding.switchEmbarazada.setChecked(a.isEmbarazada());
        binding.switchSinObraSocial.setChecked(a.isSinObraSocial());
        binding.etObservaciones.setText(a.getObservaciones() != null ? a.getObservaciones() : "");

        for (PrestacionEnfermeriaEntity p : cargada.getPrestaciones()) {
            TextView tv = cantidadPorTipoId.get(p.getTipoPrestacionId());
            if (tv != null) tv.setText(String.valueOf(p.getCantidad()));
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