package com.example.registrosatenciones.db.relation;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.example.registrosatenciones.db.entity.AtencionOdontologiaEntity;
import com.example.registrosatenciones.db.entity.OdontogramaEstadoEntity;
import com.example.registrosatenciones.db.entity.PrestacionOdontologiaEntity;

import java.util.List;

public class AtencionOdontologiaConDetalle {

    @Embedded
    private AtencionOdontologiaEntity atencion;

    @Relation(parentColumn = "localId", entityColumn = "atencionLocalId")
    private List<PrestacionOdontologiaEntity> prestaciones;

    @Relation(parentColumn = "localId", entityColumn = "atencionLocalId")
    private List<OdontogramaEstadoEntity> estados;

    public AtencionOdontologiaEntity getAtencion() { return atencion; }
    public void setAtencion(AtencionOdontologiaEntity atencion) { this.atencion = atencion; }

    public List<PrestacionOdontologiaEntity> getPrestaciones() { return prestaciones; }
    public void setPrestaciones(List<PrestacionOdontologiaEntity> prestaciones) { this.prestaciones = prestaciones; }

    public List<OdontogramaEstadoEntity> getEstados() { return estados; }
    public void setEstados(List<OdontogramaEstadoEntity> estados) { this.estados = estados; }
}
