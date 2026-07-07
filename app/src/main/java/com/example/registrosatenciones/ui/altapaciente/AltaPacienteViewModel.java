package com.example.registrosatenciones.ui.altapaciente;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.example.registrosatenciones.db.AppDatabase;
import com.example.registrosatenciones.db.SyncEstado;
import com.example.registrosatenciones.db.dao.PacienteDao;
import com.example.registrosatenciones.db.entity.PacienteEntity;
import com.example.registrosatenciones.ui.pacientes.PacientesActivity;
import com.example.registrosatenciones.util.AppExecutors;

public class AltaPacienteViewModel extends AndroidViewModel {

    private final Context context;
    private final PacienteDao pacienteDao;

    public AltaPacienteViewModel(@NonNull Application application) {
        super(application);
        context = application.getApplicationContext();
        pacienteDao = AppDatabase.getInstancia(context).pacienteDao();
    }

    public void guardarPaciente(String dni, String apellido, String nombre, String fechaNacimiento,
                                String sexo, String domicilio, String telefono, String obraSocial) {
        if (TextUtils.isEmpty(dni) || TextUtils.isEmpty(apellido) || TextUtils.isEmpty(nombre)
                || TextUtils.isEmpty(fechaNacimiento) || TextUtils.isEmpty(sexo)) {
            Toast.makeText(context, "Completá DNI, apellido, nombre, fecha de nacimiento y sexo", Toast.LENGTH_SHORT).show();
            return;
        }

        String dniLimpio = dni.trim();

        AppExecutors.io().execute(() -> {
            PacienteEntity existente = pacienteDao.buscarPorDni(dniLimpio);
            if (existente != null) {
                AppExecutors.ejecutarEnUI(() ->
                        Toast.makeText(context, "Ya existe un paciente con ese DNI", Toast.LENGTH_LONG).show());
                return;
            }

            PacienteEntity nuevo = new PacienteEntity();
            nuevo.setDni(dniLimpio);
            nuevo.setApellido(apellido.trim());
            nuevo.setNombre(nombre.trim());
            nuevo.setFechaNacimiento(fechaNacimiento);
            nuevo.setSexo(sexo);
            nuevo.setDomicilio(TextUtils.isEmpty(domicilio) ? null : domicilio.trim());
            nuevo.setTelefono(TextUtils.isEmpty(telefono) ? null : telefono.trim());
            nuevo.setObraSocial(TextUtils.isEmpty(obraSocial) ? null : obraSocial.trim());
            nuevo.setSyncState(SyncEstado.PENDIENTE);
            pacienteDao.insertar(nuevo);

            AppExecutors.ejecutarEnUI(() -> {
                Toast.makeText(context, "Paciente guardado. Se sincronizará cuando haya conexión.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(context, PacientesActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                context.startActivity(intent);
            });
        });
    }
}
