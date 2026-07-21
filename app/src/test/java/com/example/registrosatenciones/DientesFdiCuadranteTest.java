package com.example.registrosatenciones;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.registrosatenciones.ui.odontologia.model.DientesFdi;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.junit.Test;

/**
 * Verifica, cuadrante por cuadrante y diente por diente, que el editor abre exactamente
 * los dientes FDI que corresponden (1=Sup.Der, 2=Sup.Izq, 3=Inf.Izq, 4=Inf.Der) y que los
 * cuatro cuadrantes juntos cubren toda la dentición, sin dientes cruzados ni duplicados.
 */
public class DientesFdiCuadranteTest {

    @Test public void permanentesPorCuadranteSonLosDientesCorrectos() {
        assertArrayEquals(new int[]{18, 17, 16, 15, 14, 13, 12, 11}, DientesFdi.permanentesDeCuadrante(1));
        assertArrayEquals(new int[]{21, 22, 23, 24, 25, 26, 27, 28}, DientesFdi.permanentesDeCuadrante(2));
        assertArrayEquals(new int[]{31, 32, 33, 34, 35, 36, 37, 38}, DientesFdi.permanentesDeCuadrante(3));
        assertArrayEquals(new int[]{48, 47, 46, 45, 44, 43, 42, 41}, DientesFdi.permanentesDeCuadrante(4));
    }

    @Test public void temporariosPorCuadranteSonLosDientesCorrectos() {
        assertArrayEquals(new int[]{55, 54, 53, 52, 51}, DientesFdi.temporariosDeCuadrante(1));
        assertArrayEquals(new int[]{61, 62, 63, 64, 65}, DientesFdi.temporariosDeCuadrante(2));
        assertArrayEquals(new int[]{71, 72, 73, 74, 75}, DientesFdi.temporariosDeCuadrante(3));
        assertArrayEquals(new int[]{85, 84, 83, 82, 81}, DientesFdi.temporariosDeCuadrante(4));
    }

    @Test public void cadaDienteCaeEnSuCuadranteSegunElPrimerDigitoFdi() {
        for (int cuadrante = 1; cuadrante <= 4; cuadrante++) {
            for (int diente : DientesFdi.permanentesDeCuadrante(cuadrante)) {
                assertEquals("permanente " + diente, cuadrante, diente / 10);
            }
            // Temporarios: cuadrantes 5..8 mapeados a 1..4 (superior derecho → 5, etc.).
            for (int diente : DientesFdi.temporariosDeCuadrante(cuadrante)) {
                assertEquals("temporario " + diente, cuadrante + 4, diente / 10);
            }
        }
    }

    @Test public void losCuatroCuadrantesCubrenTodaLaDenticionSinDuplicados() {
        List<Integer> todos = new ArrayList<>();
        for (int c = 1; c <= 4; c++) {
            for (int d : DientesFdi.permanentesDeCuadrante(c)) todos.add(d);
            for (int d : DientesFdi.temporariosDeCuadrante(c)) todos.add(d);
        }
        // 32 permanentes + 20 temporarios = 52 piezas, todas distintas.
        assertEquals(52, todos.size());
        assertEquals("hay dientes duplicados entre cuadrantes", 52, new TreeSet<>(todos).size());

        // Coinciden exactamente con el universo de piezas que dibuja el resumen.
        TreeSet<Integer> resumen = new TreeSet<>();
        for (int d : DientesFdi.PERMANENTES) resumen.add(d);
        for (int d : DientesFdi.TEMPORARIOS) resumen.add(d);
        assertTrue("cuadrantes y resumen no cubren el mismo conjunto de dientes",
                resumen.equals(new TreeSet<>(todos)));
    }
}
