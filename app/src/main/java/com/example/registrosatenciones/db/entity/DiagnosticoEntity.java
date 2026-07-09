package com.example.registrosatenciones.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cat_diagnostico")
public class DiagnosticoEntity {

    @PrimaryKey
    private int id;                 // id del server (NO autogenerado)
    private String codigo;
    private String descripcion;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
