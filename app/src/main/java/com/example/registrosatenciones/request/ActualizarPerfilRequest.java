package com.example.registrosatenciones.request;

public class ActualizarPerfilRequest {
    private String email;
    private String contrasenaActual;
    private String contrasenaNueva;

    public ActualizarPerfilRequest(String email, String contrasenaActual, String contrasenaNueva) {
        this.email = email;
        this.contrasenaActual = contrasenaActual;
        this.contrasenaNueva = contrasenaNueva;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getContrasenaActual() { return contrasenaActual; }
    public void setContrasenaActual(String contrasenaActual) { this.contrasenaActual = contrasenaActual; }

    public String getContrasenaNueva() { return contrasenaNueva; }
    public void setContrasenaNueva(String contrasenaNueva) { this.contrasenaNueva = contrasenaNueva; }
}
