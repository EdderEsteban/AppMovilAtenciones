package com.example.registrosatenciones.util;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.registrosatenciones.db.AppDatabase;
import com.example.registrosatenciones.db.entity.TipoPrestacionEnfermeriaEntity;
import com.example.registrosatenciones.request.ApiClient;
import com.example.registrosatenciones.response.TipoPrestacionResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CatalogoSync {

    private CatalogoSync() {}

    public static void descargarTiposPrestacionEnfermeria(Context context) {
        String token = PreferenciasUsuario.getAuthHeader(context);
        ApiClient.getApiAtenciones().obtenerTiposPrestacionEnfermeria(token).enqueue(new Callback<List<TipoPrestacionResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<TipoPrestacionResponse>> call, @NonNull Response<List<TipoPrestacionResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AppExecutors.io().execute(() -> guardar(context, response.body()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<TipoPrestacionResponse>> call, @NonNull Throwable t) {
                // sin conexión: se sigue usando el catálogo que ya estaba cacheado (si había)
            }
        });
    }

    private static void guardar(Context context, List<TipoPrestacionResponse> remotos) {
        List<TipoPrestacionEnfermeriaEntity> entidades = new ArrayList<>();
        for (TipoPrestacionResponse r : remotos) {
            TipoPrestacionEnfermeriaEntity entidad = new TipoPrestacionEnfermeriaEntity();
            entidad.setId(r.getId());
            entidad.setGrupo(r.getGrupo());
            entidad.setNombrePrestacion(r.getNombrePrestacion());
            entidades.add(entidad);
        }
        AppDatabase.getInstancia(context).tipoPrestacionEnfermeriaDao().guardarCatalogo(entidades);
    }
}
