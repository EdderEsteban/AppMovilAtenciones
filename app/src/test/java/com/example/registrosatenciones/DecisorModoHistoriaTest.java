package com.example.registrosatenciones;

import static org.junit.Assert.assertEquals;

import com.example.registrosatenciones.ui.historiaclinica.DecisorModoHistoria;
import com.example.registrosatenciones.ui.historiaclinica.ModoHistoria;

import org.junit.Test;

public class DecisorModoHistoriaTest {

    @Test public void conInternetYServerIdEsOnline() {
        assertEquals(ModoHistoria.ONLINE, DecisorModoHistoria.decidir(true, 42));
    }

    @Test public void sinInternetEsLocal() {
        assertEquals(ModoHistoria.LOCAL, DecisorModoHistoria.decidir(false, 42));
    }

    @Test public void sinServerIdEsLocalAunqueHayaInternet() {
        assertEquals(ModoHistoria.LOCAL, DecisorModoHistoria.decidir(true, null));
    }
}
