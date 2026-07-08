package com.example.registrosatenciones.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.registrosatenciones.db.entity.PacienteEntity;

import java.util.List;

@Dao
public interface PacienteDao {

    @Insert
    long insertar(PacienteEntity paciente);          // devuelve el localId generado

    @Update
    void actualizar(PacienteEntity paciente);

    // Búsqueda offline (la usa la pantalla de Pacientes sin conexión)
    @Query("SELECT * FROM pacientes " +
            "WHERE dni LIKE '%' || :q || '%' " +
            "   OR apellido LIKE '%' || :q || '%' " +
            "   OR nombre   LIKE '%' || :q || '%' " +
            "ORDER BY apellido, nombre")
    LiveData<List<PacienteEntity>> buscar(String q);

    @Query("SELECT * FROM pacientes WHERE localId = :localId")
    LiveData<PacienteEntity> observar(long localId);

    // Reconciliación del sync (llamada sincrónica, en background)
    @Query("SELECT * FROM pacientes WHERE dni = :dni LIMIT 1")
    PacienteEntity buscarPorDni(String dni);

    @Query("SELECT * FROM pacientes WHERE syncState = :estado")
    List<PacienteEntity> listarPorEstado(int estado);   // p/ pendientes en el sync

    @Query("SELECT * FROM pacientes WHERE syncState = :estado ORDER BY apellido, nombre")
    LiveData<List<PacienteEntity>> observarPorEstado(int estado);

    @Query("SELECT * FROM pacientes WHERE localId = :localId")
    PacienteEntity obtenerPorLocalId(long localId);
}