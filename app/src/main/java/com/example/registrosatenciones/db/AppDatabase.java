package com.example.registrosatenciones.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.annotation.NonNull;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.registrosatenciones.db.dao.AtencionEnfermeriaDao;
import com.example.registrosatenciones.db.dao.AtencionOdontologiaDao;
import com.example.registrosatenciones.db.dao.DiagnosticoDao;
import com.example.registrosatenciones.db.dao.ObraSocialDao;
import com.example.registrosatenciones.db.dao.PacienteDao;
import com.example.registrosatenciones.db.dao.TipoPrestacionEnfermeriaDao;
import com.example.registrosatenciones.db.dao.TipoPrestacionOdontologiaDao;
import com.example.registrosatenciones.db.entity.AtencionEnfermeriaEntity;
import com.example.registrosatenciones.db.entity.AtencionOdontologiaEntity;
import com.example.registrosatenciones.db.entity.DiagnosticoEntity;
import com.example.registrosatenciones.db.entity.ObraSocialEntity;
import com.example.registrosatenciones.db.entity.OdontogramaEstadoEntity;
import com.example.registrosatenciones.db.entity.PacienteEntity;
import com.example.registrosatenciones.db.entity.PrestacionEnfermeriaEntity;
import com.example.registrosatenciones.db.entity.PrestacionOdontologiaEntity;
import com.example.registrosatenciones.db.entity.TipoPrestacionEnfermeriaEntity;
import com.example.registrosatenciones.db.entity.TipoPrestacionOdontologiaEntity;

