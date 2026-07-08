package com.example.registrosatenciones.ui.sincronizacion;

public class ItemPendiente {
    private final String titulo;
    private final String subtitulo;

    public ItemPendiente(String titulo, String subtitulo) {
        this.titulo = titulo;
        this.subtitulo = subtitulo;
    }

    public String getTitulo() { return titulo; }
    public String getSubtitulo() { return subtitulo; }
}
