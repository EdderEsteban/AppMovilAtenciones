package com.example.registrosatenciones.ui.pacientes;

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.registrosatenciones.db.AppDatabase;
import com.example.registrosatenciones.db.SyncEstado;
import com.example.registrosatenciones.db.dao.PacienteDao;
import com.example.registrosatenciones.db.entity.PacienteEntity;
import com.example.registrosatenciones.request.ApiClient;
import com.example.registrosatenciones.response.PacienteResponse;
import com.example.registrosatenciones.util.AppExecutors;
import com.example.registrosatenciones.util.PreferenciasUsuario;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PacientesViewModel extends AndroidViewModel {

    private final Context context;
    private final PacienteDao pacienteDao;

    private final MutableLiveData<String> query = new MutableLiveData<>("");
    private final LiveData<List<PacienteEntity>> pacientes;
    private final MutableLiveData<Boolean> sinConexion = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> cargando = new MutableLiveData<>(false);

    public PacientesViewModel(@NonNull Application application) {
        super(application);
        context = application.getApplicationContext();
        pacienteDao = AppDatabase.getInstancia(context).pacienteDao();

        pacientes = Transformations.switchMap(query, texto -> {
            if (texto == null || texto.trim().length() < 2) {
                MutableLiveData<List<PacienteEntity>> vacio = new MutableLiveData<>();
                vacio.setValue(new ArrayList<>());
                return vacio;
            }
            return pacienteDao.buscar(texto.trim());
        });
    }

    public LiveData<List<PacienteEntity>> getPacientes() {
        return pacientes;
    }

    public LiveData<Boolean> getSinConexion() {
        return sinConexion;
    }

    public LiveData<Boolean> getCargando() {
        return cargando;
    }

    public void buscar(String texto) {
        query.setValue(texto);

        String q = texto == null ? "" : texto.trim();
        if (q.length() < 2) return;

        cargando.setValue(true);
        String token = PreferenciasUsuario.getAuthHeader(context);
        ApiClient.getApiAtenciones().buscarPacientes(token, q).enqueue(new Callback<List<PacienteResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<PacienteResponse>> call, @NonNull Response<List<PacienteResponse>> response) {
                cargando.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    sinConexion.setValue(false);
                    AppExecutors.io().execute(() -> guardarEnCache(response.body()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<PacienteResponse>> call, @NonNull Throwable t) {
                cargando.setValue(false);
                sinConexion.setValue(true);
            }
        });
    }

    private void guardarEnCache(List<PacienteResponse> remotos) {
        for (PacienteResponse r : remotos) {
            PacienteEntity local = pacienteDao.buscarPorDni(r.getDni());

            if (local != null && local.getSyncState() == SyncEstado.PENDIENTE) {
                continue; // no pisar un alta local que todavía no se sincronizó
            }

            PacienteEntity entity = local != null ? local : new PacienteEntity();
            entity.setServerId(r.getId());
            entity.setDni(r.getDni());
            entity.setApellido(r.getApellido());
            entity.setNombre(r.getNombre());
            entity.setSexo(r.getSexo());
            entity.setEdad(r.getEdad());
            entity.setObraSocial(r.getObraSocial());
            entity.setTelefono(r.getTelefono());
            entity.setSyncState(SyncEstado.SINCRONIZADO);

            if (local != null) {
                pacienteDao.actualizar(entity);
            } else {
                pacienteDao.insertar(entity);
            }
        }
    }
}