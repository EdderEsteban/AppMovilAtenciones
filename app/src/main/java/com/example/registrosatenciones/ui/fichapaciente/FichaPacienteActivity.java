package com.example.registrosatenciones.ui.fichapaciente;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.registrosatenciones.adapters.TimelineAtencionesAdapter;
import com.example.registrosatenciones.adapters.TimelineOdontologiaAdapter;
import com.example.registrosatenciones.databinding.ActivityFichaPacienteBinding;
import com.example.registrosatenciones.ui.detalleatencionodontologia.DetalleAtencionOdontologiaActivity;
import com.example.registrosatenciones.ui.registraratencion.RegistrarAtencionActivity;
import com.example.registrosatenciones.ui.registraratencionodontologia.RegistrarAtencionOdontologiaActivity;

public class FichaPacienteActivity extends AppCompatActivity {

    public static final String EXTRA_PACIENTE_LOCAL_ID = "pacienteLocalId";

    private ActivityFichaPacienteBinding binding;
    private FichaPacienteViewModel viewModel;
    private long pacienteLocalId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFichaPacienteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        pacienteLocalId = getIntent().getLongExtra(EXTRA_PACIENTE_LOCAL_ID, -1);

        viewModel = new ViewModelProvider(this).get(FichaPacienteViewModel.class);

        viewModel.observarPaciente(pacienteLocalId).observe(this, paciente -> {
            if (paciente == null) return;
            binding.tvNombrePaciente.setText(paciente.getApellido() + ", " + paciente.getNombre());

            StringBuilder detalle = new StringBuilder("DNI ").append(paciente.getDni());
            if (paciente.getEdad() != null) detalle.append(" · ").append(paciente.getEdad()).append(" años");
            if (paciente.getSexo() != null) detalle.append(" · ").append(paciente.getSexo());
            binding.tvDetallePaciente.setText(detalle.toString());
        });

        if (viewModel.esOdontologo()) {
            configurarTimelineOdontologia();
        } else {
            configurarTimelineEnfermeria();
        }
    }

    private void configurarTimelineEnfermeria() {
        TimelineAtencionesAdapter adapter = new TimelineAtencionesAdapter(this);
        binding.rvTimeline.setLayoutManager(new LinearLayoutManager(this));
        binding.rvTimeline.setAdapter(adapter);

        viewModel.observarAtenciones(pacienteLocalId).observe(this, atenciones -> {
            adapter.setAtenciones(atenciones);
            binding.tvSinAtenciones.setVisibility(atenciones == null || atenciones.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.observarCatalogo().observe(this, adapter::setCatalogo);

        binding.btnNuevaAtencion.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegistrarAtencionActivity.class);
            intent.putExtra(RegistrarAtencionActivity.EXTRA_PACIENTE_LOCAL_ID, pacienteLocalId);
            startActivity(intent);
        });
    }

    private void configurarTimelineOdontologia() {
        TimelineOdontologiaAdapter adapter = new TimelineOdontologiaAdapter(this, atencion -> {
            Intent intent = new Intent(this, DetalleAtencionOdontologiaActivity.class);
            intent.putExtra(DetalleAtencionOdontologiaActivity.EXTRA_ATENCION_LOCAL_ID, atencion.getAtencion().getLocalId());
            intent.putExtra(DetalleAtencionOdontologiaActivity.EXTRA_PACIENTE_LOCAL_ID, pacienteLocalId);
            startActivity(intent);
        });
        binding.rvTimeline.setLayoutManager(new LinearLayoutManager(this));
        binding.rvTimeline.setAdapter(adapter);

        viewModel.observarAtencionesOdo(pacienteLocalId).observe(this, atenciones -> {
            adapter.setAtenciones(atenciones);
            binding.tvSinAtenciones.setVisibility(atenciones == null || atenciones.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.observarCatalogoOdo().observe(this, adapter::setCatalogoPrestaciones);
        viewModel.getDiagnosticos().observe(this, adapter::setDiagnosticos);

        binding.btnNuevaAtencion.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegistrarAtencionOdontologiaActivity.class);
            intent.putExtra(RegistrarAtencionOdontologiaActivity.EXTRA_PACIENTE_LOCAL_ID, pacienteLocalId);
            startActivity(intent);
        });
    }
}
