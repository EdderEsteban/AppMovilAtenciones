package com.example.registrosatenciones.ui.odontologia.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Portado 1:1 de wwwroot/js/odontograma.js (aplicarEstado / calcularCPO) del panel .NET.
public class OdontogramaModelo {
    public static final int SANO = 0, CARIES = 1, OBTURADO = 2, AUSENTE = 3, EXTRACCION = 4, CORONA = 5;

    private final Map<Integer, Map<String, Integer>> estados = new HashMap<>();

    public Map<String, Integer> estadoDe(int diente) {
        return estados.get(diente);
    }

    public void aplicar(int diente, String superficie, int estado) {
        Map<String, Integer> sups = estados.get(diente);

        if (estado == AUSENTE || estado == EXTRACCION) {
            Map<String, Integer> nuevo = new HashMap<>();
            nuevo.put("*", estado);
            estados.put(diente, nuevo);

        } else if (estado == SANO) {
            if (sups == null) return;
            if ("*".equals(superficie)) {
                estados.remove(diente);
            } else {
                sups.remove(superficie);
                sups.remove("*");
                if (sups.isEmpty()) estados.remove(diente);
            }

        } else { // CARIES, OBTURADO, CORONA
            if (sups == null) {
                sups = new HashMap<>();
                estados.put(diente, sups);
            }
            sups.remove("*");
            String destino = "*".equals(superficie) ? "O" : superficie;
            sups.put(destino, estado);
        }
    }

    public List<OdontogramaItem> aplanar() {
        List<OdontogramaItem> lista = new ArrayList<>();
        for (Map.Entry<Integer, Map<String, Integer>> d : estados.entrySet()) {
            for (Map.Entry<String, Integer> s : d.getValue().entrySet()) {
                if (s.getValue() != null && s.getValue() > 0) {
                    lista.add(new OdontogramaItem(d.getKey(), s.getKey(), s.getValue()));
                }
            }
        }
        return lista;
    }

    public void cargar(List<OdontogramaItem> lista) {
        estados.clear();
        if (lista == null) return;
        for (OdontogramaItem it : lista) {
            Map<String, Integer> sups = estados.get(it.getNumeroDiente());
            if (sups == null) {
                sups = new HashMap<>();
                estados.put(it.getNumeroDiente(), sups);
            }
            sups.put(it.getSuperficie(), it.getEstado());
        }
    }

    public ValoracionLocal calcularCpo() {
        ValoracionLocal v = new ValoracionLocal();

        for (int num : DientesFdi.PERMANENTES) {
            Map<String, Integer> sups = estados.get(num);
            if (sups == null) continue;
            Integer estrella = sups.get("*");
            if (estrella != null && estrella == AUSENTE) { v.perdidosPerm++; continue; }
            if (estrella != null && estrella == EXTRACCION) continue;
            if (contieneValor(sups, CARIES)) v.cariesPerm++;
            else if (contieneValor(sups, OBTURADO) || contieneValor(sups, CORONA)) v.obturadosPerm++;
        }

        for (int num : DientesFdi.TEMPORARIOS) {
            Map<String, Integer> sups = estados.get(num);
            if (sups == null) continue;
            Integer estrella = sups.get("*");
            if (estrella != null && estrella == AUSENTE) continue;
            if (estrella != null && estrella == EXTRACCION) { v.extraccionTemp++; continue; }
            if (contieneValor(sups, CARIES)) v.cariesTemp++;
            else if (contieneValor(sups, OBTURADO) || contieneValor(sups, CORONA)) v.obturadosTemp++;
        }

        return v;
    }

    private static boolean contieneValor(Map<String, Integer> sups, int valor) {
        for (Integer e : sups.values()) {
            if (e != null && e == valor) return true;
        }
        return false;
    }
}
