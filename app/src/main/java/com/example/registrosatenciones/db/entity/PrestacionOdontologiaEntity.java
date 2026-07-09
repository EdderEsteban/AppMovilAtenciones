package com.example.registrosatenciones.db.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "prestaciones_odontologia",
        foreignKeys = @ForeignKey(
                entity = AtencionOdontologiaEntity.class,
                parentColumns = "localId",
                childColumns = "atencionLocalId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("atencionLocalId")})
public class PrestacionOdontologiaEntity {

    @PrimaryKey(autoGenerate = true)
    private long localId;

    private long atencionLocalId;   // FK → atenciones_odontologia.localId (CASCADE)
    private int tipoPrestacionId;   // id del catálogo (del server)
    private int cantidad;

    public long getLocalId() { return localId; }
    public void setLocalId(long localId) { this.localId = localId; }

    public long getAtencionLocalId() { return atencionLocalId; }
    public void setAtencionLocalId(long atencionLocalId) { this.atencionLocalId = atencionLocalId; }

    public int getTipoPrestacionId() { return tipoPrestacionId; }
    public void setTipoPrestacionId(int tipoPrestacionId) { this.tipoPrestacionId = tipoPrestacionId; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
}
