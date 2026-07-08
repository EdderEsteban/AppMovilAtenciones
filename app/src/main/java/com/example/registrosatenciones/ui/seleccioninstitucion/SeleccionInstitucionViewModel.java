package com.example.registrosatenciones.ui.seleccioninstitucion;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.registrosatenciones.request.ApiClient;
import com.example.registrosatenciones.request.SeleccionInstitucionRequest;
import com.example.registrosatenciones.response.InstitucionResponse;
import com.example.registrosatenciones.response.LoginResponse;
import com.example.registrosatenciones.ui.inicio.InicioActivity;
import com.example.registrosatenciones.util.PreferenciasUsuario;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SeleccionInstitucionViewModel extends AndroidViewModel {

    private final Context context;
    private final MutableLiveData<List<InstitucionResponse>> instituciones = new MutableLiveData<>();
    private final MutableLiveData<Integer> institucionSeleccionadaId = new MutableLiveData<>();
    private final MutableLiveData<Boolean> cargando = new MutableLiveData<>(false);

    public SeleccionInstitucionViewModel(@NonNull Application application) {
        super(application);
        context = application.getApplicationContext();
    }

    public void setInstituciones(List<InstitucionResponse> lista) {
        instituciones.setValue(lista);
    }

    public LiveData<List<InstitucionResponse>> getInstituciones() {
        return instituciones;
    }

    public LiveData<Integer> getInstitucionSeleccionadaId() {
        return institucionSeleccionadaId;
    }

    public LiveData<Boolean> getCargando() {
        return cargando;
    }

    public void onInstitucionSeleccionada(int institucionId) {
        institucionSeleccionadaId.setValue(institucionId);
    }

    public void onContinuarClick() {
        Integer institucionId = institucionSeleccionadaId.getValue();
        if (institucionId == null) {
            Toast.makeText(context, "Elegí una institución para continuar", Toast.LENGTH_SHORT).show();
            return;
        }

        cargando.setValue(true);
        String token = PreferenciasUsuario.getAuthHeader(context);
        ApiClient.AtencionesService api = ApiClient.getApiAtenciones();
        Call<LoginResponse> call = api.seleccionarInstitucion(token, new SeleccionInstitucionRequest(institucionId));

        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                cargando.setValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    onSeleccionExitosa(response.body());
                } else {
                    Toast.makeText(context, ApiClient.obtenerMensajeError(response), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                cargando.setValue(false);
                Toast.makeText(context, "Error de conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void onSeleccionExitosa(LoginResponse body) {
        PreferenciasUsuario.guardarSesion(context, body.getToken(), body.getUsuarioId(),
                body.getNombreCompleto(), body.getEmail(), body.getRol());

        String nombreInstitucion = buscarNombreInstitucion(body.getInstitucionActivaId(), body.getInstituciones());
        PreferenciasUsuario.guardarInstitucionActiva(context, body.getInstitucionActivaId(), nombreInstitucion);
        com.example.registrosatenciones.util.CatalogoSync.descargarTiposPrestacionEnfermeria(context);

        Intent intent = new Intent(context, InicioActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private String buscarNombreInstitucion(Integer institucionId, List<InstitucionResponse> instituciones) {
        if (institucionId == null || instituciones == null) return null;
        for (InstitucionResponse institucion : instituciones) {
            if (institucion.getId() == institucionId) return institucion.getNombre();
        }
        return null;
    }
}