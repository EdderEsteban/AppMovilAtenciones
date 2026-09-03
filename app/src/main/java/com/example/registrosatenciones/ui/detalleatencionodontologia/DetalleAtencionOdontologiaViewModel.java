package com.example.registrosatenciones.ui.detalleatencionodontologia;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.registrosatenciones.db.AppDatabase;
import com.example.registrosatenciones.db.SyncEstado;
import com.example.registrosatenciones.db.dao.AtencionOdontologiaDao;
import com.example.registrosatenciones.db.dao.DiagnosticoDao;
import com.example.registrosatenciones.db.dao.PacienteDao;
import com.example.registrosatenciones.db.dao.TipoPrestacionOdontologiaDao;
import com.example.registrosatenciones.db.entity.AtencionOdontologiaEntity;
import com.example.registrosatenciones.db.entity.DiagnosticoEntity;
import com.example.registrosatenciones.db.entity.PacienteEntity;
import com.example.registrosatenciones.db.entity.TipoPrestacionOdontologiaEntity;
import com.example.registrosatenciones.db.relation.AtencionOdontologiaConDetalle;
import com.example.registrosatenciones.request.ApiClient;
import com.example.registrosatenciones.response.AtencionOdontologiaDetalleResponse;
import com.example.registrosatenciones.util.AppExecutors;
import com.example.registrosatenciones.util.PreferenciasUsuario;
import com.example.registrosatenciones.util.VentanaEdicion;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetalleAtencionOdontologiaViewModel extends AndroidViewModel {

    private final Context contexto;
    private final PacienteDao pacienteDao;
    private final AtencionOdontologiaDao atencionDao;
    private final TipoPrestacionOdontologiaDao tipoPrestacionDao;
    private final DiagnosticoDao diagnosticoDao;

    private final MutableLiveData<List<TipoPrestacionOdontologiaEntity>> tiposPrestacion = new MutableLiveData<>();
    private final MutableLiveData<List<DiagnosticoEntity>> diagnosticos = new MutableLiveData<>();
    private final MutableLiveData<AtencionOdontologiaDetalleResponse> detalleOnline = new MutableLiveData<>();
    private final MutableLiveData<Boolean> errorOnline = new MutableLiveData<>();

    // Falso hasta que observarDetalle() enganche una atención local. En el modo
    // online (odontograma traído del servidor) no hay atención local que editar:
    // no se llama a observarDetalle y puedeEditar queda en falso toda la vida
    // de la pantalla.
    private final MediatorLiveData<Boolean> puedeEditar = new MediatorLiveData<>();

    // Fuente actualmente enganchada a puedeEditar. Se necesita para poder
    // sacarla antes de enganchar una nueva: la Activity no declara
    // configChanges, así que una rotación la destruye y recrea, y onCreate
    // vuelve a llamar a observarDetalle() sobre el mismo ViewModel (que
    // sobrevive). Sin este removeSource cada rotación dejaría colgada una
    // fuente vieja con su registro en el InvalidationTracker de Room.
    private LiveData<AtencionOdontologiaConDetalle> fuenteDetalleActual;

    public DetalleAtencionOdontologiaViewModel(@NonNull Application application) {
        super(application);
        contexto = application.getApplicationContext();
        AppDatabase db = AppDatabase.getInstancia(contexto);
        pacienteDao = db.pacienteDao();
        atencionDao = db.atencionOdontologiaDao();
        tipoPrestacionDao = db.tipoPrestacionOdontologiaDao();
        diagnosticoDao = db.diagnosticoDao();

        AppExecutors.io().execute(() -> {
            List<TipoPrestacionOdontologiaEntity> tp = tipoPrestacionDao.listar();
            List<DiagnosticoEntity> dg = diagnosticoDao.listar();
            AppExecutors.ejecutarEnUI(() -> {
                tiposPrestacion.setValue(tp);
                diagnosticos.setValue(dg);
            });
        });
        puedeEditar.setValue(false);
    }

    public LiveData<PacienteEntity> observarPaciente(long pacienteLocalId) {
        return pacienteDao.observar(pacienteLocalId);
    }

    public LiveData<AtencionOdontologiaConDetalle> observarDetalle(long atencionLocalId) {
        LiveData<AtencionOdontologiaConDetalle> fuente = atencionDao.observarPorLocalId(atencionLocalId);

        if (fuenteDetalleActual != null) {
            puedeEditar.removeSource(fuenteDetalleActual);
        }
        fuenteDetalleActual = fuente;

        // La condición es doble: la ventana tiene que estar abierta y la atención
        // tiene que estar pendiente de sincronizar. Una ya sincronizada no se
        // edita aunque quedara tiempo, porque el servidor ya la tiene.
        puedeEditar.addSource(fuente, detalle -> {
            if (detalle == null || detalle.getAtencion() == null) {
                puedeEditar.setValue(false);
                return;
            }
            AtencionOdontologiaEntity atencion = detalle.getAtencion();
            puedeEditar.setValue(atencion.getSyncState() == SyncEstado.PENDIENTE
                    && VentanaEdicion.estaAbierta(atencion.getFechaRegistroLocal()));
        });
        return fuente;
    }

    public LiveData<Boolean> getPuedeEditar() {
        return puedeEditar;
    }

    public LiveData<List<TipoPrestacionOdontologiaEntity>> getTiposPrestacion() {
        return tiposPrestacion;
    }

    public LiveData<List<DiagnosticoEntity>> getDiagnosticos() {
        return diagnosticos;
    }

    public LiveData<AtencionOdontologiaDetalleResponse> getDetalleOnline() {
        return detalleOnline;
    }

    public LiveData<Boolean> getErrorOnline() {
        return errorOnline;
    }

    public void cargarOnline(int serverId) {
        String token = PreferenciasUsuario.getAuthHeader(contexto);
        ApiClient.getApiAtenciones().obtenerAtencionOdontologia(token, serverId)
                .enqueue(new Callback<AtencionOdontologiaDetalleResponse>() {
            @Override public void onResponse(Call<AtencionOdontologiaDetalleResponse> call,
                                             Response<AtencionOdontologiaDetalleResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    detalleOnline.setValue(response.body());
                } else {
                    errorOnline.setValue(true);
                }
            }
            @Override public void onFailure(Call<AtencionOdontologiaDetalleResponse> call, Throwable t) {
                errorOnline.setValue(true);
            }
        });
    }
}
