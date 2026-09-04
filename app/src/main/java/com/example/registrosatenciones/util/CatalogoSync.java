package com.example.registrosatenciones.util;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.registrosatenciones.db.AppDatabase;
import com.example.registrosatenciones.db.entity.DiagnosticoEntity;
import com.example.registrosatenciones.db.entity.ObraSocialEntity;
import com.example.registrosatenciones.db.entity.TipoPrestacionEnfermeriaEntity;
import com.example.registrosatenciones.db.entity.TipoPrestacionOdontologiaEntity;
import com.example.registrosatenciones.request.ApiClient;
import com.example.registrosatenciones.response.DiagnosticoResponse;
import com.example.registrosatenciones.response.ObraSocialResponse;
import com.example.registrosatenciones.response.TipoPrestacionOdontologiaResponse;
import com.example.registrosatenciones.response.TipoPrestacionResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CatalogoSync {

    private CatalogoSync() {}

    // Descarga el catálogo correspondiente al rol de la sesión activa.
    public static void descargarCatalogosSegunRol(Context context) {
        if ("Odontólogo".equals(PreferenciasUsuario.getRol(context))) {
            descargarTiposPrestacionOdontologia(context);
            descargarDiagnosticos(context);
        } else {
            descargarTiposPrestacionEnfermeria(context);
        }
        // Obra social se usa en las dos atenciones (enfermería y odontología),
        // así que se descarga para los dos roles.
        descargarObrasSociales(context);
    }

    public static void descargarTiposPrestacionEnfermeria(Context context) {
        String token = PreferenciasUsuario.getAuthHeader(context);
        ApiClient.getApiAtenciones().obtenerTiposPrestacionEnfermeria(token).enqueue(new Callback<List<TipoPrestacionResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<TipoPrestacionResponse>> call, @NonNull Response<List<TipoPrestacionResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AppExecutors.io().execute(() -> guardarTiposEnfermeria(context, response.body()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<TipoPrestacionResponse>> call, @NonNull Throwable t) {
                // sin conexión: se sigue usando el catálogo que ya estaba cacheado (si había)
            }
        });
    }

    public static void descargarTiposPrestacionOdontologia(Context context) {
        String token = PreferenciasUsuario.getAuthHeader(context);
        ApiClient.getApiAtenciones().obtenerTiposPrestacionOdontologia(token).enqueue(new Callback<List<TipoPrestacionOdontologiaResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<TipoPrestacionOdontologiaResponse>> call, @NonNull Response<List<TipoPrestacionOdontologiaResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AppExecutors.io().execute(() -> guardarTiposOdontologia(context, response.body()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<TipoPrestacionOdontologiaResponse>> call, @NonNull Throwable t) {
                // sin conexión: se sigue usando el catálogo que ya estaba cacheado (si había)
            }
        });
    }

    public static void descargarDiagnosticos(Context context) {
        String token = PreferenciasUsuario.getAuthHeader(context);
        ApiClient.getApiAtenciones().obtenerDiagnosticos(token).enqueue(new Callback<List<DiagnosticoResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<DiagnosticoResponse>> call, @NonNull Response<List<DiagnosticoResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AppExecutors.io().execute(() -> guardarDiagnosticos(context, response.body()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<DiagnosticoResponse>> call, @NonNull Throwable t) {
                // sin conexión: se sigue usando el catálogo que ya estaba cacheado (si había)
            }
        });
    }

    public static void descargarObrasSociales(Context context) {
        String token = PreferenciasUsuario.getAuthHeader(context);
        ApiClient.getApiAtenciones().obtenerObrasSociales(token).enqueue(new Callback<List<ObraSocialResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<ObraSocialResponse>> call, @NonNull Response<List<ObraSocialResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AppExecutors.io().execute(() -> guardarObrasSociales(context, response.body()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ObraSocialResponse>> call, @NonNull Throwable t) {
                // sin conexión: se sigue usando el catálogo que ya estaba cacheado (si había)
            }
        });
    }

    private static void guardarTiposEnfermeria(Context context, List<TipoPrestacionResponse> remotos) {
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

    private static void guardarTiposOdontologia(Context context, List<TipoPrestacionOdontologiaResponse> remotos) {
        List<TipoPrestacionOdontologiaEntity> entidades = new ArrayList<>();
        for (TipoPrestacionOdontologiaResponse r : remotos) {
            TipoPrestacionOdontologiaEntity entidad = new TipoPrestacionOdontologiaEntity();
            entidad.setId(r.getId());
            entidad.setNombre(r.getNombrePrestacion());
            entidades.add(entidad);
        }
        AppDatabase.getInstancia(context).tipoPrestacionOdontologiaDao().guardarCatalogo(entidades);
    }

    private static void guardarDiagnosticos(Context context, List<DiagnosticoResponse> remotos) {
        List<DiagnosticoEntity> entidades = new ArrayList<>();
        for (DiagnosticoResponse r : remotos) {
            DiagnosticoEntity entidad = new DiagnosticoEntity();
            entidad.setId(r.getId());
            entidad.setCodigo(r.getCodigo());
            entidad.setDescripcion(r.getDescripcion());
            entidades.add(entidad);
        }
        AppDatabase.getInstancia(context).diagnosticoDao().guardarCatalogo(entidades);
    }

    private static void guardarObrasSociales(Context context, List<ObraSocialResponse> remotos) {
        List<ObraSocialEntity> entidades = new ArrayList<>();
        for (ObraSocialResponse r : remotos) {
            ObraSocialEntity entidad = new ObraSocialEntity();
            entidad.setId(r.getId());
            entidad.setNombre(r.getNombre());
            entidades.add(entidad);
        }
        AppDatabase.getInstancia(context).obraSocialDao().guardarCatalogo(entidades);
    }
}
