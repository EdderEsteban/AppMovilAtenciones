package com.example.registrosatenciones.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tipos_prestacion_enfermeria")
public class TipoPrestacionEnfermeriaEntity {

    @PrimaryKey
    private int id;                 // id del server (NO autogenerado)
    private String grupo;
    private String nombrePrestacion;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getGrupo() { return grupo; }
    public void setGrupo(String grupo) { this.grupo = grupo; }

    public String getNombrePrestacion() { return nombrePrestacion; }
    public void setNombrePrestacion(String nombrePrestacion) { this.nombrePrestacion = nombrePrestacion; }
}