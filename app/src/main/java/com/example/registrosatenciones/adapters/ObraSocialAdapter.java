package com.example.registrosatenciones.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.registrosatenciones.R;
import com.example.registrosatenciones.db.entity.ObraSocialEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ObraSocialAdapter extends RecyclerView.Adapter<ObraSocialAdapter.ObraSocialViewHolder> {

    public interface OnObraSocialClickListener {
        void onClick(ObraSocialEntity obraSocial);
    }

    private final Context context;
    private final OnObraSocialClickListener listener;
    private List<ObraSocialEntity> todos = new ArrayList<>();
    private List<ObraSocialEntity> filtrados = new ArrayList<>();

    public ObraSocialAdapter(Context context, OnObraSocialClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setObrasSociales(List<ObraSocialEntity> obrasSociales) {
        this.todos = obrasSociales != null ? obrasSociales : new ArrayList<>();
        this.filtrados = new ArrayList<>(this.todos);
        notifyDataSetChanged();
    }

    // Los nombres traen la sigla entre paréntesis al principio (p. ej. "(OSDE)
    // OBRA SOCIAL DE EJECUTIVOS..."), así que alcanza con buscar la subcadena
    // en el nombre completo, sin distinguir mayúsculas.
    public void filtrar(String query) {
        filtrados.clear();
        if (query == null || query.trim().isEmpty()) {
            filtrados.addAll(todos);
        } else {
            String q = query.trim().toLowerCase(Locale.ROOT);
            for (ObraSocialEntity o : todos) {
                if (o.getNombre().toLowerCase(Locale.ROOT).contains(q)) {
                    filtrados.add(o);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ObraSocialViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_obra_social, parent, false);
        return new ObraSocialViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ObraSocialViewHolder holder, int position) {
        ObraSocialEntity obraSocial = filtrados.get(position);
        holder.tvNombre.setText(obraSocial.getNombre());
        holder.itemView.setOnClickListener(v -> listener.onClick(obraSocial));
    }

    @Override
    public int getItemCount() {
        return filtrados.size();
    }

    public static class ObraSocialViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvNombre;

        public ObraSocialViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombreObraSocial);
        }
    }
}
