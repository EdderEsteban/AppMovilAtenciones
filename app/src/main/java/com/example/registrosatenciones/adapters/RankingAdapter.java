package com.example.registrosatenciones.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.registrosatenciones.R;

import java.util.ArrayList;
import java.util.List;

public class RankingAdapter extends RecyclerView.Adapter<RankingAdapter.RankingViewHolder> {

    private final Context context;
    private List<String> nombres = new ArrayList<>();
    private List<Integer> cantidades = new ArrayList<>();
    private int maximo = 1;

    public RankingAdapter(Context context) {
        this.context = context;
    }

    public void setDatos(List<String> nombres, List<Integer> cantidades) {
        this.nombres = nombres != null ? nombres : new ArrayList<>();
        this.cantidades = cantidades != null ? cantidades : new ArrayList<>();
        this.maximo = 1;
        for (Integer c : this.cantidades) {
            if (c != null && c > maximo) maximo = c;
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RankingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_ranking, parent, false);
        return new RankingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RankingViewHolder holder, int position) {
        String nombre = nombres.get(position);
        int cantidad = position < cantidades.size() ? cantidades.get(position) : 0;

        holder.tvNombre.setText(nombre);
        holder.tvCantidad.setText(String.valueOf(cantidad));

        LinearLayout.LayoutParams paramsBarra = (LinearLayout.LayoutParams) holder.barraRanking.getLayoutParams();
        paramsBarra.weight = cantidad;
        holder.barraRanking.setLayoutParams(paramsBarra);

        LinearLayout.LayoutParams paramsEspacio = (LinearLayout.LayoutParams) holder.espacioRanking.getLayoutParams();
        paramsEspacio.weight = maximo - cantidad;
        holder.espacioRanking.setLayoutParams(paramsEspacio);
    }

    @Override
    public int getItemCount() {
        return nombres.size();
    }

    public static class RankingViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvNombre;
        private final TextView tvCantidad;
        private final View barraRanking;
        private final View espacioRanking;

        public RankingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            tvCantidad = itemView.findViewById(R.id.tvCantidad);
            barraRanking = itemView.findViewById(R.id.barraRanking);
            espacioRanking = itemView.findViewById(R.id.espacioRanking);
        }
    }
}
