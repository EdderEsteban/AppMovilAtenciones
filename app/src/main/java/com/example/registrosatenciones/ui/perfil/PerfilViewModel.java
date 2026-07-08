package com.example.registrosatenciones.ui.perfil;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.registrosatenciones.request.ActualizarPerfilRequest;
import com.example.registrosatenciones.request.ApiClient;
import com.example.registrosatenciones.response.InstitucionResponse;
import com.example.registrosatenciones.response.LoginResponse;
import com.example.registrosatenciones.ui.login.LoginActivity;
import com.example.registrosatenciones.util.PreferenciasUsuario;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PerfilViewModel extends AndroidViewModel {

    private final Context context;
    private final MutableLiveData<Boolean> cargando = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> actualizadoExitoso = new MutableLiveData<>();

    public PerfilViewModel(@NonNull Application application) {
        super(application);
        context = application.getApplicationContext();
    }

    public String getNombreCompleto() {
        return PreferenciasUsuario.getNombreCompleto(context);
    }

    public String getRol() {
        return PreferenciasUsuario.getRol(context);
    }

    public String getInstitucionActiva() {
        return PreferenciasUsuario.getInstitucionActivaNombre(context);
    }

    public String getEmailActual() {
        return PreferenciasUsuario.getEmail(context);
    }

    public LiveData<Boolean> getCargando() {
        return cargando;
    }

    public LiveData<Boolean> getActualizadoExitoso() {
        return actualizadoExitoso;
    }

    public void actualizarPerfil(String email, String contrasenaActual, String contrasenaNueva, String confirmarContrasenaNueva) {
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(contrasenaActual)) {
            Toast.makeText(context, "Completá el email y tu contraseña actual", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!TextUtils.isEmpty(contrasenaNueva) && !contrasenaNueva.equals(confirmarContrasenaNueva)) {
            Toast.makeText(context, "Las contraseñas nuevas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }

        cargando.setValue(true);
        String token = PreferenciasUsuario.getAuthHeader(context);
        ActualizarPerfilRequest request = new ActualizarPerfilRequest(
                email.trim(), contrasenaActual, TextUtils.isEmpty(contrasenaNueva) ? null : contrasenaNueva);

        ApiClient.getApiAtenciones().actualizarPerfil(token, request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                cargando.setValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse body = response.body();
                    PreferenciasUsuario.guardarSesion(context, body.getToken(), body.getUsuarioId(),
                            body.getNombreCompleto(), body.getEmail(), body.getRol());

                    if (body.getInstitucionActivaId() != null) {
                        String nombreInstitucion = buscarNombreInstitucion(body.getInstitucionActivaId(), body.getInstituciones());
                        PreferenciasUsuario.guardarInstitucionActiva(context, body.getInstitucionActivaId(), nombreInstitucion);
                    }

                    Toast.makeText(context, "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show();
                    actualizadoExitoso.setValue(true);
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

    public void cerrarSesion() {
        PreferenciasUsuario.cerrarSesion(context);
        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
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
