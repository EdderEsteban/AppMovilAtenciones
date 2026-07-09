package com.example.registrosatenciones.response;

public class ValoracionDentalResponse {
    private int cariesPerm;
    private int perdidosPerm;
    private int obturadosPerm;
    private int cariesTemp;
    private int extraccionTemp;
    private int obturadosTemp;

    public int getCariesPerm() { return cariesPerm; }
    public void setCariesPerm(int cariesPerm) { this.cariesPerm = cariesPerm; }

    public int getPerdidosPerm() { return perdidosPerm; }
    public void setPerdidosPerm(int perdidosPerm) { this.perdidosPerm = perdidosPerm; }

    public int getObturadosPerm() { return obturadosPerm; }
    public void setObturadosPerm(int obturadosPerm) { this.obturadosPerm = obturadosPerm; }

    public int getCariesTemp() { return cariesTemp; }
    public void setCariesTemp(int cariesTemp) { this.cariesTemp = cariesTemp; }

    public int getExtraccionTemp() { return extraccionTemp; }
    public void setExtraccionTemp(int extraccionTemp) { this.extraccionTemp = extraccionTemp; }

    public int getObturadosTemp() { return obturadosTemp; }
    public void setObturadosTemp(int obturadosTemp) { this.obturadosTemp = obturadosTemp; }
}
