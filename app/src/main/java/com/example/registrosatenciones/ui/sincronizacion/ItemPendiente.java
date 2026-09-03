package com.example.registrosatenciones.ui.sincronizacion;

public class ItemPendiente {
    private final String titulo;
    private final String subtitulo;
    private final boolean esError;
    private final String tiempoRestante;

    public ItemPendiente(String titulo, String subtitulo, boolean esError) {
        this(titulo, subtitulo, esError, null);
    }

    // tiempoRestante es null cuando el ítem no tiene ventana de edición (pacientes)
    // o cuando la ventana ya cerró.
    public ItemPendiente(String titulo, String subtitulo, boolean esError, String tiempoRestante) {
        this.titulo = titulo;
        this.subtitulo = subtitulo;
        this.esError = esError;
        this.tiempoRestante = tiempoRestante;
    }

    public String getTitulo() { return titulo; }
    public String getSubtitulo() { return subtitulo; }
    public boolean isEsError() { return esError; }
    public String getTiempoRestante() { return tiempoRestante; }
}
