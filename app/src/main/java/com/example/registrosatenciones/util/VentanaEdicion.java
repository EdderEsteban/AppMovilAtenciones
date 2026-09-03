package com.example.registrosatenciones.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// La ventana de edición es un valor DERIVADO de la fecha de captura, no un dato
// guardado: se calcula cada vez que hace falta. Así la regla sobrevive a cerrar
// la app o quedarse sin batería, y no puede desincronizarse de lo que se ve.
public final class VentanaEdicion {

    public static final int MINUTOS_VENTANA = 15;

    private static final long MS_VENTANA = MINUTOS_VENTANA * 60L * 1000L;
    private static final String FORMATO = "yyyy-MM-dd'T'HH:mm:ss";

    private VentanaEdicion() {}

    private static SimpleDateFormat formato() {
        return new SimpleDateFormat(FORMATO, Locale.ROOT);
    }

    // Milisegundos que faltan para que la ventana cierre. 0 si ya venció o si la
    // fecha no se puede interpretar (ante la duda, cerrada: no se permite editar
    // algo cuya antigüedad no se puede determinar).
    public static long msRestantes(String fechaRegistroLocal) {
        if (fechaRegistroLocal == null) return 0;
        try {
            Date captura = formato().parse(fechaRegistroLocal);
            if (captura == null) return 0;
            long vence = captura.getTime() + MS_VENTANA;
            long restante = vence - System.currentTimeMillis();
            return restante > 0 ? restante : 0;
        } catch (ParseException e) {
            return 0;
        }
    }

    public static boolean estaAbierta(String fechaRegistroLocal) {
        return msRestantes(fechaRegistroLocal) > 0;
    }

    // Fecha de corte para la consulta de sincronización: todo lo capturado en
    // este instante o antes ya cumplió los 15 minutos y puede enviarse.
    public static String fechaCorte() {
        return formato().format(new Date(System.currentTimeMillis() - MS_VENTANA));
    }

    // "12:34" para la cuenta regresiva. Devuelve null si la ventana está cerrada.
    public static String formatearRestante(String fechaRegistroLocal) {
        long ms = msRestantes(fechaRegistroLocal);
        if (ms <= 0) return null;
        long totalSegundos = ms / 1000;
        return String.format(Locale.ROOT, "%d:%02d", totalSegundos / 60, totalSegundos % 60);
    }
}
