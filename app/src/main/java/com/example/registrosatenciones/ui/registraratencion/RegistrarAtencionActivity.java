package com.example.registrosatenciones.ui.registraratencion;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.registrosatenciones.R;
import com.example.registrosatenciones.adapters.ObraSocialAdapter;
import com.example.registrosatenciones.databinding.ActivityRegistrarAtencionBinding;
import com.example.registrosatenciones.databinding.DialogSeleccionarObraSocialBinding;
import com.example.registrosatenciones.db.entity.AtencionEnfermeriaEntity;
import com.example.registrosatenciones.db.entity.ObraSocialEntity;
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
    private List<ObraSocialEntity> listaObrasSociales = new ArrayList<>();

    private final Map<Integer, TextView> cantidadPorTipoId = new HashMap<>();

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
            // El divisor separa Embarazada de Sin obra social; en edición ese bloque no está.
            binding.divisorEmbarazada.setVisibility(esVaron || modoEdicion ? View.GONE : View.VISIBLE);

            // Obra social: si ya tiene una cargada, se muestra de solo lectura y no se pide de nuevo
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

        viewModel.observarCatalogo().observe(this, this::renderizarPrestaciones);

        viewModel.getObrasSociales().observe(this, lista ->
                listaObrasSociales = lista != null ? lista : new ArrayList<>());

        viewModel.getGuardadoExitoso().observe(this, guardado -> {
            if (guardado != null && guardado) finish();
        });

        if (modoEdicion) {
            binding.btnGuardar.setText("Guardar cambios");
            ocultarBloqueObraSocial();
            viewModel.getAtencionEnEdicion().observe(this, cargada -> {
                intentarPrecargarFormulario();
                aplicarCantidadesEnEdicion();
            });
            viewModel.cargarParaEditar(atencionLocalId);
        }

        binding.switchSinObraSocial.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (pacienteTieneObraSocial) return;
            actualizarVisibilidadNuevaObraSocial();
        });

        binding.etNuevaObraSocial.setOnClickListener(v -> mostrarSelectorObraSocial());

        binding.btnGuardar.setOnClickListener(v -> {
            int tipoAtencion = binding.btnInternado.isChecked() ? 2 : 1;
            boolean embarazada = binding.switchEmbarazada.isChecked();
            String observaciones = binding.etObservaciones.getText().toString();

            if (modoEdicion) {
                // La obra social no se manda: en edición no se edita.
                viewModel.actualizarAtencion(atencionLocalId, tipoAtencion, embarazada,
                        observaciones, obtenerPrestacionesSeleccionadas());
            } else {
                viewModel.guardarAtencion(pacienteLocalId, tipoAtencion, embarazada,
                        binding.switchSinObraSocial.isChecked(), observaciones,
                        viewModel.getNuevaObraSocialSeleccionadaId(),
                        viewModel.getNuevaObraSocialSeleccionadaNombre(),
                        obtenerPrestacionesSeleccionadas());
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

        viewModel.marcarCatalogoListo();
        intentarPrecargarFormulario();
        aplicarCantidadesEnEdicion();
    }

    // La atención a editar y el catálogo llegan por LiveData independientes: solo
    // dispara cuando las dos fuentes ya llegaron y todavía no se precargó,
    // porque reaplicar la precarga pisaría sin avisar lo que el usuario ya tocó.
    // El estado vive en el ViewModel y no acá, así que una rotación tampoco la
    // vuelve a aplicar: los campos con id los restaura Android solo.
    private void intentarPrecargarFormulario() {
        if (!viewModel.debePrecargarFormulario()) return;
        precargarFormulario(viewModel.getAtencionEnEdicion().getValue());
        viewModel.marcarFormularioPrecargado();
    }

    private void precargarFormulario(AtencionConPrestaciones cargada) {
        if (cargada == null) return;
        AtencionEnfermeriaEntity a = cargada.getAtencion();

        if (a.getTipoAtencion() == 2) binding.btnInternado.setChecked(true);
        else binding.btnAmbulatorio.setChecked(true);

        binding.switchEmbarazada.setChecked(a.isEmbarazada());
        binding.etObservaciones.setText(a.getObservaciones() != null ? a.getObservaciones() : "");
    }

    // Las cantidades van aparte de la precarga: viven en las vistas que
    // renderizarPrestaciones destruye y reconstruye en cero cada vez que el
    // catálogo emite. Por eso se reaplican en cada reconstrucción, y no una
    // sola vez como los campos de arriba.
    private void aplicarCantidadesEnEdicion() {
        AtencionConPrestaciones cargada = viewModel.getAtencionEnEdicion().getValue();
        if (cargada == null) return;
        viewModel.sembrarCantidadesSiVacio(cargada.getPrestaciones());
        for (Map.Entry<Integer, TextView> entry : cantidadPorTipoId.entrySet()) {
            entry.getValue().setText(String.valueOf(viewModel.getCantidad(entry.getKey())));
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