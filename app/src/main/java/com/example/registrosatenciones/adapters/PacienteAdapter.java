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
import com.example.registrosatenciones.db.entity.PacienteEntity;

import java.util.ArrayList;
import java.util.List;

public class PacienteAdapter extends RecyclerView.Adapter<PacienteAdapter.PacienteViewHolder> {

    public interface OnPacienteClickListener {
        void onClick(PacienteEntity paciente);
    }

    private final Context context;
    private final OnPacienteClickListener listener;
    private List<PacienteEntity> pacientes = new ArrayList<>();

    public PacienteAdapter(Context context, OnPacienteClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setPacientes(List<PacienteEntity> pacientes) {
        this.pacientes = pacientes != null ? pacientes : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PacienteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_paciente, parent, false);
        return new PacienteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PacienteViewHolder holder, int position) {
        PacienteEntity paciente = pacientes.get(position);

        String iniciales = obtenerIniciales(paciente.getApellido(), paciente.getNombre());
        holder.tvAvatar.setText(iniciales);

        holder.tvNombrePaciente.setText(paciente.getApellido() + ", " + paciente.getNombre());

        StringBuilder detalle = new StringBuilder("DNI ").append(paciente.getDni());
        if (paciente.getEdad() != null) {
            detalle.append(" · ").append(paciente.getEdad()).append(" a");
        }
        if (paciente.getSexo() != null) {
            detalle.append(" · ").append(paciente.getSexo());
        }
        holder.tvDetallePaciente.setText(detalle.toString());

        boolean pendiente = paciente.getSyncState() == SyncEstado.PENDIENTE;
        holder.chipNuevo.setVisibility(pendiente ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> listener.onClick(paciente));
    }

    @Override
    public int getItemCount() {
        return pacientes.size();
    }

    private String obtenerIniciales(String apellido, String nombre) {
        String a = apellido != null && !apellido.isEmpty() ? apellido.substring(0, 1) : "";
        String n = nombre != null && !nombre.isEmpty() ? nombre.substring(0, 1) : "";
        return (a + n).toUpperCase();
    }

    public static class PacienteViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvAvatar;
        private final TextView tvNombrePaciente;
        private final TextView tvDetallePaciente;
        private final TextView chipNuevo;

        public PacienteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tvAvatar);
            tvNombrePaciente = itemView.findViewById(R.id.tvNombrePaciente);
            tvDetallePaciente = itemView.findViewById(R.id.tvDetallePaciente);
            chipNuevo = itemView.findViewById(R.id.chipNuevo);
        }
    }
}