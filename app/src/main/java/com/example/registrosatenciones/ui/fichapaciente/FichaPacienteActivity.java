package com.example.registrosatenciones.ui.fichapaciente;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.registrosatenciones.adapters.TimelineAtencionesAdapter;
import com.example.registrosatenciones.adapters.TimelineHistoriaAdapter;
import com.example.registrosatenciones.adapters.TimelineOdontologiaAdapter;
import com.example.registrosatenciones.databinding.ActivityFichaPacienteBinding;
import com.example.registrosatenciones.ui.detalleatencionenfermeria.DetalleAtencionEnfermeriaActivity;
import com.example.registrosatenciones.ui.detalleatencionodontologia.DetalleAtencionOdontologiaActivity;
import com.example.registrosatenciones.ui.historiaclinica.DecisorModoHistoria;
import com.example.registrosatenciones.ui.historiaclinica.ItemHistoria;
import com.example.registrosatenciones.ui.historiaclinica.ModoHistoria;
import com.example.registrosatenciones.ui.registraratencion.RegistrarAtencionActivity;
import com.example.registrosatenciones.ui.registraratencionodontologia.RegistrarAtencionOdontologiaActivity;

public class FichaPacienteActivity extends AppCompatActivity {

    public static final String EXTRA_PACIENTE_LOCAL_ID = "pacienteLocalId";

    private ActivityFichaPacienteBinding binding;
    private FichaPacienteViewModel viewModel;
    private long pacienteLocalId;
    private boolean timelineConfigurado = false;

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

            if (!timelineConfigurado) {
                timelineConfigurado = true;
                ModoHistoria modo = DecisorModoHistoria.decidir(viewModel.hayConexion(), paciente.getServerId());
                if (modo == ModoHistoria.ONLINE) {
                    configurarTimelineOnline(paciente.getServerId());
                } else if (viewModel.esOdontologo()) {
                    configurarTimelineOdontologia();
                } else {
                    configurarTimelineEnfermeria();
                }
                configurarBotonNuevaAtencion();
            }
        });
    }

    private void configurarTimelineOnline(int serverId) {
        binding.tvModoHistoria.setText("Historia clínica completa · todos los centros");
        binding.tvModoHistoria.setVisibility(View.VISIBLE);

        TimelineHistoriaAdapter adapter = new TimelineHistoriaAdapter(this, item -> {
            boolean esPendienteLocal = item.getFuente() == ItemHistoria.Fuente.LOCAL;
            Intent intent;
            if (item.esOdontologia()) {
                intent = new Intent(this, DetalleAtencionOdontologiaActivity.class);
                if (esPendienteLocal) {
                    intent.putExtra(DetalleAtencionOdontologiaActivity.EXTRA_ATENCION_LOCAL_ID, item.getLocalId());
                } else {
                    intent.putExtra(DetalleAtencionOdontologiaActivity.EXTRA_ATENCION_SERVER_ID, (int) item.getServerId());
                }
                intent.putExtra(DetalleAtencionOdontologiaActivity.EXTRA_PACIENTE_LOCAL_ID, pacienteLocalId);
            } else {
                intent = new Intent(this, DetalleAtencionEnfermeriaActivity.class);
                if (esPendienteLocal) {
                    intent.putExtra(DetalleAtencionEnfermeriaActivity.EXTRA_ATENCION_LOCAL_ID, item.getLocalId());
                } else {
                    intent.putExtra(DetalleAtencionEnfermeriaActivity.EXTRA_ATENCION_SERVER_ID, (int) item.getServerId());
                }
                intent.putExtra(DetalleAtencionEnfermeriaActivity.EXTRA_PACIENTE_LOCAL_ID, pacienteLocalId);
            }
            startActivity(intent);
        });
        binding.rvTimeline.setLayoutManager(new LinearLayoutManager(this));
        binding.rvTimeline.setAdapter(adapter);

        viewModel.getHistoriaOnline().observe(this, items -> {
            adapter.setItems(items);
            binding.tvSinAtenciones.setVisibility(items == null || items.isEmpty() ? View.VISIBLE : View.GONE);
        });
        viewModel.getErrorOnline().observe(this, error -> {
            if (error != null && error) {
                Toast.makeText(this, "No se pudo traer la HC completa, mostrando lo local", Toast.LENGTH_LONG).show();
                binding.tvModoHistoria.setText("Sin conexión · registros de este dispositivo");
                if (viewModel.esOdontologo()) configurarTimelineOdontologia(); else configurarTimelineEnfermeria();
            }
        });

        viewModel.cargarHistoriaOnline(serverId, pacienteLocalId);
    }

    private void configurarTimelineEnfermeria() {
        TimelineAtencionesAdapter adapter = new TimelineAtencionesAdapter(this, atencion -> {
            Intent intent = new Intent(this, DetalleAtencionEnfermeriaActivity.class);
            intent.putExtra(DetalleAtencionEnfermeriaActivity.EXTRA_ATENCION_LOCAL_ID, atencion.getAtencion().getLocalId());
            intent.putExtra(DetalleAtencionEnfermeriaActivity.EXTRA_PACIENTE_LOCAL_ID, pacienteLocalId);
            startActivity(intent);
        });
        binding.rvTimeline.setLayoutManager(new LinearLayoutManager(this));
        binding.rvTimeline.setAdapter(adapter);

        viewModel.observarAtenciones(pacienteLocalId).observe(this, atenciones -> {
            adapter.setAtenciones(atenciones);
            binding.tvSinAtenciones.setVisibility(atenciones == null || atenciones.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.observarCatalogo().observe(this, adapter::setCatalogo);
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
    }

    private void configurarBotonNuevaAtencion() {
        binding.btnNuevaAtencion.setOnClickListener(v -> {
            Intent intent = viewModel.esOdontologo()
                    ? new Intent(this, RegistrarAtencionOdontologiaActivity.class)
                    : new Intent(this, RegistrarAtencionActivity.class);
            intent.putExtra(RegistrarAtencionActivity.EXTRA_PACIENTE_LOCAL_ID, pacienteLocalId);
            startActivity(intent);
        });
    }
}
