package com.example.registrosatenciones.ui.historiaclinica;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Ítem del timeline unificado de la ficha (una atención, de enfermería u odontología). */
public class ItemHistoria {

    public enum Fuente { ONLINE, LOCAL }

    private final String tipo;              // "E" | "O"
    private final String fecha;             // ISO
    private final String resumen;
    private final List<String> prestaciones;
    private final Fuente fuente;
    private final Integer serverId;         // presente cuando fuente == ONLINE
    private final long localId;             // presente cuando fuente == LOCAL

    public ItemHistoria(String tipo, String fecha, String resumen, List<String> prestaciones,
                        Fuente fuente, Integer serverId, long localId) {
        this.tipo = tipo;
        this.fecha = fecha;
        this.resumen = resumen;
        this.prestaciones = prestaciones != null ? prestaciones : new ArrayList<>();
        this.fuente = fuente;
        this.serverId = serverId;
        this.localId = localId;
    }

    public String getTipo() { return tipo; }
    public String getFecha() { return fecha; }
    public String getResumen() { return resumen; }
    public List<String> getPrestaciones() { return prestaciones; }
    public Fuente getFuente() { return fuente; }
    public Integer getServerId() { return serverId; }
    public long getLocalId() { return localId; }

    public boolean esOdontologia() { return "O".equals(tipo); }

    public static List<ItemHistoria> ordenarPorFechaDesc(List<ItemHistoria> items) {
        List<ItemHistoria> copia = new ArrayList<>(items);
        Collections.sort(copia, new Comparator<ItemHistoria>() {
            @Override public int compare(ItemHistoria a, ItemHistoria b) {
                String fa = a.getFecha() != null ? a.getFecha() : "";
                String fb = b.getFecha() != null ? b.getFecha() : "";
                return fb.compareTo(fa); // descendente
            }
        });
        return copia;
    }
}
