package com.luistech.socorre.domain.modelo;

import java.util.Locale;

/** Posição no momento do evento. */
public class Localizacao {
    public double lat;
    public double lng;
    public float precisaoM;
    /** Momento da leitura do GPS (epoch ms). */
    public long obtidaEm;
    /** ATUAL, ULTIMA_CONHECIDA. */
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

    /** Link universal do Google Maps (abre no app ou no navegador). */
    public String linkMapa() {
        return String.format(Locale.US, "https://www.google.com/maps/search/?api=1&query=%.6f,%.6f", lat, lng);
    }
}
