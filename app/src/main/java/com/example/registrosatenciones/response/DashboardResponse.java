package com.example.registrosatenciones.response;

import java.util.List;

public class DashboardResponse {
    private String tituloBarras;
    private String tituloDonut;
    private String tituloLinea;
    private String tituloTop10;
    private List<String> labels7Dias;
    private List<Integer> serie7Dias;
    private List<String> donutLabels;
    private List<Integer> donutValores;
    private List<String> labels6Meses;
    private List<Integer> serie6Meses;
    private List<String> top10Nombres;
    private List<Integer> top10Cantidades;

    public String getTituloBarras() { return tituloBarras; }
    public void setTituloBarras(String tituloBarras) { this.tituloBarras = tituloBarras; }

    public String getTituloDonut() { return tituloDonut; }
    public void setTituloDonut(String tituloDonut) { this.tituloDonut = tituloDonut; }

    public String getTituloLinea() { return tituloLinea; }
    public void setTituloLinea(String tituloLinea) { this.tituloLinea = tituloLinea; }

    public String getTituloTop10() { return tituloTop10; }
    public void setTituloTop10(String tituloTop10) { this.tituloTop10 = tituloTop10; }

    public List<String> getLabels7Dias() { return labels7Dias; }
    public void setLabels7Dias(List<String> labels7Dias) { this.labels7Dias = labels7Dias; }

    public List<Integer> getSerie7Dias() { return serie7Dias; }
    public void setSerie7Dias(List<Integer> serie7Dias) { this.serie7Dias = serie7Dias; }

    public List<String> getDonutLabels() { return donutLabels; }
    public void setDonutLabels(List<String> donutLabels) { this.donutLabels = donutLabels; }

    public List<Integer> getDonutValores() { return donutValores; }
    public void setDonutValores(List<Integer> donutValores) { this.donutValores = donutValores; }

    public List<String> getLabels6Meses() { return labels6Meses; }
    public void setLabels6Meses(List<String> labels6Meses) { this.labels6Meses = labels6Meses; }

    public List<Integer> getSerie6Meses() { return serie6Meses; }
    public void setSerie6Meses(List<Integer> serie6Meses) { this.serie6Meses = serie6Meses; }

    public List<String> getTop10Nombres() { return top10Nombres; }
    public void setTop10Nombres(List<String> top10Nombres) { this.top10Nombres = top10Nombres; }

    public List<Integer> getTop10Cantidades() { return top10Cantidades; }
    public void setTop10Cantidades(List<Integer> top10Cantidades) { this.top10Cantidades = top10Cantidades; }
}