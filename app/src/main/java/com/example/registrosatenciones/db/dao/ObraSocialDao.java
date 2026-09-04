package com.example.registrosatenciones.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.registrosatenciones.db.entity.ObraSocialEntity;

import java.util.List;

@Dao
public interface ObraSocialDao {

    // Al bajar el catálogo del server pisamos el cache (mismo id → REPLACE)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void guardarCatalogo(List<ObraSocialEntity> obrasSociales);

    @Query("SELECT * FROM cat_obra_social ORDER BY nombre")
    List<ObraSocialEntity> listar();

    // El nombre se usa para mostrar la obra social de un paciente cuyo registro
    // todavía no trae el nombre desnormalizado (por ejemplo, uno que llegó del
    // servidor antes de que existiera ese campo).
    @Query("SELECT nombre FROM cat_obra_social WHERE id = :id")
    String nombreDe(int id);
}
