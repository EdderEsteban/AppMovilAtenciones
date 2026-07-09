package com.example.registrosatenciones.ui.odontologia.view;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.example.registrosatenciones.ui.odontologia.model.OdontogramaModelo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dibuja el odontograma FDI reutilizando las mismas coordenadas de polígono (0-44)
 * y los mismos colores que _Odontograma.cshtml / odontograma.css del panel .NET.
 */
public class OdontogramaView extends View {

    public interface OnSuperficieTocadaListener {
        void onSuperficieTocada(int diente, String superficie);
    }

    // V, D, L, M, O — puntos (x,y) en el sistema 0-44, calcado del SVG web.
    private static final float[][] POLIGONOS = {
            {0, 0, 44, 0, 33, 11, 11, 11},   // V
            {44, 0, 44, 44, 33, 33, 33, 11}, // D
            {44, 44, 0, 44, 11, 33, 33, 33}, // L
            {0, 44, 0, 0, 11, 11, 11, 33},   // M
            {11, 11, 33, 11, 33, 33, 11, 33} // O
    };
    private static final String[] SUPERFICIES = {"V", "D", "L", "M", "O"};

    private static final int COLOR_SUPERFICIE_CLARO = Color.parseColor("#FFFFFF");
    private static final int COLOR_BORDE_CLARO = Color.parseColor("#374151");
    private static final int COLOR_SUPERFICIE_OSCURO = Color.parseColor("#F1F5F9");
    private static final int COLOR_BORDE_OSCURO = Color.parseColor("#6B7280");
    private static final int COLOR_CARIES = Color.parseColor("#EF4444");
    private static final int COLOR_OBTURADO = Color.parseColor("#3B82F6");
    private static final int COLOR_CORONA = Color.parseColor("#F59E0B");
    private static final int COLOR_AUSENTE = Color.parseColor("#9CA3AF");
    private static final int COLOR_EXTRACCION_FONDO = Color.parseColor("#D1D5DB");
    private static final int COLOR_X_EXTRACCION = Color.parseColor("#DC2626");
    private static final int COLOR_ACTIVO = Color.parseColor("#8B5CF6");
    private static final int ALPHA_BLOQUEADO = 107; // ~0.42 * 255, calcado de .diente-bloqueado

    private static final float GAP_DP = 4f;
    private static final float LABEL_ALTO_DP = 12f;
    private static final float FILA_GAP_DP = 18f;
    private static final float CELDA_MINIMA_DP = 30f;

    private OdontogramaModelo modelo = new OdontogramaModelo();
    private List<int[]> filasDientes = new ArrayList<>();
    private boolean editable = false;
    private int dienteActivo = -1;

    private final Paint paintRelleno = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintBorde = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintTexto = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintX = new Paint(Paint.ANTI_ALIAS_FLAG);

    private OnSuperficieTocadaListener listener;

    private final List<Fila> filas = new ArrayList<>();
    private float cellSize;

    private static class Fila {
        int[] dientes;
        float topY;    // incluye la etiqueta del número
        float cellTop; // donde empieza el cuadrado del diente
    }

    public OdontogramaView(Context context) {
        super(context);
        init();
    }

