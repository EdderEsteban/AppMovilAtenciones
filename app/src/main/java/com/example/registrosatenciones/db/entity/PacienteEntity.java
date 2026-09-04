package com.example.registrosatenciones.db.entity;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "pacientes",
        indices = {@Index(value = "dni", unique = true)})
public class PacienteEntity {

    @PrimaryKey(autoGenerate = true)
    private long localId;

    @Nullable
    private Integer serverId;      // null hasta que exista/sincronice en el servidor

    private String dni;            // clave de reconciliación
    private String apellido;
    private String nombre;
    private String fechaNacimiento; // ISO "yyyy-MM-dd"
    private String sexo;            // "F" | "M"

    @Nullable private String domicilio;
    @Nullable private String telefono;
    // Se guardan los dos a propósito. El identificador es lo que viaja al
    // servidor; el nombre está desnormalizado para poder mostrar la obra social
    // sin conexión y sin depender de que el catálogo local esté al día.
    @Nullable private Integer obraSocialId;
    @Nullable private String obraSocialNombre;
    @Nullable private Integer edad;    // cacheada de la búsqueda del servidor (no se recalcula en el cliente)

    @ColumnInfo(defaultValue = "0")
    private int syncState;          // SyncEstado.PENDIENTE / SINCRONIZADO / ERROR

    // --- getters / setters ---
    public long getLocalId() { return localId; }
    public void setLocalId(long localId) { this.localId = localId; }

    @Nullable public Integer getServerId() { return serverId; }
    public void setServerId(@Nullable Integer serverId) { this.serverId = serverId; }

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(String fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }

    @Nullable public String getDomicilio() { return domicilio; }
    public void setDomicilio(@Nullable String domicilio) { this.domicilio = domicilio; }

    @Nullable public String getTelefono() { return telefono; }
    public void setTelefono(@Nullable String telefono) { this.telefono = telefono; }

    @Nullable public Integer getObraSocialId() { return obraSocialId; }
    public void setObraSocialId(@Nullable Integer obraSocialId) { this.obraSocialId = obraSocialId; }

    @Nullable public String getObraSocialNombre() { return obraSocialNombre; }
    public void setObraSocialNombre(@Nullable String obraSocialNombre) { this.obraSocialNombre = obraSocialNombre; }

    // El paciente "tiene obra social" si quedó registrada de alguna forma: con
    // identificador del padrón, o solo con el nombre que venía cargado como
    // texto antes de que existiera el padrón.
    public boolean tieneObraSocial() {
        return obraSocialId != null || (obraSocialNombre != null && !obraSocialNombre.isEmpty());
    }

    public int getSyncState() { return syncState; }
    public void setSyncState(int syncState) { this.syncState = syncState; }

    @Nullable public Integer getEdad() { return edad; }
    public void setEdad(@Nullable Integer edad) { this.edad = edad; }
}