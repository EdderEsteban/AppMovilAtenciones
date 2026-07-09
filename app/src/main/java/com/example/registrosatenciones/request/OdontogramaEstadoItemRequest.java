package com.example.registrosatenciones.request;

public class OdontogramaEstadoItemRequest {
    private int numeroDiente;
    private String superficie;
    private int estado;

    public OdontogramaEstadoItemRequest(int numeroDiente, String superficie, int estado) {
        this.numeroDiente = numeroDiente;
        this.superficie = superficie;
        this.estado = estado;
    }

    public int getNumeroDiente() { return numeroDiente; }
    public void setNumeroDiente(int numeroDiente) { this.numeroDiente = numeroDiente; }

    public String getSuperficie() { return superficie; }
    public void setSuperficie(String superficie) { this.superficie = superficie; }

    public int getEstado() { return estado; }
    public void setEstado(int estado) { this.estado = estado; }
}
