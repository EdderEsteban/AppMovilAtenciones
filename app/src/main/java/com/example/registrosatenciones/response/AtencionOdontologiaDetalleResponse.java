package com.example.registrosatenciones.response;

import java.util.List;

public class AtencionOdontologiaDetalleResponse {
    private int id;
    private String fecha;
    private int pacienteId;
    private String pacienteNombre;
    private String pacienteDni;
    private int edad;
    private String tipoConsulta;
    private String tipoTurno;
    private String diagnostico;
    private boolean embarazada;
    private boolean sinObraSocial;
    private String observaciones;
    private String profesional;
    private String institucion;
    private List<PrestacionDetalleResponse> prestaciones;
    private ValoracionDentalResponse valoracion;
    private List<OdontogramaEstadoResponse> odontograma;

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

    public String getTipoConsulta() { return tipoConsulta; }
    public void setTipoConsulta(String tipoConsulta) { this.tipoConsulta = tipoConsulta; }

    public String getTipoTurno() { return tipoTurno; }
    public void setTipoTurno(String tipoTurno) { this.tipoTurno = tipoTurno; }

    public String getDiagnostico() { return diagnostico; }
    public void setDiagnostico(String diagnostico) { this.diagnostico = diagnostico; }

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

    public ValoracionDentalResponse getValoracion() { return valoracion; }
    public void setValoracion(ValoracionDentalResponse valoracion) { this.valoracion = valoracion; }

    public List<OdontogramaEstadoResponse> getOdontograma() { return odontograma; }
    public void setOdontograma(List<OdontogramaEstadoResponse> odontograma) { this.odontograma = odontograma; }
}
