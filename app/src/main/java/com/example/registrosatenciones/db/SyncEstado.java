package com.example.registrosatenciones.db;

public final class SyncEstado {
    public static final int PENDIENTE     = 0;
    public static final int SINCRONIZADO  = 1;
    public static final int ERROR         = 2;

    private SyncEstado() {}
}