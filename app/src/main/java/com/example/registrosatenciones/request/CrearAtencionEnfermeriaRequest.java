package com.example.registrosatenciones.request;

import java.util.List;

public class CrearAtencionEnfermeriaRequest {
    private int pacienteId;
    private int tipoAtencion;          // 1 = Ambulatorio, 2 = Internado
    private boolean embarazada;
    private boolean sinObraSocial;
    private String nuevaObraSocial;    // nullable
    private String observaciones;      // nullable
    private List<PrestacionItemRequest> prestaciones;

    public int getPacienteId() { return pacienteId; }
    public void setPacienteId(int pacienteId) { this.pacienteId = pacienteId; }

    public int getTipoAtencion() { return tipoAtencion; }
    public void setTipoAtencion(int tipoAtencion) { this.tipoAtencion = tipoAtencion; }

    public boolean isEmbarazada() { return embarazada; }
    public void setEmbarazada(boolean embarazada) { this.embarazada = embarazada; }

    public boolean isSinObraSocial() { return sinObraSocial; }
    public void setSinObraSocial(boolean sinObraSocial) { this.sinObraSocial = sinObraSocial; }

    public String getNuevaObraSocial() { return nuevaObraSocial; }
    public void setNuevaObraSocial(String nuevaObraSocial) { this.nuevaObraSocial = nuevaObraSocial; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public List<PrestacionItemRequest> getPrestaciones() { return prestaciones; }
    public void setPrestaciones(List<PrestacionItemRequest> prestaciones) { this.prestaciones = prestaciones; }
}