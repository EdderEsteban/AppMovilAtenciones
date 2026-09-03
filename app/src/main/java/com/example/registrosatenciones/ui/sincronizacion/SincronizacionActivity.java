package com.example.registrosatenciones.ui.sincronizacion;

import android.os.Bundle;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.example.registrosatenciones.R;
import com.example.registrosatenciones.adapters.PendienteAdapter;
import com.example.registrosatenciones.databinding.ActivitySincronizacionBinding;
import com.example.registrosatenciones.db.entity.AtencionEnfermeriaEntity;
import com.example.registrosatenciones.db.entity.AtencionOdontologiaEntity;
import com.example.registrosatenciones.db.entity.PacienteEntity;
import com.example.registrosatenciones.ui.common.NavegacionInferiorActivity;
import com.example.registrosatenciones.util.Conectividad;
import com.example.registrosatenciones.util.VentanaEdicion;

import java.util.ArrayList;
import java.util.List;

public class SincronizacionActivity extends NavegacionInferiorActivity {

    private ActivitySincronizacionBinding binding;
    private SincronizacionViewModel viewModel;
    private PendienteAdapter adapter;

    private List<PacienteEntity> pacientesPendientes = new ArrayList<>();
    private List<PacienteEntity> pacientesConError = new ArrayList<>();
    private List<AtencionEnfermeriaEntity> atencionesPendientes = new ArrayList<>();
    private List<AtencionEnfermeriaEntity> atencionesConError = new ArrayList<>();
    private List<AtencionOdontologiaEntity> atencionesOdoPendientes = new ArrayList<>();
    private List<AtencionOdontologiaEntity> atencionesOdoConError = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySincronizacionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(SincronizacionViewModel.class);

        adapter = new PendienteAdapter(this);
        binding.rvPendientes.setLayoutManager(new LinearLayoutManager(this));
        binding.rvPendientes.setAdapter(adapter);

        viewModel.observarPacientesPendientes().observe(this, lista -> {
            pacientesPendientes = lista;
            actualizarLista();
        });

        viewModel.observarPacientesConError().observe(this, lista -> {
            pacientesConError = lista;
            actualizarLista();
        });

        viewModel.observarAtencionesPendientes().observe(this, lista -> {
            atencionesPendientes = lista;
            actualizarLista();
        });

        viewModel.observarAtencionesConError().observe(this, lista -> {
            atencionesConError = lista;
            actualizarLista();
        });

        viewModel.observarAtencionesOdoPendientes().observe(this, lista -> {
            atencionesOdoPendientes = lista;
            actualizarLista();
        });

        viewModel.observarAtencionesOdoConError().observe(this, lista -> {
            atencionesOdoConError = lista;
            actualizarLista();
        });

        viewModel.getSincronizando().observe(this, sincronizando -> {
            binding.btnSincronizar.setEnabled(!sincronizando);
            binding.btnSincronizar.setText(sincronizando ? "Sincronizando..." : "Sincronizar ahora");
            actualizarEstado();
        });

        viewModel.getTic().observe(this, tic -> actualizarLista());

        binding.btnSincronizar.setOnClickListener(v -> viewModel.sincronizarAhora());

        actualizarEstado();

        if (Conectividad.hayConexion(this)) {
            viewModel.sincronizarAhora();
        }

        configurarNavInferior();
    }

    @Override
    protected BottomNavigationView getBottomNavigationView() {
        return binding.bottomNav;
    }

    @Override
    protected int getTabActual() {
        return R.id.nav_sincronizar;
    }

    @Override
    protected void onResume() {
        super.onResume();
        actualizarEstado();
    }

    private void actualizarEstado() {
        boolean online = Conectividad.hayConexion(this);
        int totalPendientes = pacientesPendientes.size() + atencionesPendientes.size() + atencionesOdoPendientes.size();
        int totalError = pacientesConError.size() + atencionesConError.size() + atencionesOdoConError.size();

        if (Boolean.TRUE.equals(viewModel.getSincronizando().getValue())) {
            binding.tvEstado.setText("Sincronizando...");
        } else if (!online) {
            binding.tvEstado.setText("Sin conexión · se sincroniza automáticamente al volver la señal");
        } else if (totalPendientes == 0 && totalError == 0) {
            binding.tvEstado.setText("Todo sincronizado");
        } else if (totalError > 0) {
            binding.tvEstado.setText(totalPendientes + " pendiente(s) · " + totalError + " con error");
        } else {
            binding.tvEstado.setText(totalPendientes + " pendiente(s) por sincronizar");
        }

        binding.tvTituloPendientes.setText("Pendientes (" + (totalPendientes + totalError) + ")");
    }

    private void actualizarLista() {
        List<ItemPendiente> items = new ArrayList<>();

        for (PacienteEntity p : pacientesConError) {
            items.add(new ItemPendiente(p.getApellido() + ", " + p.getNombre(), "No se pudo crear — revisá los datos", true));
        }
        for (AtencionEnfermeriaEntity a : atencionesConError) {
            String tipo = a.getTipoAtencion() == 1 ? "Ambulatorio" : "Internado";
            items.add(new ItemPendiente("Atención · " + tipo, "No se pudo guardar — revisá los datos", true));
        }
        for (AtencionOdontologiaEntity a : atencionesOdoConError) {
            String tipo = a.getTipoConsulta() == 1 ? "1ª vez" : "Ulterior";
            items.add(new ItemPendiente("Atención odont. · " + tipo, "No se pudo guardar — revisá los datos", true));
        }
        for (PacienteEntity p : pacientesPendientes) {
            items.add(new ItemPendiente(p.getApellido() + ", " + p.getNombre(), "Paciente nuevo · se buscará por DNI", false));
        }
        for (AtencionEnfermeriaEntity a : atencionesPendientes) {
            String tipo = a.getTipoAtencion() == 1 ? "Ambulatorio" : "Internado";
            String tiempoRestante = VentanaEdicion.formatearRestante(a.getFechaRegistroLocal());
            items.add(new ItemPendiente("Atención · " + tipo, a.getFechaRegistroLocal(), false, tiempoRestante));
        }
        for (AtencionOdontologiaEntity a : atencionesOdoPendientes) {
            String tipo = a.getTipoConsulta() == 1 ? "1ª vez" : "Ulterior";
            String tiempoRestante = VentanaEdicion.formatearRestante(a.getFechaRegistroLocal());
            items.add(new ItemPendiente("Atención odont. · " + tipo, a.getFechaRegistroLocal(), false, tiempoRestante));
        }

        adapter.setItems(items);
        actualizarEstado();
    }
}
