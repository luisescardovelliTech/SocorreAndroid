package com.luistech.socorre.domain.modelo;

import com.google.firebase.firestore.Exclude;

import java.util.Date;

public class Evento {
    @Exclude
    public String id;
    public String tipo;
    public String status;
    public Date dataHora;
    public Localizacao localizacao;
    public Float precisaoM;
    public Integer bateriaPct;
    public String trajetoId;
    public String perfilAtividade;
    public Leituras leituras;
    public Envios envios = new Envios();
    public boolean teste;
    public java.util.Map<String, Object> previaTeste;

    public Evento() {
    }
}
