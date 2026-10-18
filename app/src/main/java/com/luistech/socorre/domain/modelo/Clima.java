package com.luistech.socorre.domain.modelo;

/** Fotografia do clima no início do trajeto (Open-Meteo). */
public class Clima {
    public double temperaturaC;
    public double ventoKmh;
    /** Código WMO do tempo atual (0 = céu limpo). -1 = desconhecido. */
    public int codigoTempo = -1;
    public boolean chuva;
    public long obtidoEm;

    public Clima() {
    }
}
