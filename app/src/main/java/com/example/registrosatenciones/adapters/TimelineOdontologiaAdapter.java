package com.example.registrosatenciones.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.registrosatenciones.R;
import com.example.registrosatenciones.db.SyncEstado;
import com.example.registrosatenciones.db.entity.DiagnosticoEntity;
import com.example.registrosatenciones.db.entity.PrestacionOdontologiaEntity;
import com.example.registrosatenciones.db.entity.TipoPrestacionOdontologiaEntity;
import com.example.registrosatenciones.db.relation.AtencionOdontologiaConDetalle;
import com.example.registrosatenciones.util.VentanaEdicion;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimelineOdontologiaAdapter extends RecyclerView.Adapter<TimelineOdontologiaAdapter.TimelineViewHolder> {

    public interface OnAtencionClickListener {
        void onClick(AtencionOdontologiaConDetalle atencion);
    }

    private final Context context;
    private final OnAtencionClickListener listener;
    private List<AtencionOdontologiaConDetalle> atenciones = new ArrayList<>();
    private Map<Integer, String> nombresPrestaciones = new HashMap<>();
    private Map<Integer, String> nombresDiagnosticos = new HashMap<>();

    public TimelineOdontologiaAdapter(Context context, OnAtencionClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setAtenciones(List<AtencionOdontologiaConDetalle> atenciones) {
        this.atenciones = atenciones != null ? atenciones : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setCatalogoPrestaciones(List<TipoPrestacionOdontologiaEntity> catalogo) {
        Map<Integer, String> mapa = new HashMap<>();
        if (catalogo != null) {
            for (TipoPrestacionOdontologiaEntity t : catalogo) {
                mapa.put(t.getId(), t.getNombre());
            }
        }
        this.nombresPrestaciones = mapa;
        notifyDataSetChanged();
    }

    public void setDiagnosticos(List<DiagnosticoEntity> diagnosticos) {
        Map<Integer, String> mapa = new HashMap<>();
        if (diagnosticos != null) {
            for (DiagnosticoEntity d : diagnosticos) {
                mapa.put(d.getId(), d.getCodigo() + " — " + d.getDescripcion());
            }
        }
        this.nombresDiagnosticos = mapa;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TimelineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_atencion_timeline_odontologia, parent, false);
        return new TimelineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimelineViewHolder holder, int position) {
        AtencionOdontologiaConDetalle item = atenciones.get(position);

        holder.tvFecha.setText(item.getAtencion().getFechaRegistroLocal());
        holder.tvTipoConsulta.setText((item.getAtencion().getTipoConsulta() == 1 ? "1ª vez" : "Ulterior"));

        String diagnostico = nombresDiagnosticos.get(item.getAtencion().getDiagnosticoId());
        holder.tvDiagnostico.setText(diagnostico != null ? diagnostico : "Diagnóstico #" + item.getAtencion().getDiagnosticoId());

        boolean pendiente = item.getAtencion().getSyncState() == SyncEstado.PENDIENTE;
        holder.chipPendiente.setVisibility(pendiente ? View.VISIBLE : View.GONE);
        String restante = VentanaEdicion.formatearRestante(item.getAtencion().getFechaRegistroLocal());
        holder.chipPendiente.setText(restante != null ? "Editable — " + restante : "Pendiente");

        holder.chipGroupPrestaciones.removeAllViews();
        for (PrestacionOdontologiaEntity p : item.getPrestaciones()) {
            String nombre = nombresPrestaciones.getOrDefault(p.getTipoPrestacionId(), "Prestación #" + p.getTipoPrestacionId());
            Chip chip = new Chip(context);
            chip.setText(nombre + " ×" + p.getCantidad());
            chip.setClickable(false);
            chip.setCheckable(false);
            chip.setTextSize(10.5f);
            holder.chipGroupPrestaciones.addView(chip);
        }

        holder.cardAtencion.setOnClickListener(v -> listener.onClick(item));
    }

    @Override
    public int getItemCount() {
        return atenciones.size();
    }

    public static class TimelineViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvFecha;
        private final TextView chipPendiente;
        private final TextView tvTipoConsulta;
        private final TextView tvDiagnostico;
        private final ChipGroup chipGroupPrestaciones;
        private final MaterialCardView cardAtencion;

        public TimelineViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            chipPendiente = itemView.findViewById(R.id.chipPendiente);
            tvTipoConsulta = itemView.findViewById(R.id.tvTipoConsulta);
            tvDiagnostico = itemView.findViewById(R.id.tvDiagnostico);
            chipGroupPrestaciones = itemView.findViewById(R.id.chipGroupPrestaciones);
            cardAtencion = itemView.findViewById(R.id.cardAtencion);
        }
    }
}
