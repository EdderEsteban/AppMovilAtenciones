package com.example.registrosatenciones.ui.odontologia;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import com.example.registrosatenciones.databinding.ActivityEditorCuadranteBinding;
import com.example.registrosatenciones.databinding.SheetEstadosOdontogramaBinding;
import com.example.registrosatenciones.ui.odontologia.model.DientesFdi;
import com.example.registrosatenciones.ui.odontologia.model.OdontogramaItem;
import com.example.registrosatenciones.ui.odontologia.model.OdontogramaModelo;

import java.util.ArrayList;

public class EditorCuadranteActivity extends AppCompatActivity {

    public static final String EXTRA_CUADRANTE = "cuadrante";
    public static final String EXTRA_ESTADOS = "estados";

    private ActivityEditorCuadranteBinding binding;
    private OdontogramaModelo modelo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditorCuadranteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        int cuadrante = getIntent().getIntExtra(EXTRA_CUADRANTE, 1);
        @SuppressWarnings("unchecked")
        ArrayList<OdontogramaItem> estadosEntrada = (ArrayList<OdontogramaItem>) getIntent().getSerializableExtra(EXTRA_ESTADOS);

        modelo = new OdontogramaModelo();
        modelo.cargar(estadosEntrada);

        binding.tvTituloCuadrante.setText(tituloDeCuadrante(cuadrante));

        binding.odontogramaCuadrante.setModelo(modelo);
        binding.odontogramaCuadrante.setDientes(
                DientesFdi.permanentesDeCuadrante(cuadrante),
                DientesFdi.temporariosDeCuadrante(cuadrante));
        binding.odontogramaCuadrante.setEditable(true);
        binding.odontogramaCuadrante.setOnSuperficieTocadaListener(this::mostrarHojaEstados);

        binding.btnListo.setOnClickListener(v -> {
            Intent resultado = new Intent();
            resultado.putExtra(EXTRA_ESTADOS, new ArrayList<>(modelo.aplanar()));
            setResult(RESULT_OK, resultado);
            finish();
        });
    }

    private void mostrarHojaEstados(int diente, String superficie) {
        binding.odontogramaCuadrante.marcarActivo(diente);

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        SheetEstadosOdontogramaBinding sheet = SheetEstadosOdontogramaBinding.inflate(getLayoutInflater());
        dialog.setContentView(sheet.getRoot());
        dialog.setOnDismissListener(d -> binding.odontogramaCuadrante.limpiarActivo());

        String etiquetaSuperficie = "*".equals(superficie) ? "diente completo" : "superficie " + superficie;
        sheet.tvSheetTitulo.setText("Diente " + diente + " · " + etiquetaSuperficie);

        sheet.btnEstadoSano.setOnClickListener(v -> aplicarYcerrar(dialog, diente, superficie, OdontogramaModelo.SANO));
        sheet.btnEstadoCaries.setOnClickListener(v -> aplicarYcerrar(dialog, diente, superficie, OdontogramaModelo.CARIES));
        sheet.btnEstadoObturado.setOnClickListener(v -> aplicarYcerrar(dialog, diente, superficie, OdontogramaModelo.OBTURADO));
        sheet.btnEstadoAusente.setOnClickListener(v -> aplicarYcerrar(dialog, diente, superficie, OdontogramaModelo.AUSENTE));
        sheet.btnEstadoExtraccion.setOnClickListener(v -> aplicarYcerrar(dialog, diente, superficie, OdontogramaModelo.EXTRACCION));
        sheet.btnEstadoCorona.setOnClickListener(v -> aplicarYcerrar(dialog, diente, superficie, OdontogramaModelo.CORONA));

        dialog.show();
    }

    private void aplicarYcerrar(BottomSheetDialog dialog, int diente, String superficie, int estado) {
        modelo.aplicar(diente, superficie, estado);
        binding.odontogramaCuadrante.redibujar();
        dialog.dismiss();
    }

    private String tituloDeCuadrante(int cuadrante) {
        switch (cuadrante) {
            case 1: return "Cuadrante superior derecho";
            case 2: return "Cuadrante superior izquierdo";
            case 3: return "Cuadrante inferior izquierdo";
            case 4: return "Cuadrante inferior derecho";
            default: return "Cuadrante";
        }
    }
}
