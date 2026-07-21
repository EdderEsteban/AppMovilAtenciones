package com.example.registrosatenciones;

import static org.junit.Assert.assertEquals;

import com.example.registrosatenciones.ui.historiaclinica.ItemHistoria;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class ItemHistoriaTest {

    @Test public void ordenaPorFechaDescendente() {
        ItemHistoria vieja = new ItemHistoria("E", "2025-01-10T09:00:00", "Ambulatorio",
                new ArrayList<>(), ItemHistoria.Fuente.ONLINE, 1, 0);
        ItemHistoria nueva = new ItemHistoria("O", "2026-07-20T11:00:00", "1ª vez",
                new ArrayList<>(), ItemHistoria.Fuente.ONLINE, 2, 0);

        List<ItemHistoria> ordenada = ItemHistoria.ordenarPorFechaDesc(Arrays.asList(vieja, nueva));

        assertEquals("2026-07-20T11:00:00", ordenada.get(0).getFecha());
        assertEquals("2025-01-10T09:00:00", ordenada.get(1).getFecha());
    }

    @Test public void conservaLaFuenteYElId() {
        ItemHistoria local = new ItemHistoria("O", "2026-07-21T08:00:00", "Ulterior",
                Arrays.asList("Extracción x1"), ItemHistoria.Fuente.LOCAL, null, 77);
        assertEquals(ItemHistoria.Fuente.LOCAL, local.getFuente());
        assertEquals(77, local.getLocalId());
        assertEquals("O", local.getTipo());
    }
}
