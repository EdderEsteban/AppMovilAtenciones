package com.example.registrosatenciones.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.registrosatenciones.db.entity.DiagnosticoEntity;

import java.util.List;

@Dao
public interface DiagnosticoDao {

    // Al bajar el catálogo del server pisamos el cache (mismo id → REPLACE)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void guardarCatalogo(List<DiagnosticoEntity> diagnosticos);

    @Query("SELECT * FROM cat_diagnostico ORDER BY codigo")
    List<DiagnosticoEntity> listar();
}
