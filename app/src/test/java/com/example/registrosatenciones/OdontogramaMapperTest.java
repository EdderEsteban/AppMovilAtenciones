package com.example.registrosatenciones;

import static org.junit.Assert.assertEquals;

import com.example.registrosatenciones.response.OdontogramaEstadoResponse;
import com.example.registrosatenciones.ui.odontologia.model.OdontogramaItem;
import com.example.registrosatenciones.ui.odontologia.model.OdontogramaMapper;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class OdontogramaMapperTest {

    private static OdontogramaEstadoResponse estado(int diente, String sup, int est) {
        OdontogramaEstadoResponse e = new OdontogramaEstadoResponse();
        e.setNumeroDiente(diente);
        e.setSuperficie(sup);
        e.setEstado(est);
        return e;
    }

    @Test public void mapeaCadaEstadoAUnItem() {
        List<OdontogramaItem> items = OdontogramaMapper.desdeRespuesta(Arrays.asList(
                estado(11, "V", 1), estado(36, "*", 3)));
        assertEquals(2, items.size());
        assertEquals(11, items.get(0).getNumeroDiente());
        assertEquals("V", items.get(0).getSuperficie());
        assertEquals(1, items.get(0).getEstado());
        assertEquals(36, items.get(1).getNumeroDiente());
        assertEquals("*", items.get(1).getSuperficie());
        assertEquals(3, items.get(1).getEstado());
    }

    @Test public void listaNulaDaListaVacia() {
        assertEquals(0, OdontogramaMapper.desdeRespuesta(null).size());
    }
}
