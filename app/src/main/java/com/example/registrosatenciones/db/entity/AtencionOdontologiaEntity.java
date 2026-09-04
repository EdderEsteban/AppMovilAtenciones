package com.example.registrosatenciones.db.entity;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "atenciones_odontologia",
        indices = {@Index("pacienteLocalId")})
public class AtencionOdontologiaEntity {

    @PrimaryKey(autoGenerate = true)
    private long localId;

    @Nullable
    private Integer serverId;

    private long pacienteLocalId;      // 🔑 apunta al localId del paciente, no al serverId

    private int tipoConsulta;          // 1=1ª vez, 2=Ulterior
    private int tipoTurno;             // 1=Ventanilla,2=Profesional,3=Demanda,4=Interdisc
    private int diagnosticoId;
    private boolean embarazada;
    private boolean sinObraSocial;
    @Nullable private Integer nuevaObraSocialId;
    @Nullable private String observaciones;

    private String fechaRegistroLocal; // ISO "yyyy-MM-dd'T'HH:mm:ss" — cuándo se capturó
    private int institucionIdCaptura;  // institución activa al momento de capturar

    @ColumnInfo(defaultValue = "0")
    private int syncState;             // SyncEstado.*

    // CPO/ceo calculado localmente (feedback offline; el server recalcula al sincronizar)
    private int cariesPerm;
    private int perdidosPerm;
    private int obturadosPerm;
    private int cariesTemp;
    private int extraccionTemp;
    private int obturadosTemp;

    public long getLocalId() { return localId; }
    public void setLocalId(long localId) { this.localId = localId; }

    @Nullable public Integer getServerId() { return serverId; }
    public void setServerId(@Nullable Integer serverId) { this.serverId = serverId; }

    public long getPacienteLocalId() { return pacienteLocalId; }
    public void setPacienteLocalId(long pacienteLocalId) { this.pacienteLocalId = pacienteLocalId; }

    public int getTipoConsulta() { return tipoConsulta; }
    public void setTipoConsulta(int tipoConsulta) { this.tipoConsulta = tipoConsulta; }

    public int getTipoTurno() { return tipoTurno; }
    public void setTipoTurno(int tipoTurno) { this.tipoTurno = tipoTurno; }

    public int getDiagnosticoId() { return diagnosticoId; }
    public void setDiagnosticoId(int diagnosticoId) { this.diagnosticoId = diagnosticoId; }

    public boolean isEmbarazada() { return embarazada; }
    public void setEmbarazada(boolean embarazada) { this.embarazada = embarazada; }

    public boolean isSinObraSocial() { return sinObraSocial; }
    public void setSinObraSocial(boolean sinObraSocial) { this.sinObraSocial = sinObraSocial; }

    @Nullable public Integer getNuevaObraSocialId() { return nuevaObraSocialId; }
    public void setNuevaObraSocialId(@Nullable Integer nuevaObraSocialId) { this.nuevaObraSocialId = nuevaObraSocialId; }

    @Nullable public String getObservaciones() { return observaciones; }
    public void setObservaciones(@Nullable String observaciones) { this.observaciones = observaciones; }

    public String getFechaRegistroLocal() { return fechaRegistroLocal; }
    public void setFechaRegistroLocal(String fechaRegistroLocal) { this.fechaRegistroLocal = fechaRegistroLocal; }

    public int getInstitucionIdCaptura() { return institucionIdCaptura; }
    public void setInstitucionIdCaptura(int institucionIdCaptura) { this.institucionIdCaptura = institucionIdCaptura; }

    public int getSyncState() { return syncState; }
    public void setSyncState(int syncState) { this.syncState = syncState; }

    public int getCariesPerm() { return cariesPerm; }
    public void setCariesPerm(int cariesPerm) { this.cariesPerm = cariesPerm; }

    public int getPerdidosPerm() { return perdidosPerm; }
    public void setPerdidosPerm(int perdidosPerm) { this.perdidosPerm = perdidosPerm; }

    public int getObturadosPerm() { return obturadosPerm; }
    public void setObturadosPerm(int obturadosPerm) { this.obturadosPerm = obturadosPerm; }

    public int getCariesTemp() { return cariesTemp; }
    public void setCariesTemp(int cariesTemp) { this.cariesTemp = cariesTemp; }

    public int getExtraccionTemp() { return extraccionTemp; }
    public void setExtraccionTemp(int extraccionTemp) { this.extraccionTemp = extraccionTemp; }

    public int getObturadosTemp() { return obturadosTemp; }
    public void setObturadosTemp(int obturadosTemp) { this.obturadosTemp = obturadosTemp; }
}
