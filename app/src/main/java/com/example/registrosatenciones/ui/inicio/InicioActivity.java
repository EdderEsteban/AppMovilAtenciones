package com.example.registrosatenciones.ui.inicio;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.example.registrosatenciones.R;
import com.example.registrosatenciones.adapters.RankingAdapter;
import com.example.registrosatenciones.databinding.ActivityInicioBinding;
import com.example.registrosatenciones.response.DashboardResponse;
import com.example.registrosatenciones.ui.common.NavegacionInferiorActivity;
import com.example.registrosatenciones.ui.dashboard.DashboardViewModel;
import com.example.registrosatenciones.ui.pacientes.PacientesActivity;
import com.example.registrosatenciones.util.PreferenciasUsuario;

import java.util.List;

public class InicioActivity extends NavegacionInferiorActivity {

    private ActivityInicioBinding binding;
    private DashboardViewModel viewModel;
    private RankingAdapter rankingAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInicioBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.tvSaludo.setText("Hola, " + primerNombre(PreferenciasUsuario.getNombreCompleto(this)));
        binding.tvInstitucion.setText(PreferenciasUsuario.getInstitucionActivaNombre(this));

        rankingAdapter = new RankingAdapter(this);
        binding.rvTop10.setLayoutManager(new LinearLayoutManager(this));
        binding.rvTop10.setAdapter(rankingAdapter);

        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        viewModel.getDashboard().observe(this, this::renderizarDashboard);
        viewModel.getCargando().observe(this, cargando ->
                binding.progressDashboard.setVisibility(cargando ? View.VISIBLE : View.GONE));

        viewModel.getSinConexion().observe(this, sinConexion -> {
            boolean sin = Boolean.TRUE.equals(sinConexion);
            binding.tvSinConexionDashboard.setVisibility(sin ? View.VISIBLE : View.GONE);
            binding.scrollDashboard.setVisibility(sin ? View.GONE : View.VISIBLE);
        });
        viewModel.cargar();

        configurarNavInferior();

        binding.fabNuevaAtencion.setOnClickListener(v ->
                startActivity(new Intent(this, PacientesActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.cargar();
        viewModel.sincronizarVencidas();
    }

    @Override
    protected BottomNavigationView getBottomNavigationView() {
        return binding.bottomNav;
    }

    @Override
    protected int getTabActual() {
        return R.id.nav_inicio;
    }

    private void renderizarDashboard(DashboardResponse d) {
        if (d == null) return;

        binding.tvTituloBarras.setText(d.getTituloBarras());
        binding.tvTituloDonut.setText(d.getTituloDonut());
        binding.tvTituloLinea.setText(d.getTituloLinea());
        binding.tvTituloTop10.setText(d.getTituloTop10());

        agregarBarras(binding.containerBarras7Dias, binding.containerLabels7Dias, d.getSerie7Dias(), d.getLabels7Dias());
        agregarBarras(binding.containerBarras6Meses, binding.containerLabels6Meses, d.getSerie6Meses(), d.getLabels6Meses());
        agregarBarraProporcion(binding.barraDonut, binding.leyendaDonut, d.getDonutLabels(), d.getDonutValores());

        rankingAdapter.setDatos(d.getTop10Nombres(), d.getTop10Cantidades());
    }

    private void agregarBarras(LinearLayout contenedorBarras, LinearLayout contenedorLabels,
                                List<Integer> valores, List<String> labels) {
        contenedorBarras.removeAllViews();
        contenedorLabels.removeAllViews();
        if (valores == null || valores.isEmpty()) return;

        int maximo = 1;
        for (Integer v : valores) {
            if (v != null && v > maximo) maximo = v;
        }

        for (int i = 0; i < valores.size(); i++) {
            int valor = valores.get(i) != null ? valores.get(i) : 0;
            int alturaPorcentaje = Math.max(4, (valor * 100) / maximo);

            LinearLayout columna = new LinearLayout(this);
            columna.setOrientation(LinearLayout.VERTICAL);
            columna.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));

            View espacio = new View(this);
            espacio.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 100 - alturaPorcentaje));

            View barra = new View(this);
            LinearLayout.LayoutParams paramsBarra = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, alturaPorcentaje);
            paramsBarra.setMarginStart(dpAPx(3));
            paramsBarra.setMarginEnd(dpAPx(3));
            barra.setLayoutParams(paramsBarra);
            barra.setBackgroundColor(getColor(R.color.color_brand));

            columna.addView(espacio);
            columna.addView(barra);
            contenedorBarras.addView(columna);

            TextView label = new TextView(this);
            label.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            label.setGravity(Gravity.CENTER);
            label.setText(labels != null && i < labels.size() ? labels.get(i) : "");
            label.setTextSize(9f);
            label.setTextColor(getColor(R.color.color_muted));
            contenedorLabels.addView(label);
        }
    }

    private void agregarBarraProporcion(LinearLayout contenedor, LinearLayout leyenda,
                                         List<String> etiquetas, List<Integer> valores) {
        contenedor.removeAllViews();
        leyenda.removeAllViews();
        if (valores == null || valores.isEmpty()) return;

        int[] coloresRes = { R.color.color_brand, R.color.color_accent };

        for (int i = 0; i < valores.size(); i++) {
            int valor = valores.get(i) != null ? valores.get(i) : 0;
            int colorRes = coloresRes[i % coloresRes.length];

            LinearLayout.LayoutParams paramsSegmento = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.MATCH_PARENT, Math.max(valor, 1));
            View segmento = new View(this);
            segmento.setLayoutParams(paramsSegmento);
            segmento.setBackgroundColor(getColor(colorRes));
            contenedor.addView(segmento);

            LinearLayout fila = new LinearLayout(this);
            fila.setOrientation(LinearLayout.HORIZONTAL);
            fila.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams paramsFila = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            paramsFila.topMargin = dpAPx(6);
            fila.setLayoutParams(paramsFila);

            View colorBox = new View(this);
            LinearLayout.LayoutParams paramsBox = new LinearLayout.LayoutParams(dpAPx(11), dpAPx(11));
            paramsBox.setMarginEnd(dpAPx(8));
            colorBox.setLayoutParams(paramsBox);
            colorBox.setBackgroundColor(getColor(colorRes));
            fila.addView(colorBox);

            TextView tvEtiqueta = new TextView(this);
            tvEtiqueta.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            tvEtiqueta.setText(etiquetas != null && i < etiquetas.size() ? etiquetas.get(i) : "");
            tvEtiqueta.setTextSize(12.5f);
            tvEtiqueta.setTextColor(getColor(R.color.color_muted));
            fila.addView(tvEtiqueta);

            TextView tvValor = new TextView(this);
            tvValor.setText(String.valueOf(valor));
            tvValor.setTextSize(12.5f);
            tvValor.setTypeface(tvValor.getTypeface(), Typeface.BOLD);
            fila.addView(tvValor);

            leyenda.addView(fila);
        }
    }

    private int dpAPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private String primerNombre(String nombreCompleto) {
        if (nombreCompleto == null || nombreCompleto.isEmpty()) return "";
        return nombreCompleto.split(" ")[0];
    }
}
