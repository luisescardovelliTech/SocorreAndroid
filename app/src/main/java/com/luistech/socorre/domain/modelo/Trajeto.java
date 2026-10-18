package com.luistech.socorre.domain.modelo;

import com.google.firebase.firestore.Exclude;

import java.util.Date;

/** users/{uid}/trajetos/{trajetoId}. */
public class Trajeto {
    @Exclude
    public String id;
    public Date inicio;
    public Date fim;
    public String perfilAtividade;
    public double distanciaM;
    public long duracaoSeg;
    public double velocidadeMediaKmh;
    /** Polyline codificada (formato Google). */
    public String polyline;
    public Clima climaInicio;

    public Trajeto() {
    }
}
