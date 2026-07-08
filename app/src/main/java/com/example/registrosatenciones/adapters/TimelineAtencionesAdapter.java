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
import com.example.registrosatenciones.db.entity.PrestacionEnfermeriaEntity;
import com.example.registrosatenciones.db.entity.TipoPrestacionEnfermeriaEntity;
import com.example.registrosatenciones.db.relation.AtencionConPrestaciones;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimelineAtencionesAdapter extends RecyclerView.Adapter<TimelineAtencionesAdapter.TimelineViewHolder> {

    private final Context context;
    private List<AtencionConPrestaciones> atenciones = new ArrayList<>();
    private Map<Integer, String> nombresPrestaciones = new HashMap<>();

    public TimelineAtencionesAdapter(Context context) {
        this.context = context;
    }

    public void setAtenciones(List<AtencionConPrestaciones> atenciones) {
        this.atenciones = atenciones != null ? atenciones : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setCatalogo(List<TipoPrestacionEnfermeriaEntity> catalogo) {
        Map<Integer, String> mapa = new HashMap<>();
        if (catalogo != null) {
            for (TipoPrestacionEnfermeriaEntity t : catalogo) {
                mapa.put(t.getId(), t.getNombrePrestacion());
            }
        }
        this.nombresPrestaciones = mapa;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TimelineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_atencion_timeline, parent, false);
        return new TimelineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimelineViewHolder holder, int position) {
        AtencionConPrestaciones item = atenciones.get(position);

        holder.tvFecha.setText(item.getAtencion().getFechaRegistroLocal());
        holder.tvTipoAtencion.setText(item.getAtencion().getTipoAtencion() == 1 ? "Ambulatorio" : "Internado");

        boolean pendiente = item.getAtencion().getSyncState() == SyncEstado.PENDIENTE;
        holder.chipPendiente.setVisibility(pendiente ? View.VISIBLE : View.GONE);

        holder.chipGroupPrestaciones.removeAllViews();
        for (PrestacionEnfermeriaEntity p : item.getPrestaciones()) {
            String nombre = nombresPrestaciones.getOrDefault(p.getTipoPrestacionId(), "Prestación #" + p.getTipoPrestacionId());
            Chip chip = new Chip(context);
            chip.setText(nombre + " ×" + p.getCantidad());
            chip.setClickable(false);
            chip.setCheckable(false);
            chip.setTextSize(10.5f);
            holder.chipGroupPrestaciones.addView(chip);
        }
    }

    @Override
    public int getItemCount() {
        return atenciones.size();
    }

    public static class TimelineViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvFecha;
        private final TextView chipPendiente;
        private final TextView tvTipoAtencion;
        private final ChipGroup chipGroupPrestaciones;

        public TimelineViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            chipPendiente = itemView.findViewById(R.id.chipPendiente);
            tvTipoAtencion = itemView.findViewById(R.id.tvTipoAtencion);
            chipGroupPrestaciones = itemView.findViewById(R.id.chipGroupPrestaciones);
        }
    }
}