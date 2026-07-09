package com.example.registrosatenciones.request;

import java.util.List;

public class CrearAtencionOdontologiaRequest {
    private int pacienteId;
    private int tipoConsulta;
    private int tipoTurno;
    private int diagnosticoId;
    private boolean embarazada;
    private boolean sinObraSocial;
    private String nuevaObraSocial;
    private String observaciones;
    private List<PrestacionItemRequest> prestaciones;
    private List<OdontogramaEstadoItemRequest> odontograma;

    public int getPacienteId() { return pacienteId; }
    public void setPacienteId(int pacienteId) { this.pacienteId = pacienteId; }

    public int getTipoConsulta() { return tipoConsulta; }
    public void setTipoConsulta(int tipoConsulta) { this.tipoConsulta = tipoConsulta; }

    public int getTipoTurno() { return tipoTurno; }
    public void setTipoTurno(int tipoTurno) { this.tipoTurno = tipoTurno; }

    public int getDiagnosticoId() { return diagnosticoId; }
    public void setDiagnosticoId(int diagnosticoId) { this.diagnosticoId = diagnosticoId; }

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

    public List<OdontogramaEstadoItemRequest> getOdontograma() { return odontograma; }
    public void setOdontograma(List<OdontogramaEstadoItemRequest> odontograma) { this.odontograma = odontograma; }
}
