package com.example.registrosatenciones.response;

import java.util.List;

public class LoginResponse {
    private String token;
    private String expiraUtc;
    private int usuarioId;
    private String nombreCompleto;
    private String email;
    private String rol;
    private boolean requiereSeleccion;
    private Integer institucionActivaId;      // nullable
    private List<InstitucionResponse> instituciones;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getExpiraUtc() { return expiraUtc; }
    public void setExpiraUtc(String expiraUtc) { this.expiraUtc = expiraUtc; }

    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public boolean isRequiereSeleccion() { return requiereSeleccion; }
    public void setRequiereSeleccion(boolean requiereSeleccion) { this.requiereSeleccion = requiereSeleccion; }

    public Integer getInstitucionActivaId() { return institucionActivaId; }
    public void setInstitucionActivaId(Integer institucionActivaId) { this.institucionActivaId = institucionActivaId; }

    public List<InstitucionResponse> getInstituciones() { return instituciones; }
    public void setInstituciones(List<InstitucionResponse> instituciones) { this.instituciones = instituciones; }
}