package com.example.registrosatenciones.ui.altapaciente;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.registrosatenciones.adapters.ObraSocialAdapter;
import com.example.registrosatenciones.databinding.ActivityAltaPacienteBinding;
import com.example.registrosatenciones.databinding.DialogSeleccionarObraSocialBinding;
import com.example.registrosatenciones.db.entity.ObraSocialEntity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AltaPacienteActivity extends AppCompatActivity {

    private ActivityAltaPacienteBinding binding;
    private AltaPacienteViewModel viewModel;
    private String fechaNacimientoIso; // yyyy-MM-dd, lo que se manda al ViewModel
    private Integer obraSocialIdSeleccionada;
    private String obraSocialNombreSeleccionada;
    private List<ObraSocialEntity> listaObrasSociales = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAltaPacienteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AltaPacienteViewModel.class);

        viewModel.getObrasSociales().observe(this, lista ->
                listaObrasSociales = lista != null ? lista : new ArrayList<>());

        binding.etFechaNacimiento.setOnClickListener(v -> mostrarSelectorFecha());
        binding.etObraSocial.setOnClickListener(v -> mostrarSelectorObraSocial());

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
                    obraSocialIdSeleccionada,
                    obraSocialNombreSeleccionada
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

    private void mostrarSelectorObraSocial() {
        DialogSeleccionarObraSocialBinding dialogBinding = DialogSeleccionarObraSocialBinding.inflate(getLayoutInflater());
        RecyclerView rv = dialogBinding.rvObrasSociales;
        rv.setLayoutManager(new LinearLayoutManager(this));

        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogBinding.getRoot()).create();

        ObraSocialAdapter adapter = new ObraSocialAdapter(this, obraSocial -> {
            obraSocialIdSeleccionada = obraSocial.getId();
            obraSocialNombreSeleccionada = obraSocial.getNombre();
            binding.etObraSocial.setText(obraSocial.getNombre());
            dialog.dismiss();
        });
        adapter.setObrasSociales(listaObrasSociales);
        rv.setAdapter(adapter);

        dialogBinding.etBuscarObraSocial.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filtrar(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        dialog.show();
    }
}
