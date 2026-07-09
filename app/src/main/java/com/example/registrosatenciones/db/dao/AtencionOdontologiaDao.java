package com.example.registrosatenciones.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.example.registrosatenciones.db.entity.AtencionOdontologiaEntity;
import com.example.registrosatenciones.db.entity.OdontogramaEstadoEntity;
import com.example.registrosatenciones.db.entity.PrestacionOdontologiaEntity;

import java.util.List;

@Dao
public interface AtencionOdontologiaDao {

    @Insert
    long insertar(AtencionOdontologiaEntity atencion);

    @Insert
    void insertarPrestaciones(List<PrestacionOdontologiaEntity> prestaciones);

    @Insert
    void insertarEstados(List<OdontogramaEstadoEntity> estados);

    // Guarda atención + prestaciones + odontograma en una sola transacción atómica.
    @Transaction
    default long guardarConDetalle(AtencionOdontologiaEntity atencion,
                                   List<PrestacionOdontologiaEntity> prestaciones,
                                   List<OdontogramaEstadoEntity> estados) {
        long atencionLocalId = insertar(atencion);
        for (PrestacionOdontologiaEntity p : prestaciones) {
            p.setAtencionLocalId(atencionLocalId);
        }
        for (OdontogramaEstadoEntity e : estados) {
            e.setAtencionLocalId(atencionLocalId);
        }
        insertarPrestaciones(prestaciones);
        insertarEstados(estados);
        return atencionLocalId;
    }

    @Update
    void actualizar(AtencionOdontologiaEntity atencion);

    // Para el sync (sincrónicas, en background)
    @Query("SELECT * FROM atenciones_odontologia WHERE syncState = :estado")
    List<AtencionOdontologiaEntity> listarPorEstado(int estado);

    @Query("SELECT * FROM prestaciones_odontologia WHERE atencionLocalId = :atencionLocalId")
    List<PrestacionOdontologiaEntity> prestacionesDe(long atencionLocalId);

    @Query("SELECT * FROM odontograma_estados WHERE atencionLocalId = :atencionLocalId")
    List<OdontogramaEstadoEntity> estadosDe(long atencionLocalId);

    @Query("SELECT * FROM atenciones_odontologia " +
            "WHERE syncState = :estado AND institucionIdCaptura = :institucionId " +
            "ORDER BY fechaRegistroLocal DESC")
    LiveData<List<AtencionOdontologiaEntity>> observarPorEstadoEInstitucion(int estado, int institucionId);
}
