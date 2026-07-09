package com.example.registrosatenciones.db.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "odontograma_estados",
        foreignKeys = @ForeignKey(
                entity = AtencionOdontologiaEntity.class,
                parentColumns = "localId",
                childColumns = "atencionLocalId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("atencionLocalId")})
public class OdontogramaEstadoEntity {

    @PrimaryKey(autoGenerate = true)
    private long localId;

    private long atencionLocalId;   // FK → atenciones_odontologia.localId (CASCADE)
    private int numeroDiente;       // FDI: 11-48 permanentes, 51-85 temporarios
    private String superficie;      // V, D, L, M, O o * (todo el diente)
    private int estado;             // 1 Caries,2 Obturado,3 Ausente,4 ExtrIndicada,5 Corona

    public long getLocalId() { return localId; }
    public void setLocalId(long localId) { this.localId = localId; }

    public long getAtencionLocalId() { return atencionLocalId; }
    public void setAtencionLocalId(long atencionLocalId) { this.atencionLocalId = atencionLocalId; }

    public int getNumeroDiente() { return numeroDiente; }
    public void setNumeroDiente(int numeroDiente) { this.numeroDiente = numeroDiente; }

    public String getSuperficie() { return superficie; }
    public void setSuperficie(String superficie) { this.superficie = superficie; }

    public int getEstado() { return estado; }
    public void setEstado(int estado) { this.estado = estado; }
}
