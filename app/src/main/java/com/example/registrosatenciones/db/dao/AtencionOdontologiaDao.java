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
import com.example.registrosatenciones.db.relation.AtencionOdontologiaConDetalle;

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

    @Query("DELETE FROM prestaciones_odontologia WHERE atencionLocalId = :atencionLocalId")
    void borrarPrestacionesDe(long atencionLocalId);

    @Query("DELETE FROM odontograma_estados WHERE atencionLocalId = :atencionLocalId")
    void borrarEstadosDe(long atencionLocalId);

    // Lectura sincrónica para precargar el formulario de edición.
    @Transaction
    @Query("SELECT * FROM atenciones_odontologia WHERE localId = :atencionLocalId")
    AtencionOdontologiaConDetalle obtenerConDetalle(long atencionLocalId);

    // Reemplaza atención, prestaciones y odontograma en una sola transacción, por
    // la misma razón que guardarConDetalle: un borrado sin reinserción dejaría la
    // atención mutilada.
    @Transaction
    default void actualizarConDetalle(AtencionOdontologiaEntity atencion,
                                      List<PrestacionOdontologiaEntity> prestaciones,
                                      List<OdontogramaEstadoEntity> estados) {
        actualizar(atencion);
        borrarPrestacionesDe(atencion.getLocalId());
        borrarEstadosDe(atencion.getLocalId());
        for (PrestacionOdontologiaEntity p : prestaciones) {
            p.setAtencionLocalId(atencion.getLocalId());
        }
        for (OdontogramaEstadoEntity e : estados) {
            e.setAtencionLocalId(atencion.getLocalId());
        }
        insertarPrestaciones(prestaciones);
        insertarEstados(estados);
    }

    // Solo lo que ya cumplió la ventana de edición. La comparación de textos
    // funciona porque el formato ISO ordena igual que cronológicamente.
    @Query("SELECT * FROM atenciones_odontologia " +
           "WHERE syncState = :estado AND fechaRegistroLocal <= :fechaCorte")
    List<AtencionOdontologiaEntity> listarEnviables(int estado, String fechaCorte);

    @Query("SELECT * FROM prestaciones_odontologia WHERE atencionLocalId = :atencionLocalId")
    List<PrestacionOdontologiaEntity> prestacionesDe(long atencionLocalId);

    @Query("SELECT * FROM odontograma_estados WHERE atencionLocalId = :atencionLocalId")
    List<OdontogramaEstadoEntity> estadosDe(long atencionLocalId);

    // Atenciones de este equipo que todavía no subieron, para sumarlas a la historia online.
    @Transaction
    @Query("SELECT * FROM atenciones_odontologia " +
            "WHERE pacienteLocalId = :pacienteLocalId AND syncState = :estado")
    List<AtencionOdontologiaConDetalle> pendientesDe(long pacienteLocalId, int estado);

    // Estados del odontograma de la ÚLTIMA atención del paciente. Se usa para precargar una
    // atención nueva con lo ya registrado (igual que el GET Create de la web), de modo que el
    // odontólogo modifique sobre el histórico y no arranque de un odontograma en blanco.
    @Query("SELECT * FROM odontograma_estados WHERE atencionLocalId = " +
            "(SELECT localId FROM atenciones_odontologia WHERE pacienteLocalId = :pacienteLocalId " +
            "ORDER BY fechaRegistroLocal DESC LIMIT 1)")
    List<OdontogramaEstadoEntity> estadosDelUltimoOdontograma(long pacienteLocalId);

    @Query("SELECT * FROM atenciones_odontologia " +
            "WHERE syncState = :estado AND institucionIdCaptura = :institucionId " +
            "ORDER BY fechaRegistroLocal DESC")
    LiveData<List<AtencionOdontologiaEntity>> observarPorEstadoEInstitucion(int estado, int institucionId);

    // Timeline de la ficha (offline). Ordena por fecha de captura, más nueva primero.
    @Transaction
    @Query("SELECT * FROM atenciones_odontologia " +
            "WHERE pacienteLocalId = :pacienteLocalId " +
            "ORDER BY fechaRegistroLocal DESC")
    LiveData<List<AtencionOdontologiaConDetalle>> observarPorPaciente(long pacienteLocalId);

    // Detalle histórico (solo lectura), 100% local — no requiere conexión.
    @Transaction
    @Query("SELECT * FROM atenciones_odontologia WHERE localId = :atencionLocalId")
    LiveData<AtencionOdontologiaConDetalle> observarPorLocalId(long atencionLocalId);
}
