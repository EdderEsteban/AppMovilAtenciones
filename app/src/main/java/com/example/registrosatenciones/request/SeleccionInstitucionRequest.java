package com.example.registrosatenciones.request;

public class SeleccionInstitucionRequest {
    private int institucionId;

    public SeleccionInstitucionRequest(int institucionId) {
        this.institucionId = institucionId;
    }

    public int getInstitucionId() { return institucionId; }
    public void setInstitucionId(int institucionId) { this.institucionId = institucionId; }
}