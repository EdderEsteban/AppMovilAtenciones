package com.example.registrosatenciones.ui.login;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.registrosatenciones.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private LoginViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        binding.btnIngresar.setOnClickListener(v -> viewModel.iniciarSesion(
                binding.etEmail.getText().toString().trim(),
                binding.etPassword.getText().toString()
        ));

        viewModel.getCargando().observe(this, cargando -> {
            binding.progressLogin.setVisibility(cargando ? android.view.View.VISIBLE : android.view.View.GONE);
            binding.btnIngresar.setEnabled(!cargando);
        });
    }
}