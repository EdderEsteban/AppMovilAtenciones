package com.example.registrosatenciones;

import static org.junit.Assert.assertEquals;

import com.example.registrosatenciones.ui.odontologia.view.MedidorAnchoOdontograma;

import org.junit.Test;

/**
 * Cubre el ancho que el OdontogramaView reporta en onMeasure. El caso crítico es el del
 * odontograma resumen/detalle: van dentro de un HorizontalScrollView, que mide a sus hijos
 * con modo UNSPECIFIED y tamaño 0. Si la vista reportaba ese 0, medía 0px de ancho y no se
 * dibujaba nada — por eso "lo que pintabas no se reflejaba" en el resumen.
 */
public class OdontogramaViewMedicionTest {

    // Contenido de referencia: la fila más ancha del resumen (16 permanentes) a 30dp con gaps.
    private static final int ANCHO_CONTENIDO = 540;

    @Test public void dentroDeHorizontalScrollView_reportaAnchoDelContenidoNoCero() {
        // HorizontalScrollView → modo libre (UNSPECIFIED), tamaño 0.
        int ancho = MedidorAnchoOdontograma.anchoMedido(MedidorAnchoOdontograma.MODO_LIBRE, 0, ANCHO_CONTENIDO);
        assertEquals(ANCHO_CONTENIDO, ancho);
    }

    @Test public void modoExacto_respetaElAnchoDelPadre() {
        // Editor: match_parent → EXACTLY con el ancho real de pantalla.
        int ancho = MedidorAnchoOdontograma.anchoMedido(MedidorAnchoOdontograma.MODO_EXACTO, 1080, ANCHO_CONTENIDO);
        assertEquals(1080, ancho);
    }

    @Test public void modoMaximo_seLimitaAlPadreCuandoElContenidoEsMasAncho() {
        int ancho = MedidorAnchoOdontograma.anchoMedido(MedidorAnchoOdontograma.MODO_MAXIMO, 400, ANCHO_CONTENIDO);
        assertEquals(400, ancho);
    }

    @Test public void modoMaximo_usaElContenidoCuandoEntraEnElPadre() {
        int ancho = MedidorAnchoOdontograma.anchoMedido(MedidorAnchoOdontograma.MODO_MAXIMO, 800, ANCHO_CONTENIDO);
        assertEquals(ANCHO_CONTENIDO, ancho);
    }
}