    public OdontogramaView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OdontogramaView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paintBorde.setStyle(Paint.Style.STROKE);
        paintBorde.setStrokeWidth(dp(1));
        paintTexto.setTextAlign(Paint.Align.CENTER);
        paintTexto.setTextSize(dp(9));
        paintX.setStrokeWidth(dp(2));
        paintX.setStrokeCap(Paint.Cap.ROUND);
        paintX.setColor(COLOR_X_EXTRACCION);
    }

    public void setModelo(OdontogramaModelo modelo) {
        this.modelo = modelo;
        invalidate();
    }

    // Modo cuadrante: 2 filas (temporarios arriba, permanentes abajo — igual que la web).
    public void setDientes(int[] permanentes, int[] temporarios) {
        List<int[]> filas = new ArrayList<>();
        if (temporarios != null && temporarios.length > 0) filas.add(temporarios);
        if (permanentes != null && permanentes.length > 0) filas.add(permanentes);
        setFilas(filas);
    }

    // Modo resumen/histórico: N filas arbitrarias (odontograma completo = 4 filas anatómicas).
    public void setFilas(List<int[]> filas) {
        this.filasDientes = filas != null ? filas : new ArrayList<>();
        requestLayout();
        invalidate();
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
        invalidate();
    }

    public void setOnSuperficieTocadaListener(OnSuperficieTocadaListener listener) {
        this.listener = listener;
    }

    public void marcarActivo(int diente) {
        this.dienteActivo = diente;
        invalidate();
    }

    public void limpiarActivo() {
        this.dienteActivo = -1;
        invalidate();
    }

    public void redibujar() {
        invalidate();
    }

    private boolean esOscuro() {
        int modo = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return modo == Configuration.UI_MODE_NIGHT_YES;
    }

    private float dp(float valor) {
        return valor * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int anchoDisponible = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();

        int maxDientesPorFila = 0;
        for (int[] fila : filasDientes) {
            maxDientesPorFila = Math.max(maxDientesPorFila, fila.length);
        }
        float celda = maxDientesPorFila > 0
                ? (anchoDisponible - dp(GAP_DP) * (maxDientesPorFila - 1)) / maxDientesPorFila
                : dp(CELDA_MINIMA_DP);
        celda = Math.max(celda, dp(CELDA_MINIMA_DP));
        cellSize = celda;

        filas.clear();
        float y = getPaddingTop();
        for (int[] dientes : filasDientes) {
            if (dientes.length == 0) continue;
            Fila f = new Fila();
            f.dientes = dientes;
            f.topY = y;
            f.cellTop = y + dp(LABEL_ALTO_DP);
            filas.add(f);
            y = f.cellTop + cellSize + dp(FILA_GAP_DP);
        }
        if (!filas.isEmpty()) {
            y -= dp(FILA_GAP_DP);
        }

        float alto = y + getPaddingBottom();
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), (int) alto);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        boolean oscuro = esOscuro();
        int colorSuperficieBase = oscuro ? COLOR_SUPERFICIE_OSCURO : COLOR_SUPERFICIE_CLARO;
        int colorBordeBase = oscuro ? COLOR_BORDE_OSCURO : COLOR_BORDE_CLARO;
        paintTexto.setColor(colorBordeBase);

        for (Fila fila : filas) {
            float x = getPaddingLeft();
            for (int diente : fila.dientes) {
                dibujarDiente(canvas, diente, x, fila.cellTop, colorSuperficieBase, colorBordeBase);
                canvas.drawText(String.valueOf(diente), x + cellSize / 2f, fila.topY + dp(LABEL_ALTO_DP) - dp(2), paintTexto);
                x += cellSize + dp(GAP_DP);
            }
        }
    }

    private void dibujarDiente(Canvas canvas, int diente, float left, float top,
                                int colorSuperficieBase, int colorBordeBase) {
        Map<String, Integer> sups = modelo.estadoDe(diente);
        Integer estrella = sups != null ? sups.get("*") : null;
        boolean bloqueado = estrella != null && (estrella == OdontogramaModelo.AUSENTE || estrella == OdontogramaModelo.EXTRACCION);
        boolean activo = diente == dienteActivo;

        float escala = cellSize / 44f;

        if (bloqueado) {
            int colorFondo = estrella == OdontogramaModelo.AUSENTE ? COLOR_AUSENTE : COLOR_EXTRACCION_FONDO;
            paintRelleno.setStyle(Paint.Style.FILL);
            paintRelleno.setColor(colorFondo);
            paintRelleno.setAlpha(ALPHA_BLOQUEADO);
            for (float[] poligono : POLIGONOS) {
                canvas.drawPath(pathDePoligono(poligono, left, top, escala), paintRelleno);
            }
            paintBorde.setColor(colorBordeBase);
            paintBorde.setAlpha(ALPHA_BLOQUEADO);
            for (float[] poligono : POLIGONOS) {
                canvas.drawPath(pathDePoligono(poligono, left, top, escala), paintBorde);
            }
            if (estrella == OdontogramaModelo.EXTRACCION) {
                paintX.setAlpha(ALPHA_BLOQUEADO);
                canvas.drawLine(left + 6 * escala, top + 6 * escala, left + 38 * escala, top + 38 * escala, paintX);
                canvas.drawLine(left + 38 * escala, top + 6 * escala, left + 6 * escala, top + 38 * escala, paintX);
                paintX.setAlpha(255);
            }
        } else {
            for (int i = 0; i < POLIGONOS.length; i++) {
                String sup = SUPERFICIES[i];
                Integer estado = sups != null ? sups.get(sup) : null;
                int color = colorDeEstado(estado, colorSuperficieBase);
                paintRelleno.setStyle(Paint.Style.FILL);
                paintRelleno.setColor(color);
                paintRelleno.setAlpha(255);
                canvas.drawPath(pathDePoligono(POLIGONOS[i], left, top, escala), paintRelleno);
            }
            paintBorde.setColor(colorBordeBase);
            paintBorde.setAlpha(255);
            for (float[] poligono : POLIGONOS) {
                canvas.drawPath(pathDePoligono(poligono, left, top, escala), paintBorde);
            }
        }

        if (activo) {
            Paint contorno = new Paint(paintBorde);
            contorno.setColor(COLOR_ACTIVO);
            contorno.setStrokeWidth(dp(1.5f));
            for (float[] poligono : POLIGONOS) {
                canvas.drawPath(pathDePoligono(poligono, left, top, escala), contorno);
            }
        }
    }

    private int colorDeEstado(Integer estado, int colorSuperficieBase) {
        if (estado == null) return colorSuperficieBase;
        switch (estado) {
            case OdontogramaModelo.CARIES: return COLOR_CARIES;
            case OdontogramaModelo.OBTURADO: return COLOR_OBTURADO;
            case OdontogramaModelo.CORONA: return COLOR_CORONA;
            default: return colorSuperficieBase;
        }
    }

    private Path pathDePoligono(float[] puntos, float left, float top, float escala) {
        Path path = new Path();
        path.moveTo(left + puntos[0] * escala, top + puntos[1] * escala);
        for (int i = 2; i < puntos.length; i += 2) {
            path.lineTo(left + puntos[i] * escala, top + puntos[i + 1] * escala);
        }
        path.close();
        return path;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!editable || event.getAction() != MotionEvent.ACTION_DOWN) {
            return editable;
        }

        float tx = event.getX();
        float ty = event.getY();

        for (Fila fila : filas) {
            if (ty < fila.cellTop || ty > fila.cellTop + cellSize) continue;

            float x = getPaddingLeft();
            for (int diente : fila.dientes) {
                if (tx >= x && tx <= x + cellSize) {
                    float relX = (tx - x) / cellSize * 44f;
                    float relY = (ty - fila.cellTop) / cellSize * 44f;
                    String superficie = superficieTocada(diente, relX, relY);
                    if (listener != null && superficie != null) {
                        listener.onSuperficieTocada(diente, superficie);
                    }
                    return true;
                }
                x += cellSize + dp(GAP_DP);
            }
        }
        return true;
    }

    private String superficieTocada(int diente, float relX, float relY) {
        Map<String, Integer> sups = modelo.estadoDe(diente);
        Integer estrella = sups != null ? sups.get("*") : null;
        if (estrella != null && (estrella == OdontogramaModelo.AUSENTE || estrella == OdontogramaModelo.EXTRACCION)) {
            return "*";
        }
        for (int i = 0; i < POLIGONOS.length; i++) {
            if (contienePunto(POLIGONOS[i], relX, relY)) {
                return SUPERFICIES[i];
            }
        }
        return null;
    }

    // Ray casting estándar sobre el polígono (puntos x,y intercalados).
    private boolean contienePunto(float[] puntos, float px, float py) {
        int n = puntos.length / 2;
        boolean dentro = false;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            float xi = puntos[i * 2], yi = puntos[i * 2 + 1];
            float xj = puntos[j * 2], yj = puntos[j * 2 + 1];
            boolean interseca = ((yi > py) != (yj > py))
                    && (px < (xj - xi) * (py - yi) / (yj - yi) + xi);
            if (interseca) dentro = !dentro;
        }
        return dentro;
    }
}
