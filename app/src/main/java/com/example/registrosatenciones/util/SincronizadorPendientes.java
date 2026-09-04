package com.example.registrosatenciones.util;

import android.content.Context;

import com.example.registrosatenciones.db.AppDatabase;
import com.example.registrosatenciones.db.SyncEstado;
import com.example.registrosatenciones.db.dao.AtencionEnfermeriaDao;
import com.example.registrosatenciones.db.dao.AtencionOdontologiaDao;
import com.example.registrosatenciones.db.dao.PacienteDao;
import com.example.registrosatenciones.db.entity.AtencionEnfermeriaEntity;
import com.example.registrosatenciones.db.entity.AtencionOdontologiaEntity;
import com.example.registrosatenciones.db.entity.OdontogramaEstadoEntity;
import com.example.registrosatenciones.db.entity.PacienteEntity;
import com.example.registrosatenciones.db.entity.PrestacionEnfermeriaEntity;
import com.example.registrosatenciones.db.entity.PrestacionOdontologiaEntity;
import com.example.registrosatenciones.request.ApiClient;
import com.example.registrosatenciones.request.CrearAtencionEnfermeriaRequest;
import com.example.registrosatenciones.request.CrearAtencionOdontologiaRequest;
import com.example.registrosatenciones.request.CrearPacienteRequest;
import com.example.registrosatenciones.request.OdontogramaEstadoItemRequest;
import com.example.registrosatenciones.request.PrestacionItemRequest;
import com.example.registrosatenciones.response.CrearAtencionResponse;
import com.example.registrosatenciones.response.CrearPacienteResponse;
import com.example.registrosatenciones.response.PacienteResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import retrofit2.Response;

// Ciclo de sincronización de lo pendiente (pacientes, atenciones de enfermería
// y de odontología) que cumplió la ventana de edición de 15 minutos. No hace
// interfaz de usuario: quien llama decide qué mostrar y qué hacer si la sesión
// perdió la institución activa.
public class SincronizadorPendientes {

    // Guarda de reentrancia: hay tres disparadores (botón manual, cuenta
    // regresiva de la pantalla de sincronización, barrido al entrar a Inicio) y
    // AppExecutors.io() es un pool de varios hilos, no serial. Sin esto, dos
    // corridas en paralelo pueden leer el mismo pendiente antes de que la primera
    // lo marque SINCRONIZADO y enviarlo dos veces al servidor.
    private static final AtomicBoolean enCurso = new AtomicBoolean(false);

    private SincronizadorPendientes() {}

    // Envía todo lo pendiente que ya cumplió la ventana de edición. Corre en el
    // hilo llamador, que debe ser de entrada/salida. Devuelve false si la sesión
    // perdió la institución activa (409): el llamador decide qué hacer con eso.
    // Si ya hay una corrida en curso, esta se descarta y devuelve true (como si
    // la sesión fuera válida): no se pierde nada, lo pendiente sigue pendiente y
    // se envía en el próximo disparo.
    public static boolean ejecutar(Context context) {
        if (!enCurso.compareAndSet(false, true)) {
            return true;
        }
        try {
            return ejecutarSinGuarda(context);
        } finally {
            enCurso.set(false);
        }
    }

    private static boolean ejecutarSinGuarda(Context context) {
        AppDatabase db = AppDatabase.getInstancia(context);
        PacienteDao pacienteDao = db.pacienteDao();
        AtencionEnfermeriaDao atencionDao = db.atencionEnfermeriaDao();
        AtencionOdontologiaDao atencionOdoDao = db.atencionOdontologiaDao();

        String token = PreferenciasUsuario.getAuthHeader(context);
        int institucionActiva = PreferenciasUsuario.getInstitucionActivaId(context);
        String fechaCorte = VentanaEdicion.fechaCorte();

        List<PacienteEntity> pacientesPendientes = pacienteDao.listarPorEstado(SyncEstado.PENDIENTE);
        for (PacienteEntity paciente : pacientesPendientes) {
            boolean sesionValida = sincronizarPaciente(pacienteDao, paciente, token);
            if (!sesionValida) return false;
        }

        List<AtencionEnfermeriaEntity> atencionesPendientes =
                atencionDao.listarEnviables(SyncEstado.PENDIENTE, fechaCorte);
        for (AtencionEnfermeriaEntity atencion : atencionesPendientes) {
            if (atencion.getInstitucionIdCaptura() != institucionActiva) {
                continue; // corresponde a otra institución; se sincroniza cuando el usuario vuelva ahí
            }
            boolean sesionValida = sincronizarAtencion(pacienteDao, atencionDao, atencion, token);
            if (!sesionValida) return false;
        }

        List<AtencionOdontologiaEntity> atencionesOdoPendientes =
                atencionOdoDao.listarEnviables(SyncEstado.PENDIENTE, fechaCorte);
        for (AtencionOdontologiaEntity atencion : atencionesOdoPendientes) {
            if (atencion.getInstitucionIdCaptura() != institucionActiva) {
                continue; // corresponde a otra institución; se sincroniza cuando el usuario vuelva ahí
            }
            boolean sesionValida = sincronizarAtencionOdo(pacienteDao, atencionOdoDao, atencion, token);
            if (!sesionValida) return false;
        }

        return true;
    }

