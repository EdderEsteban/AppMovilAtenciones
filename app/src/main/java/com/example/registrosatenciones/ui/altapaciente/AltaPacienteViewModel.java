package com.example.registrosatenciones.ui.altapaciente;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.registrosatenciones.db.AppDatabase;
import com.example.registrosatenciones.db.SyncEstado;
import com.example.registrosatenciones.db.dao.ObraSocialDao;
import com.example.registrosatenciones.db.dao.PacienteDao;
import com.example.registrosatenciones.db.entity.ObraSocialEntity;
import com.example.registrosatenciones.db.entity.PacienteEntity;
import com.example.registrosatenciones.ui.pacientes.PacientesActivity;
import com.example.registrosatenciones.util.AppExecutors;

import java.util.List;

public class AltaPacienteViewModel extends AndroidViewModel {

    private final Context context;
    private final PacienteDao pacienteDao;
    private final ObraSocialDao obraSocialDao;

    private final MutableLiveData<List<ObraSocialEntity>> obrasSociales = new MutableLiveData<>();

    public AltaPacienteViewModel(@NonNull Application application) {
        super(application);
        context = application.getApplicationContext();
        AppDatabase db = AppDatabase.getInstancia(context);
        pacienteDao = db.pacienteDao();
        obraSocialDao = db.obraSocialDao();

        AppExecutors.io().execute(() -> {
            List<ObraSocialEntity> lista = obraSocialDao.listar();
            AppExecutors.ejecutarEnUI(() -> obrasSociales.setValue(lista));
        });
    }

    public LiveData<List<ObraSocialEntity>> getObrasSociales() {
        return obrasSociales;
    }

    public void guardarPaciente(String dni, String apellido, String nombre, String fechaNacimiento,
                                String sexo, String domicilio, String telefono,
                                Integer obraSocialId, String obraSocialNombre) {
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
            nuevo.setObraSocialId(obraSocialId);
            nuevo.setObraSocialNombre(TextUtils.isEmpty(obraSocialNombre) ? null : obraSocialNombre.trim());
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
