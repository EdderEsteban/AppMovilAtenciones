package com.example.registrosatenciones.ui.dashboard;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.registrosatenciones.request.ApiClient;
import com.example.registrosatenciones.response.DashboardResponse;
import com.example.registrosatenciones.ui.login.LoginActivity;
import com.example.registrosatenciones.util.CatalogoSync;
import com.example.registrosatenciones.util.PreferenciasUsuario;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardViewModel extends AndroidViewModel {

    private final Context context;
    private final MutableLiveData<DashboardResponse> dashboard = new MutableLiveData<>();
    private final MutableLiveData<Boolean> cargando = new MutableLiveData<>(false);

    public DashboardViewModel(@NonNull Application application) {
        super(application);
        context = application.getApplicationContext();
    }

    public LiveData<DashboardResponse> getDashboard() {
        return dashboard;
    }

    public LiveData<Boolean> getCargando() {
        return cargando;
    }

    public void cargar() {
        cargando.setValue(true);
        String token = PreferenciasUsuario.getAuthHeader(context);

        // Autoreparación: si la descarga del catálogo falló al loguear (mala señal, etc.),
        // se reintenta solo cada vez que se visita Inicio, sin que el usuario tenga que hacer nada.
        CatalogoSync.descargarCatalogosSegunRol(context);

        ApiClient.getApiAtenciones().obtenerDashboard(token).enqueue(new Callback<DashboardResponse>() {
            @Override
            public void onResponse(@NonNull Call<DashboardResponse> call, @NonNull Response<DashboardResponse> response) {
                cargando.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    dashboard.setValue(response.body());
                } else if (response.code() == 409) {
                    Toast.makeText(context, "Tu sesión perdió la institución activa. Volvé a iniciar sesión.", Toast.LENGTH_LONG).show();
                    irALogin();
                } else {
                    Toast.makeText(context, ApiClient.obtenerMensajeError(response), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<DashboardResponse> call, @NonNull Throwable t) {
                cargando.setValue(false);
                Toast.makeText(context, "Sin conexión — no se pudo cargar el resumen", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void irALogin() {
        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}
