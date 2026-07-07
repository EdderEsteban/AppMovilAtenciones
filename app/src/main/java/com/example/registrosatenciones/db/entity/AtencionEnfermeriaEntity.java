package com.example.registrosatenciones.db.entity;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "atenciones_enfermeria",
        indices = {@Index("pacienteLocalId")})
public class AtencionEnfermeriaEntity {

    @PrimaryKey(autoGenerate = true)
    private long localId;

    @Nullable
    private Integer serverId;

    private long pacienteLocalId;      // 🔑 apunta al localId del paciente, no al serverId

    private int tipoAtencion;          // 1 = Ambulatorio, 2 = Internado
    private boolean embarazada;
    private boolean sinObraSocial;
    @Nullable private String nuevaObraSocial;
    @Nullable private String observaciones;

    private String fechaRegistroLocal; // ISO "yyyy-MM-dd'T'HH:mm:ss" — cuándo se capturó
    private int institucionIdCaptura;  // institución activa al momento de capturar

    @ColumnInfo(defaultValue = "0")
    private int syncState;             // SyncEstado.*

    public long getLocalId() { return localId; }
    public void setLocalId(long localId) { this.localId = localId; }

    @Nullable public Integer getServerId() { return serverId; }
    public void setServerId(@Nullable Integer serverId) { this.serverId = serverId; }

    public long getPacienteLocalId() { return pacienteLocalId; }
    public void setPacienteLocalId(long pacienteLocalId) { this.pacienteLocalId = pacienteLocalId; }

    public int getTipoAtencion() { return tipoAtencion; }
    public void setTipoAtencion(int tipoAtencion) { this.tipoAtencion = tipoAtencion; }

    public boolean isEmbarazada() { return embarazada; }
    public void setEmbarazada(boolean embarazada) { this.embarazada = embarazada; }

    public boolean isSinObraSocial() { return sinObraSocial; }
    public void setSinObraSocial(boolean sinObraSocial) { this.sinObraSocial = sinObraSocial; }

    @Nullable public String getNuevaObraSocial() { return nuevaObraSocial; }
    public void setNuevaObraSocial(@Nullable String nuevaObraSocial) { this.nuevaObraSocial = nuevaObraSocial; }

    @Nullable public String getObservaciones() { return observaciones; }
    public void setObservaciones(@Nullable String observaciones) { this.observaciones = observaciones; }

    public String getFechaRegistroLocal() { return fechaRegistroLocal; }
    public void setFechaRegistroLocal(String fechaRegistroLocal) { this.fechaRegistroLocal = fechaRegistroLocal; }

    public int getInstitucionIdCaptura() { return institucionIdCaptura; }
    public void setInstitucionIdCaptura(int institucionIdCaptura) { this.institucionIdCaptura = institucionIdCaptura; }

    public int getSyncState() { return syncState; }
    public void setSyncState(int syncState) { this.syncState = syncState; }
}