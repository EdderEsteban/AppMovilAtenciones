package com.example.registrosatenciones.response;

import java.util.List;

public class AtencionResumenResponse {
    private int id;
    private String tipo;          // "E" | "O"
    private String fecha;
    private String resumen;
    private String diagnostico;   // nullable (solo odontología)
    private List<String> prestaciones;
    private String observaciones; // nullable

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getResumen() { return resumen; }
    public void setResumen(String resumen) { this.resumen = resumen; }

    public String getDiagnostico() { return diagnostico; }
    public void setDiagnostico(String diagnostico) { this.diagnostico = diagnostico; }

    public List<String> getPrestaciones() { return prestaciones; }
    public void setPrestaciones(List<String> prestaciones) { this.prestaciones = prestaciones; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}