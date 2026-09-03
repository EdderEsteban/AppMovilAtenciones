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

    // Cuenta regresiva de la ventana de edición: el Handler late una vez por
    // segundo mientras haya al menos una atención con ventana abierta, y se
    // detiene solo cuando no queda ninguna (arranca de nuevo si aparece otra).
    private final MutableLiveData<Boolean> tic = new MutableLiveData<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = this::onTic;
    private boolean tickerActivo = false;

    private final LiveData<List<AtencionEnfermeriaEntity>> liveAtencionesPendientes;
    private final LiveData<List<AtencionOdontologiaEntity>> liveAtencionesOdoPendientes;
    private List<AtencionEnfermeriaEntity> cacheAtencionesPendientes = new ArrayList<>();
    private List<AtencionOdontologiaEntity> cacheAtencionesOdoPendientes = new ArrayList<>();
    private Set<String> ventanasAbiertasPrevias = new HashSet<>();

    private final Observer<List<AtencionEnfermeriaEntity>> observerAtenciones = lista -> {
        cacheAtencionesPendientes = lista != null ? lista : new ArrayList<>();
        evaluarTicker();
    };
    private final Observer<List<AtencionOdontologiaEntity>> observerAtencionesOdo = lista -> {
        cacheAtencionesOdoPendientes = lista != null ? lista : new ArrayList<>();
        evaluarTicker();
    };

    public SincronizacionViewModel(@NonNull Application application) {
        super(application);
        context = application.getApplicationContext();
        AppDatabase db = AppDatabase.getInstancia(context);
        pacienteDao = db.pacienteDao();
        atencionDao = db.atencionEnfermeriaDao();
        atencionOdoDao = db.atencionOdontologiaDao();

        liveAtencionesPendientes = observarAtencionesPendientes();
        liveAtencionesOdoPendientes = observarAtencionesOdoPendientes();
        liveAtencionesPendientes.observeForever(observerAtenciones);
        liveAtencionesOdoPendientes.observeForever(observerAtencionesOdo);
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

    public LiveData<Boolean> getTic() {
        return tic;
    }

    // Arranca el Handler si aparece alguna atención con ventana abierta y todavía
    // no estaba latiendo. Se llama cada vez que cambia la lista de pendientes.
    private void evaluarTicker() {
        if (!tickerActivo && hayVentanaAbierta()) {
            tickerActivo = true;
            handler.postDelayed(ticker, INTERVALO_TIC_MS);
        }
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

        tic.setValue(true);

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
        liveAtencionesPendientes.removeObserver(observerAtenciones);
        liveAtencionesOdoPendientes.removeObserver(observerAtencionesOdo);
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
