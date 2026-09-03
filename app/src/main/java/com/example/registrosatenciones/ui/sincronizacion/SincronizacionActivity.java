package com.example.registrosatenciones.ui.sincronizacion;

import android.os.Bundle;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.example.registrosatenciones.R;
import com.example.registrosatenciones.adapters.PendienteAdapter;
import com.example.registrosatenciones.databinding.ActivitySincronizacionBinding;
import com.example.registrosatenciones.ui.common.NavegacionInferiorActivity;
import com.example.registrosatenciones.util.Conectividad;

import java.util.ArrayList;
import java.util.List;

public class SincronizacionActivity extends NavegacionInferiorActivity {

    private ActivitySincronizacionBinding binding;
    private SincronizacionViewModel viewModel;
    private PendienteAdapter adapter;

    // Copia local solo para que actualizarEstado() cuente sin volver a leer el
    // ViewModel; la lista ya viene armada (título, subtítulo, estado) desde ahí.
    private List<ItemPendiente> items = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySincronizacionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(SincronizacionViewModel.class);

        adapter = new PendienteAdapter(this);
        binding.rvPendientes.setLayoutManager(new LinearLayoutManager(this));
        binding.rvPendientes.setAdapter(adapter);

        viewModel.getItems().observe(this, lista -> {
            items = lista != null ? lista : new ArrayList<>();
            adapter.setItems(items);
            actualizarEstado();
        });

        viewModel.getSincronizando().observe(this, sincronizando -> {
            binding.btnSincronizar.setEnabled(!sincronizando);
            binding.btnSincronizar.setText(sincronizando ? "Sincronizando..." : "Sincronizar ahora");
            actualizarEstado();
        });

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
        int totalError = 0;
        for (ItemPendiente item : items) {
            if (item.isEsError()) totalError++;
        }
        int totalPendientes = items.size() - totalError;

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

        binding.tvTituloPendientes.setText("Pendientes (" + items.size() + ")");
    }
}
