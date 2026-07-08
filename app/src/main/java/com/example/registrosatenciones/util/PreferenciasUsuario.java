package com.example.registrosatenciones.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.registrosatenciones.response.InstitucionResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class PreferenciasUsuario {

    private static final String PREFS_NAME = "atenciones_prefs";

    private static final String KEY_TOKEN = "token";
    private static final String KEY_USUARIO_ID = "usuarioId";
    private static final String KEY_NOMBRE_COMPLETO = "nombreCompleto";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_ROL = "rol";
    private static final String KEY_INSTITUCION_ID = "institucionActivaId";
    private static final String KEY_INSTITUCION_NOMBRE = "institucionActivaNombre";
    private static final String KEY_INSTITUCIONES_JSON = "institucionesJson";

    private PreferenciasUsuario() {}

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void guardarSesion(Context context, String token, int usuarioId,
                                     String nombreCompleto, String email, String rol) {
        prefs(context).edit()
                .putString(KEY_TOKEN, token)
                .putInt(KEY_USUARIO_ID, usuarioId)
                .putString(KEY_NOMBRE_COMPLETO, nombreCompleto)
                .putString(KEY_EMAIL, email)
                .putString(KEY_ROL, rol)
                .apply();
    }

    public static void guardarInstitucionActiva(Context context, int institucionId, String institucionNombre) {
        prefs(context).edit()
                .putInt(KEY_INSTITUCION_ID, institucionId)
                .putString(KEY_INSTITUCION_NOMBRE, institucionNombre)
                .apply();
    }

    public static void actualizarToken(Context context, String token) {
        prefs(context).edit().putString(KEY_TOKEN, token).apply();
    }

    public static String getToken(Context context) {
        return prefs(context).getString(KEY_TOKEN, null);
    }

    public static String getAuthHeader(Context context) {
        String token = getToken(context);
        return token != null ? "Bearer " + token : null;
    }

    public static int getUsuarioId(Context context) {
        return prefs(context).getInt(KEY_USUARIO_ID, -1);
    }

    public static String getNombreCompleto(Context context) {
        return prefs(context).getString(KEY_NOMBRE_COMPLETO, null);
    }

    public static String getEmail(Context context) {
        return prefs(context).getString(KEY_EMAIL, null);
    }

    public static String getRol(Context context) {
        return prefs(context).getString(KEY_ROL, null);
    }

    public static int getInstitucionActivaId(Context context) {
        return prefs(context).getInt(KEY_INSTITUCION_ID, -1);
    }

    public static String getInstitucionActivaNombre(Context context) {
        return prefs(context).getString(KEY_INSTITUCION_NOMBRE, null);
    }

    // Se cachea la lista completa (login, selección de institución, actualizar perfil) para
    // poder ofrecer "cambiar institución" más tarde sin tener que volver a pedir credenciales.
    public static void guardarInstituciones(Context context, List<InstitucionResponse> instituciones) {
        String json = new Gson().toJson(instituciones);
        prefs(context).edit().putString(KEY_INSTITUCIONES_JSON, json).apply();
    }

    public static List<InstitucionResponse> getInstituciones(Context context) {
        String json = prefs(context).getString(KEY_INSTITUCIONES_JSON, null);
        if (json == null) return new ArrayList<>();
        Type tipo = new TypeToken<List<InstitucionResponse>>() {}.getType();
        List<InstitucionResponse> lista = new Gson().fromJson(json, tipo);
        return lista != null ? lista : new ArrayList<>();
    }

    public static boolean haySesionActiva(Context context) {
        return getToken(context) != null;
    }

    public static void cerrarSesion(Context context) {
        prefs(context).edit().clear().apply();
    }
}