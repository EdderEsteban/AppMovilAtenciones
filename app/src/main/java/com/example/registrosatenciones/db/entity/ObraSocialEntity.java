package com.example.registrosatenciones.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cat_obra_social")
public class ObraSocialEntity {

    @PrimaryKey
    private int id;                 // id del server (NO autogenerado)
    private String nombre;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
