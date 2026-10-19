package com.luistech.socorre.domain.modelo;

import java.util.Locale;

public class Localizacao {
    public double lat;
    public double lng;
    public float precisaoM;
    public long obtidaEm;
    public String origem;

    public Localizacao() {
    }

    public Localizacao(double lat, double lng, float precisaoM, long obtidaEm, String origem) {
        this.lat = lat;
        this.lng = lng;
        this.precisaoM = precisaoM;
        this.obtidaEm = obtidaEm;
        this.origem = origem;
    }

    public String linkMapa() {
        return String.format(Locale.US, "https://www.google.com/maps/search/?api=1&query=%.6f,%.6f", lat, lng);
    }
}
