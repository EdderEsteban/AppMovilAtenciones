package com.example.registrosatenciones.ui.detalleatencionenfermeria;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.registrosatenciones.databinding.ActivityDetalleAtencionEnfermeriaBinding;
import com.example.registrosatenciones.db.entity.PrestacionEnfermeriaEntity;
import com.example.registrosatenciones.db.entity.TipoPrestacionEnfermeriaEntity;
import com.example.registrosatenciones.db.relation.AtencionConPrestaciones;
import com.example.registrosatenciones.response.AtencionEnfermeriaDetalleResponse;
import com.example.registrosatenciones.response.PrestacionDetalleResponse;
import com.google.android.material.chip.Chip;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DetalleAtencionEnfermeriaActivity extends AppCompatActivity {

    public static final String EXTRA_ATENCION_LOCAL_ID = "atencionLocalId";
    public static final String EXTRA_ATENCION_SERVER_ID = "atencionServerId";
    public static final String EXTRA_PACIENTE_LOCAL_ID = "pacienteLocalId";

    private ActivityDetalleAtencionEnfermeriaBinding binding;
    private DetalleAtencionEnfermeriaViewModel viewModel;

    private AtencionConPrestaciones detalleActual;
    private String nombrePacienteActual = "";
    private Map<Integer, String> nombresPrestaciones = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetalleAtencionEnfermeriaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        long atencionLocalId = getIntent().getLongExtra(EXTRA_ATENCION_LOCAL_ID, -1);
        long pacienteLocalId = getIntent().getLongExtra(EXTRA_PACIENTE_LOCAL_ID, -1);
        int atencionServerId = getIntent().getIntExtra(EXTRA_ATENCION_SERVER_ID, -1);

        viewModel = new ViewModelProvider(this).get(DetalleAtencionEnfermeriaViewModel.class);

        if (atencionServerId > 0) {
            viewModel.getDetalleOnline().observe(this, this::renderizarOnline);
            viewModel.getErrorOnline().observe(this, error -> {
                if (error != null && error) {
                    Toast.makeText(this, "No se pudo cargar el detalle", Toast.LENGTH_LONG).show();
                    finish();
                }
            });
            viewModel.cargarOnline(atencionServerId);
        } else {
            viewModel.observarPaciente(pacienteLocalId).observe(this, paciente -> {
                if (paciente == null) return;
                nombrePacienteActual = paciente.getApellido() + ", " + paciente.getNombre();
                renderizarSiListo();
            });

            viewModel.observarCatalogo().observe(this, lista -> {
                nombresPrestaciones = mapearPrestaciones(lista);
                renderizarSiListo();
            });

            viewModel.observarDetalle(atencionLocalId).observe(this, detalle -> {
                detalleActual = detalle;
                renderizarSiListo();
            });
        }
    }

    private Map<Integer, String> mapearPrestaciones(List<TipoPrestacionEnfermeriaEntity> lista) {
        Map<Integer, String> mapa = new HashMap<>();
        if (lista != null) {
            for (TipoPrestacionEnfermeriaEntity t : lista) mapa.put(t.getId(), t.getNombrePrestacion());
        }
        return mapa;
    }

    private void renderizarSiListo() {
        if (detalleActual == null || detalleActual.getAtencion() == null) return;

        binding.tvNombrePaciente.setText(nombrePacienteActual);

        String tipoAtencion = detalleActual.getAtencion().getTipoAtencion() == 2 ? "Internado" : "Ambulatorio";
        StringBuilder detalle = new StringBuilder(detalleActual.getAtencion().getFechaRegistroLocal())
                .append(" · ").append(tipoAtencion);
        if (detalleActual.getAtencion().isEmbarazada()) detalle.append(" · Embarazada");
        binding.tvDetalleAtencion.setText(detalle.toString());

        binding.chipGroupPrestaciones.removeAllViews();
        if (detalleActual.getPrestaciones() != null) {
            for (PrestacionEnfermeriaEntity p : detalleActual.getPrestaciones()) {
                String nombre = nombresPrestaciones.getOrDefault(p.getTipoPrestacionId(), "Prestación #" + p.getTipoPrestacionId());
                Chip chip = new Chip(this);
                chip.setText(nombre + " ×" + p.getCantidad());
                chip.setClickable(false);
                chip.setCheckable(false);
                binding.chipGroupPrestaciones.addView(chip);
            }
        }

        String observaciones = detalleActual.getAtencion().getObservaciones();
        if (observaciones != null && !observaciones.isEmpty()) {
            binding.tvObservaciones.setText("Observaciones: " + observaciones);
            binding.tvObservaciones.setVisibility(View.VISIBLE);
        } else {
            binding.tvObservaciones.setVisibility(View.GONE);
        }
    }

    private void renderizarOnline(AtencionEnfermeriaDetalleResponse r) {
        if (r == null) return;

        binding.tvNombrePaciente.setText(r.getPacienteNombre());
        StringBuilder detalle = new StringBuilder(r.getFecha()).append(" · ").append(r.getTipoAtencion());
        if (r.isEmbarazada()) detalle.append(" · Embarazada");
        binding.tvDetalleAtencion.setText(detalle.toString());

        String centro = (r.getInstitucion() != null ? r.getInstitucion() : "");
        if (r.getProfesional() != null && !r.getProfesional().isEmpty()) centro += " · " + r.getProfesional();
        binding.tvCentroProfesional.setText(centro);
        binding.tvCentroProfesional.setVisibility(View.VISIBLE);

        binding.chipGroupPrestaciones.removeAllViews();
        if (r.getPrestaciones() != null) {
            for (PrestacionDetalleResponse p : r.getPrestaciones()) {
                Chip chip = new Chip(this);
                chip.setText(p.getNombre() + " ×" + p.getCantidad());
                chip.setClickable(false);
                chip.setCheckable(false);
                binding.chipGroupPrestaciones.addView(chip);
            }
        }

        if (r.getObservaciones() != null && !r.getObservaciones().isEmpty()) {
            binding.tvObservaciones.setText("Observaciones: " + r.getObservaciones());
            binding.tvObservaciones.setVisibility(View.VISIBLE);
        } else {
            binding.tvObservaciones.setVisibility(View.GONE);
        }
    }
}
