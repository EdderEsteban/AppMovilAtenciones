package com.example.registrosatenciones.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.registrosatenciones.R;
import com.example.registrosatenciones.db.entity.DiagnosticoEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DiagnosticoAdapter extends RecyclerView.Adapter<DiagnosticoAdapter.DiagnosticoViewHolder> {

    public interface OnDiagnosticoClickListener {
        void onClick(DiagnosticoEntity diagnostico);
    }

    private final Context context;
    private final OnDiagnosticoClickListener listener;
    private List<DiagnosticoEntity> todos = new ArrayList<>();
    private List<DiagnosticoEntity> filtrados = new ArrayList<>();

    public DiagnosticoAdapter(Context context, OnDiagnosticoClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setDiagnosticos(List<DiagnosticoEntity> diagnosticos) {
        this.todos = diagnosticos != null ? diagnosticos : new ArrayList<>();
        this.filtrados = new ArrayList<>(this.todos);
        notifyDataSetChanged();
    }

    public void filtrar(String query) {
        filtrados.clear();
        if (query == null || query.trim().isEmpty()) {
            filtrados.addAll(todos);
        } else {
            String q = query.trim().toLowerCase(Locale.ROOT);
            for (DiagnosticoEntity d : todos) {
                if (d.getCodigo().toLowerCase(Locale.ROOT).contains(q)
                        || d.getDescripcion().toLowerCase(Locale.ROOT).contains(q)) {
                    filtrados.add(d);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DiagnosticoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_diagnostico, parent, false);
        return new DiagnosticoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DiagnosticoViewHolder holder, int position) {
        DiagnosticoEntity diagnostico = filtrados.get(position);
        holder.tvCodigo.setText(diagnostico.getCodigo());
        holder.tvDescripcion.setText(diagnostico.getDescripcion());
        holder.itemView.setOnClickListener(v -> listener.onClick(diagnostico));
    }

    @Override
    public int getItemCount() {
        return filtrados.size();
    }

    public static class DiagnosticoViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvCodigo;
        private final TextView tvDescripcion;

        public DiagnosticoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCodigo = itemView.findViewById(R.id.tvCodigoDiagnostico);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcionDiagnostico);
        }
    }
}
