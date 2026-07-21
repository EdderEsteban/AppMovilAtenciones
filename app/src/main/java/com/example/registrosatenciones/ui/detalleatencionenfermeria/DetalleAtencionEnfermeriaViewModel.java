package com.example.registrosatenciones.ui.detalleatencionenfermeria;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.registrosatenciones.db.AppDatabase;
import com.example.registrosatenciones.db.dao.AtencionEnfermeriaDao;
import com.example.registrosatenciones.db.dao.PacienteDao;
import com.example.registrosatenciones.db.dao.TipoPrestacionEnfermeriaDao;
import com.example.registrosatenciones.db.entity.PacienteEntity;
import com.example.registrosatenciones.db.entity.TipoPrestacionEnfermeriaEntity;
import com.example.registrosatenciones.db.relation.AtencionConPrestaciones;
import com.example.registrosatenciones.request.ApiClient;
import com.example.registrosatenciones.response.AtencionEnfermeriaDetalleResponse;
import com.example.registrosatenciones.util.PreferenciasUsuario;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetalleAtencionEnfermeriaViewModel extends AndroidViewModel {

    private final Context contexto;
    private final PacienteDao pacienteDao;
    private final AtencionEnfermeriaDao atencionDao;
    private final TipoPrestacionEnfermeriaDao tipoPrestacionDao;

    private final MutableLiveData<AtencionEnfermeriaDetalleResponse> detalleOnline = new MutableLiveData<>();
    private final MutableLiveData<Boolean> errorOnline = new MutableLiveData<>();

    public DetalleAtencionEnfermeriaViewModel(@NonNull Application application) {
        super(application);
        contexto = application.getApplicationContext();
        AppDatabase db = AppDatabase.getInstancia(contexto);
        pacienteDao = db.pacienteDao();
        atencionDao = db.atencionEnfermeriaDao();
        tipoPrestacionDao = db.tipoPrestacionEnfermeriaDao();
    }

    public LiveData<PacienteEntity> observarPaciente(long pacienteLocalId) {
        return pacienteDao.observar(pacienteLocalId);
    }

    public LiveData<AtencionConPrestaciones> observarDetalle(long atencionLocalId) {
        return atencionDao.observarPorLocalId(atencionLocalId);
    }

    public LiveData<List<TipoPrestacionEnfermeriaEntity>> observarCatalogo() {
        return tipoPrestacionDao.observarCatalogo();
    }

    public LiveData<AtencionEnfermeriaDetalleResponse> getDetalleOnline() {
        return detalleOnline;
    }

    public LiveData<Boolean> getErrorOnline() {
        return errorOnline;
    }

    public void cargarOnline(int serverId) {
        String token = PreferenciasUsuario.getAuthHeader(contexto);
        ApiClient.getApiAtenciones().obtenerAtencionEnfermeria(token, serverId)
                .enqueue(new Callback<AtencionEnfermeriaDetalleResponse>() {
            @Override public void onResponse(Call<AtencionEnfermeriaDetalleResponse> call,
                                             Response<AtencionEnfermeriaDetalleResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    detalleOnline.setValue(response.body());
                } else {
                    errorOnline.setValue(true);
                }
            }
            @Override public void onFailure(Call<AtencionEnfermeriaDetalleResponse> call, Throwable t) {
                errorOnline.setValue(true);
            }
        });
    }
}
