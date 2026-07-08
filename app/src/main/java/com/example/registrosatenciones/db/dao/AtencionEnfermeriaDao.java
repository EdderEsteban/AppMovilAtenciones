package com.example.registrosatenciones.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.example.registrosatenciones.db.entity.AtencionEnfermeriaEntity;
import com.example.registrosatenciones.db.entity.PrestacionEnfermeriaEntity;
import com.example.registrosatenciones.db.relation.AtencionConPrestaciones;

import java.util.List;

@Dao
public interface AtencionEnfermeriaDao {

    @Insert
    long insertar(AtencionEnfermeriaEntity atencion);

    @Insert
    void insertarPrestaciones(List<PrestacionEnfermeriaEntity> prestaciones);

    // Guarda atención + sus prestaciones en una sola transacción atómica.
    @Transaction
    default long guardarConPrestaciones(AtencionEnfermeriaEntity atencion,
                                        List<PrestacionEnfermeriaEntity> prestaciones) {
        long atencionLocalId = insertar(atencion);
        for (PrestacionEnfermeriaEntity p : prestaciones) {
            p.setAtencionLocalId(atencionLocalId);
        }
        insertarPrestaciones(prestaciones);
        return atencionLocalId;
    }

    @Update
    void actualizar(AtencionEnfermeriaEntity atencion);

    // Timeline de la ficha (offline). Ordena por fecha de captura, más nueva primero.
    @Transaction
    @Query("SELECT * FROM atenciones_enfermeria " +
            "WHERE pacienteLocalId = :pacienteLocalId " +
            "ORDER BY fechaRegistroLocal DESC")
    LiveData<List<AtencionConPrestaciones>> observarPorPaciente(long pacienteLocalId);

    // Para el sync (sincrónicas, en background)
    @Query("SELECT * FROM atenciones_enfermeria WHERE syncState = :estado")
    List<AtencionEnfermeriaEntity> listarPorEstado(int estado);

    @Query("SELECT * FROM prestaciones_enfermeria WHERE atencionLocalId = :atencionLocalId")
    List<PrestacionEnfermeriaEntity> prestacionesDe(long atencionLocalId);

    @Query("SELECT * FROM atenciones_enfermeria " +
            "WHERE syncState = :estado AND institucionIdCaptura = :institucionId " +
            "ORDER BY fechaRegistroLocal DESC")
    LiveData<List<AtencionEnfermeriaEntity>> observarPorEstadoEInstitucion(int estado, int institucionId);
}