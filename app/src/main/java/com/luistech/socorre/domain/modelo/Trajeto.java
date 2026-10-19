package com.luistech.socorre.domain.modelo;

import com.google.firebase.firestore.Exclude;

import java.util.Date;

public class Trajeto {
    @Exclude
    public String id;
    public Date inicio;
    public Date fim;
    public String perfilAtividade;
    public double distanciaM;
    public long duracaoSeg;
    public double velocidadeMediaKmh;
    public String polyline;
    public Clima climaInicio;

    public Trajeto() {
    }
}
