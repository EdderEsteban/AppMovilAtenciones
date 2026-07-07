package com.example.registrosatenciones.response;

import java.util.List;

public class AtencionEnfermeriaDetalleResponse {
    private int id;
    private String fecha;
    private int pacienteId;
    private String pacienteNombre;
    private String pacienteDni;
    private int edad;
    private String tipoAtencion;   // "Ambulatorio" | "Internado"
    private boolean embarazada;
    private boolean sinObraSocial;
    private String observaciones;  // nullable
    private String profesional;
    private String institucion;
    private List<PrestacionDetalleResponse> prestaciones;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public int getPacienteId() { return pacienteId; }
    public void setPacienteId(int pacienteId) { this.pacienteId = pacienteId; }

    public String getPacienteNombre() { return pacienteNombre; }
    public void setPacienteNombre(String pacienteNombre) { this.pacienteNombre = pacienteNombre; }

    public String getPacienteDni() { return pacienteDni; }
    public void setPacienteDni(String pacienteDni) { this.pacienteDni = pacienteDni; }

    public int getEdad() { return edad; }
    public void setEdad(int edad) { this.edad = edad; }

    public String getTipoAtencion() { return tipoAtencion; }
    public void setTipoAtencion(String tipoAtencion) { this.tipoAtencion = tipoAtencion; }

    public boolean isEmbarazada() { return embarazada; }
    public void setEmbarazada(boolean embarazada) { this.embarazada = embarazada; }

    public boolean isSinObraSocial() { return sinObraSocial; }
    public void setSinObraSocial(boolean sinObraSocial) { this.sinObraSocial = sinObraSocial; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public String getProfesional() { return profesional; }
    public void setProfesional(String profesional) { this.profesional = profesional; }

    public String getInstitucion() { return institucion; }
    public void setInstitucion(String institucion) { this.institucion = institucion; }

    public List<PrestacionDetalleResponse> getPrestaciones() { return prestaciones; }
    public void setPrestaciones(List<PrestacionDetalleResponse> prestaciones) { this.prestaciones = prestaciones; }
}