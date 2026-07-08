package com.example.registrosatenciones.ui.pacientes;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.example.registrosatenciones.R;
import com.example.registrosatenciones.adapters.PacienteAdapter;
import com.example.registrosatenciones.databinding.ActivityPacientesBinding;
import com.example.registrosatenciones.ui.altapaciente.AltaPacienteActivity;
import com.example.registrosatenciones.ui.common.NavegacionInferiorActivity;

public class PacientesActivity extends NavegacionInferiorActivity {

    private ActivityPacientesBinding binding;
    private PacientesViewModel viewModel;
    private PacienteAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPacientesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(PacientesViewModel.class);

        adapter = new PacienteAdapter(this, paciente -> {
            Intent intent = new Intent(this, com.example.registrosatenciones.ui.fichapaciente.FichaPacienteActivity.class);
            intent.putExtra(com.example.registrosatenciones.ui.fichapaciente.FichaPacienteActivity.EXTRA_PACIENTE_LOCAL_ID, paciente.getLocalId());
            startActivity(intent);
        });
        binding.rvPacientes.setLayoutManager(new LinearLayoutManager(this));
        binding.rvPacientes.setAdapter(adapter);

        viewModel.getPacientes().observe(this, lista -> {
            adapter.setPacientes(lista);
            boolean buscoAlgo = binding.etBuscar.getText() != null && binding.etBuscar.getText().length() >= 2;
            binding.tvSinResultados.setVisibility(buscoAlgo && (lista == null || lista.isEmpty()) ? View.VISIBLE : View.GONE);
        });

        viewModel.getSinConexion().observe(this, sinConexion ->
                binding.tvSinConexion.setVisibility(sinConexion ? View.VISIBLE : View.GONE));

        binding.etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.buscar(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.btnNuevo.setOnClickListener(v ->
                startActivity(new Intent(this, AltaPacienteActivity.class)));

        configurarNavInferior();
    }

    @Override
    protected BottomNavigationView getBottomNavigationView() {
        return binding.bottomNav;
    }

    @Override
    protected int getTabActual() {
        return R.id.nav_pacientes;
    }
}