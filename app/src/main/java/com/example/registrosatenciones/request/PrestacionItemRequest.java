package com.example.registrosatenciones.request;

public class PrestacionItemRequest {
    private int tipoPrestacionId;
    private int cantidad;

    public PrestacionItemRequest(int tipoPrestacionId, int cantidad) {
        this.tipoPrestacionId = tipoPrestacionId;
        this.cantidad = cantidad;
    }

    public int getTipoPrestacionId() { return tipoPrestacionId; }
    public void setTipoPrestacionId(int tipoPrestacionId) { this.tipoPrestacionId = tipoPrestacionId; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
}