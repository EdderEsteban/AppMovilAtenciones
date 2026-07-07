package com.example.registrosatenciones.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.registrosatenciones.db.dao.AtencionEnfermeriaDao;
import com.example.registrosatenciones.db.dao.PacienteDao;
import com.example.registrosatenciones.db.dao.TipoPrestacionEnfermeriaDao;
import com.example.registrosatenciones.db.entity.AtencionEnfermeriaEntity;
import com.example.registrosatenciones.db.entity.PacienteEntity;
import com.example.registrosatenciones.db.entity.PrestacionEnfermeriaEntity;
import com.example.registrosatenciones.db.entity.TipoPrestacionEnfermeriaEntity;

@Database(
        entities = {
                PacienteEntity.class,
                AtencionEnfermeriaEntity.class,
                PrestacionEnfermeriaEntity.class,
                TipoPrestacionEnfermeriaEntity.class
        },
        version = 1,
        exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract PacienteDao pacienteDao();
    public abstract AtencionEnfermeriaDao atencionEnfermeriaDao();
    public abstract TipoPrestacionEnfermeriaDao tipoPrestacionEnfermeriaDao();

    private static volatile AppDatabase instancia;

    public static AppDatabase getInstancia(Context context) {
        if (instancia == null) {
            synchronized (AppDatabase.class) {
                if (instancia == null) {
                    instancia = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "atenciones.db")
                            .build();
                }
            }
        }
        return instancia;
    }
}