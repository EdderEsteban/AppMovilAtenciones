package com.example.registrosatenciones.ui.sincronizacion;

public class ItemPendiente {
    private final String titulo;
    private final String subtitulo;
    private final boolean esError;

    public ItemPendiente(String titulo, String subtitulo, boolean esError) {
        this.titulo = titulo;
        this.subtitulo = subtitulo;
        this.esError = esError;
    }

    public String getTitulo() { return titulo; }
    public String getSubtitulo() { return subtitulo; }
    public boolean isEsError() { return esError; }
}
