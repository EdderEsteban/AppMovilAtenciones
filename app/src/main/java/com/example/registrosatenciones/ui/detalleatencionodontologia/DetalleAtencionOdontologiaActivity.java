package com.example.registrosatenciones.ui.detalleatencionodontologia;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.registrosatenciones.databinding.ActivityDetalleAtencionOdontologiaBinding;
import com.example.registrosatenciones.db.entity.DiagnosticoEntity;
import com.example.registrosatenciones.db.entity.OdontogramaEstadoEntity;
import com.example.registrosatenciones.db.entity.PacienteEntity;
import com.example.registrosatenciones.db.entity.PrestacionOdontologiaEntity;
import com.example.registrosatenciones.db.entity.TipoPrestacionOdontologiaEntity;
import com.example.registrosatenciones.db.relation.AtencionOdontologiaConDetalle;
import com.example.registrosatenciones.ui.odontologia.model.DientesFdi;
import com.example.registrosatenciones.ui.odontologia.model.OdontogramaItem;
import com.example.registrosatenciones.ui.odontologia.model.OdontogramaModelo;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DetalleAtencionOdontologiaActivity extends AppCompatActivity {

    public static final String EXTRA_ATENCION_LOCAL_ID = "atencionLocalId";
    public static final String EXTRA_PACIENTE_LOCAL_ID = "pacienteLocalId";

    private static final String[] TURNO_LABELS = {"", "Ventanilla", "Profesional", "Demanda espontánea", "Interdisciplinario"};

    private ActivityDetalleAtencionOdontologiaBinding binding;
    private DetalleAtencionOdontologiaViewModel viewModel;

    private AtencionOdontologiaConDetalle detalleActual;
    private String nombrePacienteActual = "";
    private Map<Integer, String> nombresPrestaciones = new HashMap<>();
    private Map<Integer, String> nombresDiagnosticos = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetalleAtencionOdontologiaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        long atencionLocalId = getIntent().getLongExtra(EXTRA_ATENCION_LOCAL_ID, -1);
        long pacienteLocalId = getIntent().getLongExtra(EXTRA_PACIENTE_LOCAL_ID, -1);

        viewModel = new ViewModelProvider(this).get(DetalleAtencionOdontologiaViewModel.class);

        binding.odontogramaDetalle.setFilas(Arrays.asList(
                DientesFdi.TEMPORARIOS_SUPERIOR, DientesFdi.TEMPORARIOS_INFERIOR,
                DientesFdi.PERMANENTES_SUPERIOR, DientesFdi.PERMANENTES_INFERIOR));
        binding.odontogramaDetalle.setEditable(false);

        viewModel.observarPaciente(pacienteLocalId).observe(this, paciente -> {
            if (paciente == null) return;
            nombrePacienteActual = paciente.getApellido() + ", " + paciente.getNombre();
            renderizarSiListo();
        });

        viewModel.getTiposPrestacion().observe(this, lista -> {
            nombresPrestaciones = mapearPrestaciones(lista);
            renderizarSiListo();
        });

        viewModel.getDiagnosticos().observe(this, lista -> {
            nombresDiagnosticos = mapearDiagnosticos(lista);
            renderizarSiListo();
        });

        viewModel.observarDetalle(atencionLocalId).observe(this, detalle -> {
            detalleActual = detalle;
            renderizarSiListo();
        });
    }

    private Map<Integer, String> mapearPrestaciones(List<TipoPrestacionOdontologiaEntity> lista) {
        Map<Integer, String> mapa = new HashMap<>();
        if (lista != null) {
            for (TipoPrestacionOdontologiaEntity t : lista) mapa.put(t.getId(), t.getNombre());
        }
        return mapa;
    }

    private Map<Integer, String> mapearDiagnosticos(List<DiagnosticoEntity> lista) {
        Map<Integer, String> mapa = new HashMap<>();
        if (lista != null) {
            for (DiagnosticoEntity d : lista) mapa.put(d.getId(), d.getCodigo() + " — " + d.getDescripcion());
        }
        return mapa;
    }

    private void renderizarSiListo() {
        if (detalleActual == null || detalleActual.getAtencion() == null) return;

        binding.tvNombrePaciente.setText(nombrePacienteActual);

        String tipoConsulta = detalleActual.getAtencion().getTipoConsulta() == 1 ? "1ª vez" : "Ulterior";
        String tipoTurno = TURNO_LABELS[Math.max(0, Math.min(4, detalleActual.getAtencion().getTipoTurno()))];
        binding.tvDetalleAtencion.setText(detalleActual.getAtencion().getFechaRegistroLocal()
                + " · " + tipoConsulta + " · " + tipoTurno);

        String diagnostico = nombresDiagnosticos.get(detalleActual.getAtencion().getDiagnosticoId());
        binding.tvDiagnostico.setText(diagnostico != null ? diagnostico
                : "Diagnóstico #" + detalleActual.getAtencion().getDiagnosticoId());

        OdontogramaModelo modelo = new OdontogramaModelo();
        List<OdontogramaItem> items = new ArrayList<>();
        if (detalleActual.getEstados() != null) {
            for (OdontogramaEstadoEntity e : detalleActual.getEstados()) {
                items.add(new OdontogramaItem(e.getNumeroDiente(), e.getSuperficie(), e.getEstado()));
            }
        }
        modelo.cargar(items);
        binding.odontogramaDetalle.setModelo(modelo);
        binding.odontogramaDetalle.redibujar();

        binding.tvCariesPerm.setText(String.valueOf(detalleActual.getAtencion().getCariesPerm()));
        binding.tvPerdidosPerm.setText(String.valueOf(detalleActual.getAtencion().getPerdidosPerm()));
        binding.tvObturadosPerm.setText(String.valueOf(detalleActual.getAtencion().getObturadosPerm()));
        binding.tvCariesTemp.setText(String.valueOf(detalleActual.getAtencion().getCariesTemp()));
        binding.tvExtraccionTemp.setText(String.valueOf(detalleActual.getAtencion().getExtraccionTemp()));
        binding.tvObturadosTemp.setText(String.valueOf(detalleActual.getAtencion().getObturadosTemp()));

        binding.chipGroupPrestaciones.removeAllViews();
        if (detalleActual.getPrestaciones() != null) {
            for (PrestacionOdontologiaEntity p : detalleActual.getPrestaciones()) {
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
}