    // Devuelve false si la sesión perdió la institución (409): no tiene sentido seguir intentando.
    private static boolean sincronizarPaciente(PacienteDao pacienteDao, PacienteEntity paciente, String token) {
        try {
            Response<List<PacienteResponse>> busqueda =
                    ApiClient.getApiAtenciones().buscarPacientes(token, paciente.getDni()).execute();

            if (!busqueda.isSuccessful() || busqueda.body() == null) return true; // se reintenta la próxima vez

            Integer serverId = null;
            for (PacienteResponse candidato : busqueda.body()) {
                if (candidato.getDni().equals(paciente.getDni())) {
                    serverId = candidato.getId();
                    break;
                }
            }

            if (serverId == null) {
                CrearPacienteRequest request = new CrearPacienteRequest(
                        paciente.getDni(), paciente.getApellido(), paciente.getNombre(),
                        paciente.getFechaNacimiento(), paciente.getSexo(),
                        paciente.getDomicilio(), paciente.getTelefono(), paciente.getObraSocialId());

                Response<CrearPacienteResponse> creacion =
                        ApiClient.getApiAtenciones().crearPaciente(token, request).execute();

                if (creacion.isSuccessful() && creacion.body() != null) {
                    serverId = creacion.body().getId();
                } else if (creacion.code() == 400) {
                    marcarError(pacienteDao, paciente); // ej: DNI duplicado — no se resuelve reintentando
                    return true;
                } else {
                    return true; // falla transitoria: se reintenta la próxima vez
                }
            }

            paciente.setServerId(serverId);
            paciente.setSyncState(SyncEstado.SINCRONIZADO);
            pacienteDao.actualizar(paciente);
            return true;

        } catch (IOException e) {
            return true; // se cortó la conexión a mitad de camino: queda PENDIENTE, se reintenta
        }
    }

    // Devuelve false si la sesión perdió la institución (409): no tiene sentido seguir intentando.
    private static boolean sincronizarAtencion(PacienteDao pacienteDao, AtencionEnfermeriaDao atencionDao,
                                                AtencionEnfermeriaEntity atencion, String token) {
        PacienteEntity paciente = pacienteDao.obtenerPorLocalId(atencion.getPacienteLocalId());
        if (paciente == null || paciente.getServerId() == null) {
            return true; // el paciente todavía no se sincronizó; se reintenta la próxima vez
        }

        List<PrestacionEnfermeriaEntity> prestacionesLocales = atencionDao.prestacionesDe(atencion.getLocalId());
        List<PrestacionItemRequest> prestaciones = new ArrayList<>();
        for (PrestacionEnfermeriaEntity p : prestacionesLocales) {
            prestaciones.add(new PrestacionItemRequest(p.getTipoPrestacionId(), p.getCantidad()));
        }

        CrearAtencionEnfermeriaRequest request = new CrearAtencionEnfermeriaRequest();
        request.setPacienteId(paciente.getServerId());
        request.setTipoAtencion(atencion.getTipoAtencion());
        request.setEmbarazada(atencion.isEmbarazada());
        request.setSinObraSocial(atencion.isSinObraSocial());
        request.setNuevaObraSocialId(atencion.getNuevaObraSocialId());
        request.setObservaciones(atencion.getObservaciones());
        request.setPrestaciones(prestaciones);

        try {
            Response<CrearAtencionResponse> respuesta =
                    ApiClient.getApiAtenciones().crearAtencionEnfermeria(token, request).execute();

            if (respuesta.isSuccessful() && respuesta.body() != null) {
                atencion.setServerId(respuesta.body().getId());
                atencion.setSyncState(SyncEstado.SINCRONIZADO);
                atencionDao.actualizar(atencion);
            } else if (respuesta.code() == 409 || respuesta.code() == 401) {
                // 409: la sesion perdio la institucion. 401: el token vencio (dura 12 h).
                // En los dos casos no tiene sentido seguir intentando: hay que reloguear.
                return false;
            } else if (respuesta.code() == 400) {
                marcarError(atencionDao, atencion); // error de negocio — no se resuelve reintentando
            }
            // otros códigos: queda PENDIENTE y se reintenta la próxima vez

            return true;

        } catch (IOException e) {
            return true; // sin conexión a mitad de camino: queda PENDIENTE
        }
    }

