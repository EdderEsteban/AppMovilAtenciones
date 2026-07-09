package com.example.registrosatenciones.response;

public class OdontogramaEstadoResponse {
    private int numeroDiente;
    private String superficie;
    private int estado;

    public int getNumeroDiente() { return numeroDiente; }
    public void setNumeroDiente(int numeroDiente) { this.numeroDiente = numeroDiente; }

    public String getSuperficie() { return superficie; }
    public void setSuperficie(String superficie) { this.superficie = superficie; }

    public int getEstado() { return estado; }
    public void setEstado(int estado) { this.estado = estado; }
}
