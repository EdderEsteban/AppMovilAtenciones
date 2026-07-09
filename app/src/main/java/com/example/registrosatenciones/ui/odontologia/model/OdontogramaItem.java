package com.example.registrosatenciones.ui.odontologia.model;

import java.io.Serializable;

public class OdontogramaItem implements Serializable {
    private int numeroDiente;
    private String superficie;
    private int estado;

    public OdontogramaItem(int numeroDiente, String superficie, int estado) {
        this.numeroDiente = numeroDiente;
        this.superficie = superficie;
        this.estado = estado;
    }

    public int getNumeroDiente() { return numeroDiente; }
    public String getSuperficie() { return superficie; }
    public int getEstado() { return estado; }
}
