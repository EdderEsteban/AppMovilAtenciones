package com.example.registrosatenciones.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.registrosatenciones.db.entity.TipoPrestacionOdontologiaEntity;

import java.util.List;

@Dao
public interface TipoPrestacionOdontologiaDao {

    // Al bajar el catálogo del server pisamos el cache (mismo id → REPLACE)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void guardarCatalogo(List<TipoPrestacionOdontologiaEntity> tipos);

    @Query("SELECT * FROM tipos_prestacion_odontologia ORDER BY nombre")
    List<TipoPrestacionOdontologiaEntity> listar();

    @Query("SELECT * FROM tipos_prestacion_odontologia ORDER BY nombre")
    LiveData<List<TipoPrestacionOdontologiaEntity>> observar();
}
