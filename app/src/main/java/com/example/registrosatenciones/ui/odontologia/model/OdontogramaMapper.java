package com.example.registrosatenciones.ui.odontologia.model;

import com.example.registrosatenciones.response.OdontogramaEstadoResponse;

import java.util.ArrayList;
import java.util.List;

/** Convierte los estados del odontograma que devuelve el server a OdontogramaItem. */
public final class OdontogramaMapper {

    private OdontogramaMapper() {}

    public static List<OdontogramaItem> desdeRespuesta(List<OdontogramaEstadoResponse> estados) {
        List<OdontogramaItem> items = new ArrayList<>();
        if (estados == null) return items;
        for (OdontogramaEstadoResponse e : estados) {
            items.add(new OdontogramaItem(e.getNumeroDiente(), e.getSuperficie(), e.getEstado()));
        }
        return items;
    }
}
