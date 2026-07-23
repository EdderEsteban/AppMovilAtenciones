package com.example.registrosatenciones.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.registrosatenciones.db.entity.TipoPrestacionEnfermeriaEntity;

import java.util.List;

@Dao
public interface TipoPrestacionEnfermeriaDao {

    // Al bajar el catálogo del server pisamos el cache (mismo id → REPLACE)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void guardarCatalogo(List<TipoPrestacionEnfermeriaEntity> tipos);

    @Query("SELECT * FROM tipos_prestacion_enfermeria ORDER BY grupo, nombrePrestacion")
    List<TipoPrestacionEnfermeriaEntity> listar();

    @Query("SELECT * FROM tipos_prestacion_enfermeria ORDER BY grupo, nombrePrestacion")
    LiveData<List<TipoPrestacionEnfermeriaEntity>> observarCatalogo();
}