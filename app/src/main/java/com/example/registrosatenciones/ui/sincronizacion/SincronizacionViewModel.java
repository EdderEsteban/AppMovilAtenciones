package com.example.registrosatenciones.ui.sincronizacion;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.registrosatenciones.db.AppDatabase;
import com.example.registrosatenciones.db.SyncEstado;
import com.example.registrosatenciones.db.dao.AtencionEnfermeriaDao;
import com.example.registrosatenciones.db.dao.AtencionOdontologiaDao;
import com.example.registrosatenciones.db.dao.PacienteDao;
import com.example.registrosatenciones.db.entity.AtencionEnfermeriaEntity;
import com.example.registrosatenciones.db.entity.AtencionOdontologiaEntity;
import com.example.registrosatenciones.db.entity.PacienteEntity;
import com.example.registrosatenciones.ui.login.LoginActivity;
import com.example.registrosatenciones.util.AppExecutors;
import com.example.registrosatenciones.util.Conectividad;
import com.example.registrosatenciones.util.PreferenciasUsuario;
import com.example.registrosatenciones.util.SincronizadorPendientes;
import com.example.registrosatenciones.util.VentanaEdicion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SincronizacionViewModel extends AndroidViewModel {

    private static final long INTERVALO_TIC_MS = 1000L;

    private final Context context;
    private final PacienteDao pacienteDao;
    private final AtencionEnfermeriaDao atencionDao;
    private final AtencionOdontologiaDao atencionOdoDao;

    private final MutableLiveData<Boolean> sincronizando = new MutableLiveData<>(false);

    // Lista ya armada para la pantalla: título, subtítulo y estado (incluido el
    // texto de la cuenta regresiva) resueltos acá. La Activity solo la observa y
    // la escribe; ningún cálculo de fecha ni formateo vive del lado de la UI.
    private final MutableLiveData<List<ItemPendiente>> items = new MutableLiveData<>(new ArrayList<>());

    // Cuenta regresiva de la ventana de edición: el Handler late una vez por
    // segundo mientras haya al menos una atención con ventana abierta, y se
    // detiene solo cuando no queda ninguna (arranca de nuevo si aparece otra).
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = this::onTic;
    private boolean tickerActivo = false;
    private boolean pausado = false;

    private final LiveData<List<PacienteEntity>> livePacientesPendientes;
    private final LiveData<List<PacienteEntity>> livePacientesConError;
    private final LiveData<List<AtencionEnfermeriaEntity>> liveAtencionesPendientes;
    private final LiveData<List<AtencionEnfermeriaEntity>> liveAtencionesConError;
    private final LiveData<List<AtencionOdontologiaEntity>> liveAtencionesOdoPendientes;
    private final LiveData<List<AtencionOdontologiaEntity>> liveAtencionesOdoConError;

    private List<PacienteEntity> cachePacientesPendientes = new ArrayList<>();
    private List<PacienteEntity> cachePacientesConError = new ArrayList<>();
    private List<AtencionEnfermeriaEntity> cacheAtencionesPendientes = new ArrayList<>();
    private List<AtencionEnfermeriaEntity> cacheAtencionesConError = new ArrayList<>();
    private List<AtencionOdontologiaEntity> cacheAtencionesOdoPendientes = new ArrayList<>();
    private List<AtencionOdontologiaEntity> cacheAtencionesOdoConError = new ArrayList<>();
    private Set<String> ventanasAbiertasPrevias = new HashSet<>();

    private final Observer<List<PacienteEntity>> observerPacientesPendientes = lista -> {
        cachePacientesPendientes = lista != null ? lista : new ArrayList<>();
        reconstruirItems();
    };
    private final Observer<List<PacienteEntity>> observerPacientesConError = lista -> {
        cachePacientesConError = lista != null ? lista : new ArrayList<>();
        reconstruirItems();
    };
    private final Observer<List<AtencionEnfermeriaEntity>> observerAtenciones = lista -> {
        cacheAtencionesPendientes = lista != null ? lista : new ArrayList<>();
        evaluarTicker();
        reconstruirItems();
    };
    private final Observer<List<AtencionEnfermeriaEntity>> observerAtencionesConError = lista -> {
        cacheAtencionesConError = lista != null ? lista : new ArrayList<>();
        reconstruirItems();
    };
    private final Observer<List<AtencionOdontologiaEntity>> observerAtencionesOdo = lista -> {
        cacheAtencionesOdoPendientes = lista != null ? lista : new ArrayList<>();
        evaluarTicker();
        reconstruirItems();
    };
    private final Observer<List<AtencionOdontologiaEntity>> observerAtencionesOdoConError = lista -> {
        cacheAtencionesOdoConError = lista != null ? lista : new ArrayList<>();
        reconstruirItems();
    };

    public SincronizacionViewModel(@NonNull Application application) {
        super(application);
        context = application.getApplicationContext();
        AppDatabase db = AppDatabase.getInstancia(context);
        pacienteDao = db.pacienteDao();
        atencionDao = db.atencionEnfermeriaDao();
        atencionOdoDao = db.atencionOdontologiaDao();

        livePacientesPendientes = observarPacientesPendientes();
        livePacientesConError = observarPacientesConError();
        liveAtencionesPendientes = observarAtencionesPendientes();
        liveAtencionesConError = observarAtencionesConError();
        liveAtencionesOdoPendientes = observarAtencionesOdoPendientes();
        liveAtencionesOdoConError = observarAtencionesOdoConError();

        livePacientesPendientes.observeForever(observerPacientesPendientes);
        livePacientesConError.observeForever(observerPacientesConError);
        liveAtencionesPendientes.observeForever(observerAtenciones);
        liveAtencionesConError.observeForever(observerAtencionesConError);
        liveAtencionesOdoPendientes.observeForever(observerAtencionesOdo);
        liveAtencionesOdoConError.observeForever(observerAtencionesOdoConError);
    }

    public LiveData<List<PacienteEntity>> observarPacientesPendientes() {
        return pacienteDao.observarPorEstado(SyncEstado.PENDIENTE);
    }

    public LiveData<List<PacienteEntity>> observarPacientesConError() {
        return pacienteDao.observarPorEstado(SyncEstado.ERROR);
    }

    public LiveData<List<AtencionEnfermeriaEntity>> observarAtencionesPendientes() {
        int institucionActiva = PreferenciasUsuario.getInstitucionActivaId(context);
        return atencionDao.observarPorEstadoEInstitucion(SyncEstado.PENDIENTE, institucionActiva);
    }

    public LiveData<List<AtencionEnfermeriaEntity>> observarAtencionesConError() {
        int institucionActiva = PreferenciasUsuario.getInstitucionActivaId(context);
        return atencionDao.observarPorEstadoEInstitucion(SyncEstado.ERROR, institucionActiva);
    }

    public LiveData<List<AtencionOdontologiaEntity>> observarAtencionesOdoPendientes() {
        int institucionActiva = PreferenciasUsuario.getInstitucionActivaId(context);
        return atencionOdoDao.observarPorEstadoEInstitucion(SyncEstado.PENDIENTE, institucionActiva);
    }

    public LiveData<List<AtencionOdontologiaEntity>> observarAtencionesOdoConError() {
        int institucionActiva = PreferenciasUsuario.getInstitucionActivaId(context);
        return atencionOdoDao.observarPorEstadoEInstitucion(SyncEstado.ERROR, institucionActiva);
    }

    public LiveData<Boolean> getSincronizando() {
        return sincronizando;
    }

    public LiveData<List<ItemPendiente>> getItems() {
        return items;
    }

    // Arma la lista que va a la pantalla: errores primero, después lo pendiente.
    // Acá vive todo el formateo (incluida la cuenta regresiva); la Activity solo
    // recibe el resultado y lo escribe.
    private void reconstruirItems() {
        List<ItemPendiente> lista = new ArrayList<>();

        for (PacienteEntity p : cachePacientesConError) {
            lista.add(new ItemPendiente(p.getApellido() + ", " + p.getNombre(), "No se pudo crear — revisá los datos", true));
        }
        for (AtencionEnfermeriaEntity a : cacheAtencionesConError) {
            String tipo = a.getTipoAtencion() == 1 ? "Ambulatorio" : "Internado";
            lista.add(new ItemPendiente("Atención · " + tipo, "No se pudo guardar — revisá los datos", true));
        }
        for (AtencionOdontologiaEntity a : cacheAtencionesOdoConError) {
            String tipo = a.getTipoConsulta() == 1 ? "1ª vez" : "Ulterior";
            lista.add(new ItemPendiente("Atención odont. · " + tipo, "No se pudo guardar — revisá los datos", true));
        }
        for (PacienteEntity p : cachePacientesPendientes) {
            lista.add(new ItemPendiente(p.getApellido() + ", " + p.getNombre(), "Paciente nuevo · se buscará por DNI", false));
        }
        for (AtencionEnfermeriaEntity a : cacheAtencionesPendientes) {
            String tipo = a.getTipoAtencion() == 1 ? "Ambulatorio" : "Internado";
            String tiempoRestante = VentanaEdicion.formatearRestante(a.getFechaRegistroLocal());
            lista.add(new ItemPendiente("Atención · " + tipo, a.getFechaRegistroLocal(), false, tiempoRestante));
        }
        for (AtencionOdontologiaEntity a : cacheAtencionesOdoPendientes) {
            String tipo = a.getTipoConsulta() == 1 ? "1ª vez" : "Ulterior";
            String tiempoRestante = VentanaEdicion.formatearRestante(a.getFechaRegistroLocal());
            lista.add(new ItemPendiente("Atención odont. · " + tipo, a.getFechaRegistroLocal(), false, tiempoRestante));
        }

        items.setValue(lista);
    }

    // Arranca el Handler si aparece alguna atención con ventana abierta y todavía
    // no estaba latiendo. Se llama cada vez que cambia la lista de pendientes.
    private void evaluarTicker() {
        if (pausado) return;
        if (!tickerActivo && hayVentanaAbierta()) {
            tickerActivo = true;
            handler.postDelayed(ticker, INTERVALO_TIC_MS);
        }
    }

    // Detiene el latido cuando la pantalla deja de estar visible. Sin esto el
    // Handler sigue corriendo con la app en segundo plano: gasta batería, puede
    // disparar una sincronización fuera de vista y, si esa corrida devuelve 409,
    // intenta abrir el login desde background, cosa que Android 10 en adelante
    // bloquea sin ningún error visible (el usuario perdería la sesión sin saberlo).
    public void pausarTicker() {
        pausado = true;
        tickerActivo = false;
        handler.removeCallbacks(ticker);
    }

    // Al volver a la pantalla se corre un tic inmediato en vez de esperar un
    // segundo: pone los tiempos al día de una y procesa los vencimientos que
    // hayan ocurrido mientras la pantalla no estaba a la vista.
    public void reanudarTicker() {
        pausado = false;
        handler.removeCallbacks(ticker);
        tickerActivo = true;
        onTic();
    }

    private boolean hayVentanaAbierta() {
        for (AtencionEnfermeriaEntity a : cacheAtencionesPendientes) {
            if (VentanaEdicion.estaAbierta(a.getFechaRegistroLocal())) return true;
        }
        for (AtencionOdontologiaEntity a : cacheAtencionesOdoPendientes) {
            if (VentanaEdicion.estaAbierta(a.getFechaRegistroLocal())) return true;
        }
        return false;
    }

    // Un tic por segundo: republica la lista (para que la Activity refresque el
    // texto de la cuenta regresiva) y detecta las atenciones que en este tic
    // cruzaron el cero para disparar el envío automático si hay conexión.
    private void onTic() {
        if (pausado) {
            tickerActivo = false;
            return;
        }

        Set<String> abiertasAhora = new HashSet<>();
        for (AtencionEnfermeriaEntity a : cacheAtencionesPendientes) {
            if (VentanaEdicion.estaAbierta(a.getFechaRegistroLocal())) {
                abiertasAhora.add("E" + a.getLocalId());
            }
        }
        for (AtencionOdontologiaEntity a : cacheAtencionesOdoPendientes) {
            if (VentanaEdicion.estaAbierta(a.getFechaRegistroLocal())) {
                abiertasAhora.add("O" + a.getLocalId());
            }
        }

        boolean algunaVencioEsteTic = false;
        for (String key : ventanasAbiertasPrevias) {
            if (!abiertasAhora.contains(key)) {
                algunaVencioEsteTic = true;
                break;
            }
        }
        ventanasAbiertasPrevias = abiertasAhora;

        reconstruirItems(); // republica la lista para refrescar el texto de la cuenta regresiva

        if (algunaVencioEsteTic && Conectividad.hayConexion(context)) {
            sincronizarAhora();
        }

        if (abiertasAhora.isEmpty()) {
            tickerActivo = false; // nada que contar: se detiene hasta que aparezca otra
        } else {
            handler.postDelayed(ticker, INTERVALO_TIC_MS);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        handler.removeCallbacks(ticker);
        livePacientesPendientes.removeObserver(observerPacientesPendientes);
        livePacientesConError.removeObserver(observerPacientesConError);
        liveAtencionesPendientes.removeObserver(observerAtenciones);
        liveAtencionesConError.removeObserver(observerAtencionesConError);
        liveAtencionesOdoPendientes.removeObserver(observerAtencionesOdo);
        liveAtencionesOdoConError.removeObserver(observerAtencionesOdoConError);
    }

    public void sincronizarAhora() {
        if (!Conectividad.hayConexion(context)) {
            Toast.makeText(context, "Sin conexión — no se puede sincronizar ahora", Toast.LENGTH_SHORT).show();
            return;
        }

        sincronizando.setValue(true);
        AppExecutors.io().execute(() -> {
            boolean sesionValida = SincronizadorPendientes.ejecutar(context);
            if (!sesionValida) {
                AppExecutors.ejecutarEnUI(this::irALoginPorConflicto);
            }
            finalizarSincronizacion();
        });
    }

    private void finalizarSincronizacion() {
        AppExecutors.ejecutarEnUI(() -> {
            sincronizando.setValue(false);
            Toast.makeText(context, "Sincronización finalizada", Toast.LENGTH_SHORT).show();
        });
    }

    private void irALoginPorConflicto() {
        Toast.makeText(context, "Tu sesión perdió la institución activa. Volvé a iniciar sesión.", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}
