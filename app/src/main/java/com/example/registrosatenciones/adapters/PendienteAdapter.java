package com.example.registrosatenciones.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.registrosatenciones.R;
import com.example.registrosatenciones.ui.sincronizacion.ItemPendiente;

import java.util.ArrayList;
import java.util.List;

public class PendienteAdapter extends RecyclerView.Adapter<PendienteAdapter.PendienteViewHolder> {

    private final Context context;
    private List<ItemPendiente> items = new ArrayList<>();

    public PendienteAdapter(Context context) {
        this.context = context;
    }

    public void setItems(List<ItemPendiente> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PendienteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_pendiente, parent, false);
        return new PendienteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PendienteViewHolder holder, int position) {
        ItemPendiente item = items.get(position);
        holder.tvTitulo.setText(item.getTitulo());
        holder.tvSubtitulo.setText(item.getSubtitulo());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class PendienteViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitulo;
        private final TextView tvSubtitulo;

        public PendienteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvSubtitulo = itemView.findViewById(R.id.tvSubtitulo);
        }
    }
}
