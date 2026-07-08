package com.example.registrosatenciones.ui.pacientes;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.registrosatenciones.adapters.PacienteAdapter;
import com.example.registrosatenciones.databinding.ActivityPacientesBinding;
import com.example.registrosatenciones.ui.altapaciente.AltaPacienteActivity;

public class PacientesActivity extends AppCompatActivity {

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

        viewModel.getPacientes().observe(this, adapter::setPacientes);

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
    }
}