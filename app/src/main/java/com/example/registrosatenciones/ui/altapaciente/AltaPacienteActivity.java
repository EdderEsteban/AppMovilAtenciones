package com.example.registrosatenciones.ui.altapaciente;

import android.app.DatePickerDialog;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.registrosatenciones.databinding.ActivityAltaPacienteBinding;

import java.util.Calendar;
import java.util.Locale;

public class AltaPacienteActivity extends AppCompatActivity {

    private ActivityAltaPacienteBinding binding;
    private AltaPacienteViewModel viewModel;
    private String fechaNacimientoIso; // yyyy-MM-dd, lo que se manda al ViewModel

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAltaPacienteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AltaPacienteViewModel.class);

        binding.etFechaNacimiento.setOnClickListener(v -> mostrarSelectorFecha());

        binding.btnGuardar.setOnClickListener(v -> {
            String sexo = binding.btnSexoF.isChecked() ? "F"
                    : binding.btnSexoM.isChecked() ? "M" : null;

            viewModel.guardarPaciente(
                    binding.etDni.getText().toString(),
                    binding.etApellido.getText().toString(),
                    binding.etNombre.getText().toString(),
                    fechaNacimientoIso,
                    sexo,
                    binding.etDomicilio.getText().toString(),
                    binding.etTelefono.getText().toString(),
                    binding.etObraSocial.getText().toString()
            );
        });
    }

    private void mostrarSelectorFecha() {
        Calendar hoy = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            fechaNacimientoIso = String.format(Locale.ROOT, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            binding.etFechaNacimiento.setText(String.format(Locale.ROOT, "%02d/%02d/%04d", dayOfMonth, month + 1, year));
        }, hoy.get(Calendar.YEAR), hoy.get(Calendar.MONTH), hoy.get(Calendar.DAY_OF_MONTH)).show();
    }
}