@Database(
        entities = {
                PacienteEntity.class,
                AtencionEnfermeriaEntity.class,
                PrestacionEnfermeriaEntity.class,
                TipoPrestacionEnfermeriaEntity.class,
                AtencionOdontologiaEntity.class,
                PrestacionOdontologiaEntity.class,
                OdontogramaEstadoEntity.class,
                TipoPrestacionOdontologiaEntity.class,
                DiagnosticoEntity.class,
                ObraSocialEntity.class
        },
        version = 3,
        exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract PacienteDao pacienteDao();
    public abstract AtencionEnfermeriaDao atencionEnfermeriaDao();
    public abstract TipoPrestacionEnfermeriaDao tipoPrestacionEnfermeriaDao();
    public abstract AtencionOdontologiaDao atencionOdontologiaDao();
    public abstract TipoPrestacionOdontologiaDao tipoPrestacionOdontologiaDao();
    public abstract DiagnosticoDao diagnosticoDao();
    public abstract ObraSocialDao obraSocialDao();

    // Migración real de la versión 2 a la 3: la obra social deja de ser texto
    // libre y pasa a ser el identificador del padrón que publica el servidor.
    //
    // Se escribe a mano en vez de dejar que Room borre y recree la base, porque
    // borrarla se lleva puestas las atenciones que el enfermero todavía no
    // sincronizó, que es justamente lo que la app existe para no perder.
    //
    // SQLite no permite eliminar una columna en las versiones de Android que la
    // app soporta, así que las tablas afectadas se recrean: tabla nueva, copia
    // de los datos, borrado de la vieja, renombrado y recreación de los índices.
    // Las sentencias CREATE TABLE están copiadas literalmente de las que Room
    // genera; si no coincidieran carácter por carácter, Room aborta al abrir.
    static final Migration MIGRACION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `cat_obra_social` "
                    + "(`id` INTEGER NOT NULL, `nombre` TEXT, PRIMARY KEY(`id`))");

            // Antes de recrear las tablas se rescata el texto de obra social que
            // pudieran tener las atenciones pendientes: no se puede convertir a
            // identificador (el padrón todavía no se descargó), pero sí se puede
            // dejar en el paciente, que es donde el servidor lo termina
            // guardando de todas formas. Se toma la más reciente.
            db.execSQL("UPDATE pacientes SET obraSocial = ("
                    + "  SELECT a.nuevaObraSocial FROM atenciones_enfermeria a"
                    + "  WHERE a.pacienteLocalId = pacientes.localId"
                    + "    AND a.nuevaObraSocial IS NOT NULL AND TRIM(a.nuevaObraSocial) <> ''"
                    + "  ORDER BY a.fechaRegistroLocal DESC LIMIT 1)"
                    + " WHERE (obraSocial IS NULL OR TRIM(obraSocial) = '')"
                    + "   AND EXISTS (SELECT 1 FROM atenciones_enfermeria a"
                    + "               WHERE a.pacienteLocalId = pacientes.localId"
                    + "                 AND a.nuevaObraSocial IS NOT NULL AND TRIM(a.nuevaObraSocial) <> '')");

            db.execSQL("UPDATE pacientes SET obraSocial = ("
                    + "  SELECT a.nuevaObraSocial FROM atenciones_odontologia a"
                    + "  WHERE a.pacienteLocalId = pacientes.localId"
                    + "    AND a.nuevaObraSocial IS NOT NULL AND TRIM(a.nuevaObraSocial) <> ''"
                    + "  ORDER BY a.fechaRegistroLocal DESC LIMIT 1)"
                    + " WHERE (obraSocial IS NULL OR TRIM(obraSocial) = '')"
                    + "   AND EXISTS (SELECT 1 FROM atenciones_odontologia a"
                    + "               WHERE a.pacienteLocalId = pacientes.localId"
                    + "                 AND a.nuevaObraSocial IS NOT NULL AND TRIM(a.nuevaObraSocial) <> '')");

            // ── pacientes: obraSocial (texto) → obraSocialId + obraSocialNombre ──
            db.execSQL("CREATE TABLE IF NOT EXISTS `pacientes_nuevo` "
                    + "(`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `serverId` INTEGER, "
                    + "`dni` TEXT, `apellido` TEXT, `nombre` TEXT, `fechaNacimiento` TEXT, `sexo` TEXT, "
                    + "`domicilio` TEXT, `telefono` TEXT, `obraSocialId` INTEGER, `obraSocialNombre` TEXT, "
                    + "`edad` INTEGER, `syncState` INTEGER NOT NULL DEFAULT 0)");
            // El texto que había pasa al nombre; el identificador queda en nulo y
            // se completa cuando el servidor lo devuelva al sincronizar.
            db.execSQL("INSERT INTO `pacientes_nuevo` "
                    + "(`localId`, `serverId`, `dni`, `apellido`, `nombre`, `fechaNacimiento`, `sexo`, "
                    + " `domicilio`, `telefono`, `obraSocialId`, `obraSocialNombre`, `edad`, `syncState`) "
                    + "SELECT `localId`, `serverId`, `dni`, `apellido`, `nombre`, `fechaNacimiento`, `sexo`, "
                    + " `domicilio`, `telefono`, NULL, `obraSocial`, `edad`, `syncState` FROM `pacientes`");
            db.execSQL("DROP TABLE `pacientes`");
            db.execSQL("ALTER TABLE `pacientes_nuevo` RENAME TO `pacientes`");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_pacientes_dni` ON `pacientes` (`dni`)");

            // ── atenciones de enfermería: nuevaObraSocial (texto) → nuevaObraSocialId ──
            db.execSQL("CREATE TABLE IF NOT EXISTS `atenciones_enfermeria_nuevo` "
                    + "(`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `serverId` INTEGER, "
                    + "`pacienteLocalId` INTEGER NOT NULL, `tipoAtencion` INTEGER NOT NULL, "
                    + "`embarazada` INTEGER NOT NULL, `sinObraSocial` INTEGER NOT NULL, "
                    + "`nuevaObraSocialId` INTEGER, `observaciones` TEXT, `fechaRegistroLocal` TEXT, "
                    + "`institucionIdCaptura` INTEGER NOT NULL, `syncState` INTEGER NOT NULL DEFAULT 0)");
            db.execSQL("INSERT INTO `atenciones_enfermeria_nuevo` "
                    + "(`localId`, `serverId`, `pacienteLocalId`, `tipoAtencion`, `embarazada`, "
                    + " `sinObraSocial`, `nuevaObraSocialId`, `observaciones`, `fechaRegistroLocal`, "
                    + " `institucionIdCaptura`, `syncState`) "
                    + "SELECT `localId`, `serverId`, `pacienteLocalId`, `tipoAtencion`, `embarazada`, "
                    + " `sinObraSocial`, NULL, `observaciones`, `fechaRegistroLocal`, "
                    + " `institucionIdCaptura`, `syncState` FROM `atenciones_enfermeria`");
            db.execSQL("DROP TABLE `atenciones_enfermeria`");
            db.execSQL("ALTER TABLE `atenciones_enfermeria_nuevo` RENAME TO `atenciones_enfermeria`");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_atenciones_enfermeria_pacienteLocalId` "
                    + "ON `atenciones_enfermeria` (`pacienteLocalId`)");

            // ── atenciones de odontología: idem ──
            db.execSQL("CREATE TABLE IF NOT EXISTS `atenciones_odontologia_nuevo` "
                    + "(`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `serverId` INTEGER, "
                    + "`pacienteLocalId` INTEGER NOT NULL, `tipoConsulta` INTEGER NOT NULL, "
                    + "`tipoTurno` INTEGER NOT NULL, `diagnosticoId` INTEGER NOT NULL, "
                    + "`embarazada` INTEGER NOT NULL, `sinObraSocial` INTEGER NOT NULL, "
                    + "`nuevaObraSocialId` INTEGER, `observaciones` TEXT, `fechaRegistroLocal` TEXT, "
                    + "`institucionIdCaptura` INTEGER NOT NULL, `syncState` INTEGER NOT NULL DEFAULT 0, "
                    + "`cariesPerm` INTEGER NOT NULL, `perdidosPerm` INTEGER NOT NULL, "
                    + "`obturadosPerm` INTEGER NOT NULL, `cariesTemp` INTEGER NOT NULL, "
                    + "`extraccionTemp` INTEGER NOT NULL, `obturadosTemp` INTEGER NOT NULL)");
            db.execSQL("INSERT INTO `atenciones_odontologia_nuevo` "
                    + "(`localId`, `serverId`, `pacienteLocalId`, `tipoConsulta`, `tipoTurno`, "
                    + " `diagnosticoId`, `embarazada`, `sinObraSocial`, `nuevaObraSocialId`, "
                    + " `observaciones`, `fechaRegistroLocal`, `institucionIdCaptura`, `syncState`, "
                    + " `cariesPerm`, `perdidosPerm`, `obturadosPerm`, `cariesTemp`, `extraccionTemp`, "
                    + " `obturadosTemp`) "
                    + "SELECT `localId`, `serverId`, `pacienteLocalId`, `tipoConsulta`, `tipoTurno`, "
                    + " `diagnosticoId`, `embarazada`, `sinObraSocial`, NULL, "
                    + " `observaciones`, `fechaRegistroLocal`, `institucionIdCaptura`, `syncState`, "
                    + " `cariesPerm`, `perdidosPerm`, `obturadosPerm`, `cariesTemp`, `extraccionTemp`, "
                    + " `obturadosTemp` FROM `atenciones_odontologia`");
            db.execSQL("DROP TABLE `atenciones_odontologia`");
            db.execSQL("ALTER TABLE `atenciones_odontologia_nuevo` RENAME TO `atenciones_odontologia`");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_atenciones_odontologia_pacienteLocalId` "
                    + "ON `atenciones_odontologia` (`pacienteLocalId`)");
        }
    };

    private static volatile AppDatabase instancia;

    public static AppDatabase getInstancia(Context context) {
        if (instancia == null) {
            synchronized (AppDatabase.class) {
                if (instancia == null) {
                    instancia = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "atenciones.db")
                            // Sin fallbackToDestructiveMigration: ante un cambio
                            // de esquema sin migración escrita, preferimos que la
                            // app falle de forma visible antes que borrar en
                            // silencio atenciones que todavía no se sincronizaron.
                            .addMigrations(MIGRACION_2_3)
                            .build();
                }
            }
        }
        return instancia;
    }
}
