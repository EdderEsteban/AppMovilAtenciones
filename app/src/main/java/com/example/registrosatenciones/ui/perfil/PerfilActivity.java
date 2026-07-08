package com.example.registrosatenciones.ui.perfil;

import android.os.Bundle;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.example.registrosatenciones.R;
import com.example.registrosatenciones.databinding.ActivityPerfilBinding;
import com.example.registrosatenciones.ui.common.NavegacionInferiorActivity;

public class PerfilActivity extends NavegacionInferiorActivity {

    private ActivityPerfilBinding binding;
    private PerfilViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPerfilBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(PerfilViewModel.class);

        binding.tvNombreCompleto.setText(viewModel.getNombreCompleto());
        binding.tvRolInstitucion.setText(viewModel.getRol() + " · " + viewModel.getInstitucionActiva());
        binding.etEmail.setText(viewModel.getEmailActual());

        viewModel.getCargando().observe(this, cargando -> {
            binding.progressPerfil.setVisibility(cargando ? View.VISIBLE : View.GONE);
            binding.btnGuardar.setEnabled(!cargando);
        });

        viewModel.getActualizadoExitoso().observe(this, exitoso -> {
            if (exitoso != null && exitoso) {
                binding.etContrasenaActual.setText("");
                binding.etContrasenaNueva.setText("");
                binding.etConfirmarContrasenaNueva.setText("");
            }
        });

        binding.btnGuardar.setOnClickListener(v -> viewModel.actualizarPerfil(
                binding.etEmail.getText().toString(),
                binding.etContrasenaActual.getText().toString(),
                binding.etContrasenaNueva.getText().toString(),
                binding.etConfirmarContrasenaNueva.getText().toString()
        ));

        binding.btnCerrarSesion.setOnClickListener(v -> viewModel.cerrarSesion());

        configurarNavInferior();
    }

    @Override
    protected BottomNavigationView getBottomNavigationView() {
        return binding.bottomNav;
    }

    @Override
    protected int getTabActual() {
        return R.id.nav_perfil;
    }
}
