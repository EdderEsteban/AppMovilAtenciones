package com.example.registrosatenciones.db.relation;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.example.registrosatenciones.db.entity.AtencionEnfermeriaEntity;
import com.example.registrosatenciones.db.entity.PrestacionEnfermeriaEntity;

import java.util.List;

public class AtencionConPrestaciones {

    @Embedded
    private AtencionEnfermeriaEntity atencion;

    @Relation(parentColumn = "localId", entityColumn = "atencionLocalId")
    private List<PrestacionEnfermeriaEntity> prestaciones;

    public AtencionEnfermeriaEntity getAtencion() { return atencion; }
    public void setAtencion(AtencionEnfermeriaEntity atencion) { this.atencion = atencion; }

    public List<PrestacionEnfermeriaEntity> getPrestaciones() { return prestaciones; }
    public void setPrestaciones(List<PrestacionEnfermeriaEntity> prestaciones) { this.prestaciones = prestaciones; }
}