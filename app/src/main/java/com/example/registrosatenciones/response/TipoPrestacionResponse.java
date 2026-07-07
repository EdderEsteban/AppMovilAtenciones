package com.example.registrosatenciones.response;

public class TipoPrestacionResponse {
    private int id;
    private String grupo;
    private String nombrePrestacion;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getGrupo() { return grupo; }
    public void setGrupo(String grupo) { this.grupo = grupo; }

    public String getNombrePrestacion() { return nombrePrestacion; }
    public void setNombrePrestacion(String nombrePrestacion) { this.nombrePrestacion = nombrePrestacion; }
}