    // Devuelve false si la sesión perdió la institución (409): no tiene sentido seguir intentando.
    private static boolean sincronizarAtencionOdo(PacienteDao pacienteDao, AtencionOdontologiaDao atencionOdoDao,
                                                   AtencionOdontologiaEntity atencion, String token) {
        PacienteEntity paciente = pacienteDao.obtenerPorLocalId(atencion.getPacienteLocalId());
        if (paciente == null || paciente.getServerId() == null) {
            return true; // el paciente todavía no se sincronizó; se reintenta la próxima vez
        }

        List<PrestacionOdontologiaEntity> prestacionesLocales = atencionOdoDao.prestacionesDe(atencion.getLocalId());
        List<PrestacionItemRequest> prestaciones = new ArrayList<>();
        for (PrestacionOdontologiaEntity p : prestacionesLocales) {
            prestaciones.add(new PrestacionItemRequest(p.getTipoPrestacionId(), p.getCantidad()));
        }

        List<OdontogramaEstadoEntity> estadosLocales = atencionOdoDao.estadosDe(atencion.getLocalId());
        List<OdontogramaEstadoItemRequest> odontograma = new ArrayList<>();
        for (OdontogramaEstadoEntity e : estadosLocales) {
            odontograma.add(new OdontogramaEstadoItemRequest(e.getNumeroDiente(), e.getSuperficie(), e.getEstado()));
        }

        CrearAtencionOdontologiaRequest request = new CrearAtencionOdontologiaRequest();
        request.setPacienteId(paciente.getServerId());
        request.setTipoConsulta(atencion.getTipoConsulta());
        request.setTipoTurno(atencion.getTipoTurno());
        request.setDiagnosticoId(atencion.getDiagnosticoId());
        request.setEmbarazada(atencion.isEmbarazada());
        request.setSinObraSocial(atencion.isSinObraSocial());
        request.setNuevaObraSocialId(atencion.getNuevaObraSocialId());
        request.setObservaciones(atencion.getObservaciones());
        request.setPrestaciones(prestaciones);
        request.setOdontograma(odontograma);

        try {
            Response<CrearAtencionResponse> respuesta =
                    ApiClient.getApiAtenciones().crearAtencionOdontologia(token, request).execute();

            if (respuesta.isSuccessful() && respuesta.body() != null) {
                atencion.setServerId(respuesta.body().getId());
                atencion.setSyncState(SyncEstado.SINCRONIZADO);
                atencionOdoDao.actualizar(atencion);
            } else if (respuesta.code() == 409 || respuesta.code() == 401) {
                // 409: la sesion perdio la institucion. 401: el token vencio (dura 12 h).
                // En los dos casos no tiene sentido seguir intentando: hay que reloguear.
                return false;
            } else if (respuesta.code() == 400) {
                marcarError(atencionOdoDao, atencion); // error de negocio — no se resuelve reintentando
            }
            // otros códigos: queda PENDIENTE y se reintenta la próxima vez

            return true;

        } catch (IOException e) {
            return true; // sin conexión a mitad de camino: queda PENDIENTE
        }
    }

    private static void marcarError(PacienteDao dao, PacienteEntity paciente) {
        paciente.setSyncState(SyncEstado.ERROR);
        dao.actualizar(paciente);
    }

    private static void marcarError(AtencionEnfermeriaDao dao, AtencionEnfermeriaEntity atencion) {
        atencion.setSyncState(SyncEstado.ERROR);
        dao.actualizar(atencion);
    }

    private static void marcarError(AtencionOdontologiaDao dao, AtencionOdontologiaEntity atencion) {
        atencion.setSyncState(SyncEstado.ERROR);
        dao.actualizar(atencion);
    }
}
