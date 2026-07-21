package com.example.registrosatenciones.ui.historiaclinica;

/** Decide si la ficha muestra la HC completa del server (ONLINE) o solo lo local (LOCAL). */
public final class DecisorModoHistoria {

    private DecisorModoHistoria() {}

    public static ModoHistoria decidir(boolean hayInternet, Integer serverId) {
        return (hayInternet && serverId != null) ? ModoHistoria.ONLINE : ModoHistoria.LOCAL;
    }
}
