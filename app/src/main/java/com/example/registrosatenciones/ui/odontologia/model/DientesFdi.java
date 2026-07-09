package com.example.registrosatenciones.ui.odontologia.model;

public class DientesFdi {

    public static final int[] PERMANENTES = generarRango(1, 4, 8);
    public static final int[] TEMPORARIOS = generarRango(5, 8, 5);

    // Orden anatómico de exhibición (odontograma completo, calcado de _Odontograma.cshtml):
    // arcada superior e inferior, cada una combinando sus dos cuadrantes.
    public static final int[] PERMANENTES_SUPERIOR = {18, 17, 16, 15, 14, 13, 12, 11, 21, 22, 23, 24, 25, 26, 27, 28};
    public static final int[] PERMANENTES_INFERIOR = {48, 47, 46, 45, 44, 43, 42, 41, 31, 32, 33, 34, 35, 36, 37, 38};
    public static final int[] TEMPORARIOS_SUPERIOR = {55, 54, 53, 52, 51, 61, 62, 63, 64, 65};
    public static final int[] TEMPORARIOS_INFERIOR = {85, 84, 83, 82, 81, 71, 72, 73, 74, 75};

    private DientesFdi() {}

    private static int[] generarRango(int cuadranteInicio, int cuadranteFin, int piezasPorCuadrante) {
        int[] resultado = new int[(cuadranteFin - cuadranteInicio + 1) * piezasPorCuadrante];
        int i = 0;
        for (int q = cuadranteInicio; q <= cuadranteFin; q++) {
            for (int p = 1; p <= piezasPorCuadrante; p++) {
                resultado[i++] = q * 10 + p;
            }
        }
        return resultado;
    }

    // Cuadrante 1=Sup.Der, 2=Sup.Izq, 3=Inf.Izq, 4=Inf.Der (orden anatómico de la web)
    public static int[] permanentesDeCuadrante(int cuadrante) {
        switch (cuadrante) {
            case 1: return new int[]{18, 17, 16, 15, 14, 13, 12, 11};
            case 2: return new int[]{21, 22, 23, 24, 25, 26, 27, 28};
            case 3: return new int[]{31, 32, 33, 34, 35, 36, 37, 38};
            case 4: return new int[]{48, 47, 46, 45, 44, 43, 42, 41};
            default: throw new IllegalArgumentException("Cuadrante inválido: " + cuadrante);
        }
    }

    public static int[] temporariosDeCuadrante(int cuadrante) {
        switch (cuadrante) {
            case 1: return new int[]{55, 54, 53, 52, 51};
            case 2: return new int[]{61, 62, 63, 64, 65};
            case 3: return new int[]{71, 72, 73, 74, 75};
            case 4: return new int[]{85, 84, 83, 82, 81};
            default: throw new IllegalArgumentException("Cuadrante inválido: " + cuadrante);
        }
    }
}
