package com.example.registrosatenciones.ui.odontologia.view;

/**
 * Lógica pura (sin dependencias de Android) para decidir el ancho que el
 * {@link OdontogramaView} debe reportar en onMeasure. Aislada para poder testearla
 * en JVM sin el runtime de Android.
 */
public final class MedidorAnchoOdontograma {

    // Modo de medición como int simple, equivalente a los de View.MeasureSpec.
    public static final int MODO_EXACTO = 0;   // EXACTLY
    public static final int MODO_MAXIMO = 1;   // AT_MOST
    public static final int MODO_LIBRE = 2;    // UNSPECIFIED

    private MedidorAnchoOdontograma() {}

    /**
     * Dentro de un HorizontalScrollView el modo es "libre" (UNSPECIFIED) con tamaño 0;
     * si devolviéramos ese 0 la vista mediría cero de ancho y no dibujaría nada — era el
     * bug del odontograma resumen/detalle: lo pintado no se reflejaba. En ese caso hay
     * que reportar el ancho real del contenido.
     */
    public static int anchoMedido(int modo, int tamSpec, int anchoContenido) {
        switch (modo) {
            case MODO_EXACTO: return tamSpec;
            case MODO_MAXIMO: return Math.min(anchoContenido, tamSpec);
            default:          return anchoContenido; // MODO_LIBRE
        }
    }
}
