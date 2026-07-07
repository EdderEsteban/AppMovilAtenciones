package com.example.registrosatenciones.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.registrosatenciones.R;
import com.example.registrosatenciones.response.InstitucionResponse;

import java.util.ArrayList;
import java.util.List;

public class InstitucionAdapter extends RecyclerView.Adapter<InstitucionAdapter.InstitucionViewHolder> {

    public interface OnInstitucionClickListener {
        void onClick(int institucionId);
    }

    private final Context context;
    private final OnInstitucionClickListener listener;
    private List<InstitucionResponse> instituciones = new ArrayList<>();
    private Integer seleccionadaId;

    public InstitucionAdapter(Context context, OnInstitucionClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setInstituciones(List<InstitucionResponse> instituciones) {
        this.instituciones = instituciones != null ? instituciones : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setSeleccionada(Integer institucionId) {
        this.seleccionadaId = institucionId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public InstitucionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_institucion, parent, false);
        return new InstitucionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InstitucionViewHolder holder, int position) {
        InstitucionResponse institucion = instituciones.get(position);

        holder.tvNombre.setText(institucion.getNombre());
        boolean seleccionada = seleccionadaId != null && seleccionadaId == institucion.getId();
        holder.radio.setChecked(seleccionada);

        holder.itemView.setOnClickListener(v -> listener.onClick(institucion.getId()));
    }

    @Override
    public int getItemCount() {
        return instituciones.size();
    }

    public static class InstitucionViewHolder extends RecyclerView.ViewHolder {
        private final RadioButton radio;
        private final TextView tvNombre;

        public InstitucionViewHolder(@NonNull View itemView) {
            super(itemView);
            radio = itemView.findViewById(R.id.radioInstitucion);
            tvNombre = itemView.findViewById(R.id.tvNombreInstitucion);
        }
    }
}