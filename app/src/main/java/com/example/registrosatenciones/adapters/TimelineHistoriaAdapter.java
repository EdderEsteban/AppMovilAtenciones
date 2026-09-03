package com.example.registrosatenciones.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.registrosatenciones.R;
import com.example.registrosatenciones.ui.historiaclinica.ItemHistoria;
import com.example.registrosatenciones.util.VentanaEdicion;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

public class TimelineHistoriaAdapter extends RecyclerView.Adapter<TimelineHistoriaAdapter.VH> {

    public interface OnItemClickListener {
        void onClick(ItemHistoria item);
    }

    private final Context context;
    private final OnItemClickListener listener;
    private List<ItemHistoria> items = new ArrayList<>();

    public TimelineHistoriaAdapter(Context context, OnItemClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setItems(List<ItemHistoria> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_atencion_timeline_historia, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ItemHistoria item = items.get(position);

        h.tvRama.setText(item.esOdontologia() ? "Odontología" : "Enfermería");
        h.tvFecha.setText(item.getFecha());
        h.tvResumen.setText(item.getResumen());
        boolean pendiente = item.getFuente() == ItemHistoria.Fuente.LOCAL;
        h.chipPendiente.setVisibility(pendiente ? View.VISIBLE : View.GONE);
        if (pendiente) {
            String restante = VentanaEdicion.formatearRestante(item.getFecha());
            h.chipPendiente.setText(restante != null ? "Editable — " + restante : "Pendiente");
        }

        h.chipGroupPrestaciones.removeAllViews();
        for (String p : item.getPrestaciones()) {
            Chip chip = new Chip(context);
            chip.setText(p);
            chip.setClickable(false);
            chip.setCheckable(false);
            chip.setTextSize(10.5f);
            h.chipGroupPrestaciones.addView(chip);
        }

        h.card.setOnClickListener(v -> listener.onClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class VH extends RecyclerView.ViewHolder {
        private final TextView tvRama;
        private final TextView tvFecha;
        private final TextView tvResumen;
        private final TextView chipPendiente;
        private final ChipGroup chipGroupPrestaciones;
        private final MaterialCardView card;

        public VH(@NonNull View itemView) {
            super(itemView);
            tvRama = itemView.findViewById(R.id.tvRama);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            tvResumen = itemView.findViewById(R.id.tvResumen);
            chipPendiente = itemView.findViewById(R.id.chipPendiente);
            chipGroupPrestaciones = itemView.findViewById(R.id.chipGroupPrestaciones);
            card = itemView.findViewById(R.id.cardAtencion);
        }
    }
}
