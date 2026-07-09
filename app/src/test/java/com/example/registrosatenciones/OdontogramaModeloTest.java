package com.example.registrosatenciones;

import static org.junit.Assert.*;

import com.example.registrosatenciones.ui.odontologia.model.OdontogramaItem;
import com.example.registrosatenciones.ui.odontologia.model.OdontogramaModelo;
import com.example.registrosatenciones.ui.odontologia.model.ValoracionLocal;

import java.util.List;

import org.junit.Test;

public class OdontogramaModeloTest {

    @Test public void cariesEnSuperficieCuentaComoCariesPerm() {
        OdontogramaModelo m = new OdontogramaModelo();
        m.aplicar(11, "V", OdontogramaModelo.CARIES);
        ValoracionLocal v = m.calcularCpo();
        assertEquals(1, v.cariesPerm);
        assertEquals(0, v.obturadosPerm);
    }

    @Test public void ausenteMarcaDienteEnteroYCuentaPerdido() {
        OdontogramaModelo m = new OdontogramaModelo();
        m.aplicar(36, "O", OdontogramaModelo.AUSENTE);   // superficie tocada da igual
        List<OdontogramaItem> lista = m.aplanar();
        assertEquals(1, lista.size());
        assertEquals("*", lista.get(0).getSuperficie());
        assertEquals(3, lista.get(0).getEstado());
        assertEquals(1, m.calcularCpo().perdidosPerm);
    }

    @Test public void extraccionPermanenteNoCuentaEnCpo() {
        OdontogramaModelo m = new OdontogramaModelo();
        m.aplicar(21, "*", OdontogramaModelo.EXTRACCION);
        ValoracionLocal v = m.calcularCpo();
        assertEquals(0, v.cariesPerm);
        assertEquals(0, v.perdidosPerm);
        assertEquals(0, v.obturadosPerm);
    }

    @Test public void obturadoYCoronaCuentanComoObturados() {
        OdontogramaModelo m = new OdontogramaModelo();
        m.aplicar(12, "O", OdontogramaModelo.OBTURADO);
        m.aplicar(13, "V", OdontogramaModelo.CORONA);
        assertEquals(2, m.calcularCpo().obturadosPerm);
    }

    @Test public void cariesPredominaSobreObturadoEnMismoDiente() {
        OdontogramaModelo m = new OdontogramaModelo();
        m.aplicar(14, "O", OdontogramaModelo.OBTURADO);
        m.aplicar(14, "V", OdontogramaModelo.CARIES);
        ValoracionLocal v = m.calcularCpo();
        assertEquals(1, v.cariesPerm);
        assertEquals(0, v.obturadosPerm);
    }

    @Test public void temporarioExtraccionCuentaEnCeo() {
        OdontogramaModelo m = new OdontogramaModelo();
        m.aplicar(51, "*", OdontogramaModelo.EXTRACCION);
        assertEquals(1, m.calcularCpo().extraccionTemp);
    }

    @Test public void temporarioAusenteNoCuenta() {
        OdontogramaModelo m = new OdontogramaModelo();
        m.aplicar(61, "*", OdontogramaModelo.AUSENTE);
        ValoracionLocal v = m.calcularCpo();
        assertEquals(0, v.extraccionTemp);
        assertEquals(0, v.cariesTemp);
    }

    @Test public void sanoBorraLaSuperficie() {
        OdontogramaModelo m = new OdontogramaModelo();
        m.aplicar(11, "V", OdontogramaModelo.CARIES);
        m.aplicar(11, "V", OdontogramaModelo.SANO);
        assertTrue(m.aplanar().isEmpty());
    }

    @Test public void cariesSobreDienteAusenteLoDesbloquea() {
        OdontogramaModelo m = new OdontogramaModelo();
        m.aplicar(11, "*", OdontogramaModelo.AUSENTE);
        m.aplicar(11, "*", OdontogramaModelo.CARIES);   // tocar diente bloqueado → superficie O
        List<OdontogramaItem> lista = m.aplanar();
        assertEquals(1, lista.size());
        assertEquals("O", lista.get(0).getSuperficie());
        assertEquals(1, lista.get(0).getEstado());
    }

    @Test public void cargarYAplanarRoundTrip() {
        OdontogramaModelo m = new OdontogramaModelo();
        m.cargar(java.util.Arrays.asList(
            new OdontogramaItem(11, "V", 1),
            new OdontogramaItem(36, "*", 3)));
        assertEquals(2, m.aplanar().size());
        assertEquals(1, m.calcularCpo().cariesPerm);
        assertEquals(1, m.calcularCpo().perdidosPerm);
    }
}
