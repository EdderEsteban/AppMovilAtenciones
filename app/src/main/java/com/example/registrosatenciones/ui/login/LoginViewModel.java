package com.example.registrosatenciones.ui.login;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.registrosatenciones.request.ApiClient;
import com.example.registrosatenciones.request.LoginRequest;
import com.example.registrosatenciones.response.InstitucionResponse;
import com.example.registrosatenciones.response.LoginResponse;
import com.example.registrosatenciones.ui.inicio.InicioActivity;
import com.example.registrosatenciones.ui.seleccioninstitucion.SeleccionInstitucionActivity;
import com.example.registrosatenciones.util.PreferenciasUsuario;

import java.io.Serializable;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginViewModel extends AndroidViewModel {

    private final Context context;
    private final MutableLiveData<Boolean> cargando = new MutableLiveData<>(false);

    public LoginViewModel(@NonNull Application application) {
        super(application);
        context = application.getApplicationContext();
    }

    public LiveData<Boolean> getCargando() {
        return cargando;
    }

    public void iniciarSesion(String email, String password) {
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(context, "Completá el correo y la contraseña", Toast.LENGTH_SHORT).show();
            return;
        }

        cargando.setValue(true);
        ApiClient.AtencionesService api = ApiClient.getApiAtenciones();
        Call<LoginResponse> call = api.login(new LoginRequest(email, password));

        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                cargando.setValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    onLoginExitoso(response.body());
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

    private void onLoginExitoso(LoginResponse body) {
        PreferenciasUsuario.guardarSesion(context, body.getToken(), body.getUsuarioId(),
                body.getNombreCompleto(), body.getEmail(), body.getRol());
        PreferenciasUsuario.guardarInstituciones(context, body.getInstituciones());

        if (body.isRequiereSeleccion()) {
            Intent intent = new Intent(context, SeleccionInstitucionActivity.class);
            intent.putExtra("instituciones", (Serializable) body.getInstituciones());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } else {
            String nombreInstitucion = buscarNombreInstitucion(body.getInstitucionActivaId(), body.getInstituciones());
            PreferenciasUsuario.guardarInstitucionActiva(context, body.getInstitucionActivaId(), nombreInstitucion);
            com.example.registrosatenciones.util.CatalogoSync.descargarTiposPrestacionEnfermeria(context);

            Intent intent = new Intent(context, InicioActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    private String buscarNombreInstitucion(Integer institucionId, List<InstitucionResponse> instituciones) {
        if (institucionId == null || instituciones == null) return null;
        for (InstitucionResponse institucion : instituciones) {
            if (institucion.getId() == institucionId) return institucion.getNombre();
        }
        return null;
    }
}