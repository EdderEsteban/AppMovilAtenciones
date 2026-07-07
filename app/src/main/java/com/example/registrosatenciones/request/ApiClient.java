package com.example.registrosatenciones.request;

import com.example.registrosatenciones.response.AtencionEnfermeriaDetalleResponse;
import com.example.registrosatenciones.response.CrearAtencionResponse;
import com.example.registrosatenciones.response.DashboardResponse;
import com.example.registrosatenciones.response.ErrorResponse;
import com.example.registrosatenciones.response.LoginResponse;
import com.example.registrosatenciones.response.PacienteDetalleResponse;
import com.example.registrosatenciones.response.PacienteResponse;
import com.example.registrosatenciones.response.TipoPrestacionResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public class ApiClient {


    public static final String URLBASE = "https://cnvgwf2q-5237.brs.devtunnels.ms/"; // <---Poner la URL!!!

    public static AtencionesService getApiAtenciones() {
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(URLBASE)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        return retrofit.create(AtencionesService.class);
    }

    // Parsea el {"error": "mensaje"} que devuelve el backend en respuestas no exitosas.
    public static String obtenerMensajeError(Response<?> response) {
        try {
            if (response.errorBody() == null) {
                return "Error desconocido (código " + response.code() + ")";
            }
            Gson gson = new Gson();
            ErrorResponse error = gson.fromJson(response.errorBody().string(), ErrorResponse.class);
            return error != null && error.getError() != null ? error.getError() : "Error desconocido";
        } catch (Exception e) {
            return "Error desconocido (código " + response.code() + ")";
        }
    }

    public interface AtencionesService {

        // Metodo Login
        @POST("auth/login")
        Call<LoginResponse> login(@Body LoginRequest request);

        // Metodo SeleccionInstitucion
        @POST("auth/institucion")
        Call<LoginResponse> seleccionarInstitucion(@Header("Authorization") String token,
                                                   @Body SeleccionInstitucionRequest request);

        // Metodo BuscarPacientes
        @GET("pacientes")
        Call<List<PacienteResponse>> buscarPacientes(@Header("Authorization") String token,
                                                     @Query("q") String q);

        // Metodo ObtenerPaciente (ficha completa)
        @GET("pacientes/{id}")
        Call<PacienteDetalleResponse> obtenerPaciente(@Header("Authorization") String token,
                                                      @Path("id") int id);

        // Metodo TiposPrestacionEnfermeria (catálogo)
        @GET("atenciones-enfermeria/tipos-prestacion")
        Call<List<TipoPrestacionResponse>> obtenerTiposPrestacionEnfermeria(@Header("Authorization") String token);

        // Metodo CrearAtencionEnfermeria
        @POST("atenciones-enfermeria")
        Call<CrearAtencionResponse> crearAtencionEnfermeria(@Header("Authorization") String token,
                                                            @Body CrearAtencionEnfermeriaRequest request);

        // Metodo ObtenerAtencionEnfermeria (detalle)
        @GET("atenciones-enfermeria/{id}")
        Call<AtencionEnfermeriaDetalleResponse> obtenerAtencionEnfermeria(@Header("Authorization") String token,
                                                                          @Path("id") int id);

        // Metodo Dashboard
        @GET("dashboard")
        Call<DashboardResponse> obtenerDashboard(@Header("Authorization") String token);
    }
}