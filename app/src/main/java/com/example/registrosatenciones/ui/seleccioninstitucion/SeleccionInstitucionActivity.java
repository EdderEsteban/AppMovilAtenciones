package com.example.registrosatenciones.ui.seleccioninstitucion;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.registrosatenciones.adapters.InstitucionAdapter;
import com.example.registrosatenciones.databinding.ActivitySeleccionInstitucionBinding;
import com.example.registrosatenciones.response.InstitucionResponse;

import java.io.Serializable;
import java.util.List;

public class SeleccionInstitucionActivity extends AppCompatActivity {

    private ActivitySeleccionInstitucionBinding binding;
    private SeleccionInstitucionViewModel viewModel;
    private InstitucionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySeleccionInstitucionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(SeleccionInstitucionViewModel.class);

        adapter = new InstitucionAdapter(this, institucionId -> viewModel.onInstitucionSeleccionada(institucionId));
        binding.rvInstituciones.setLayoutManager(new LinearLayoutManager(this));
        binding.rvInstituciones.setAdapter(adapter);

        @SuppressWarnings("unchecked")
        List<InstitucionResponse> instituciones = (List<InstitucionResponse>)
                (Serializable) getIntent().getSerializableExtra("instituciones");
        viewModel.setInstituciones(instituciones);

        viewModel.getInstituciones().observe(this, adapter::setInstituciones);
        viewModel.getInstitucionSeleccionadaId().observe(this, adapter::setSeleccionada);

        viewModel.getCargando().observe(this, cargando -> {
            binding.progressSeleccion.setVisibility(cargando ? View.VISIBLE : View.GONE);
            binding.btnContinuar.setEnabled(!cargando);
        });

        binding.btnContinuar.setOnClickListener(v -> viewModel.onContinuarClick());
    }
}
