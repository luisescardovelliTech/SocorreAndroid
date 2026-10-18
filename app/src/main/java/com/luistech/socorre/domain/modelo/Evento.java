package com.luistech.socorre.domain.modelo;

import com.google.firebase.firestore.Exclude;

import java.util.Date;

/** users/{uid}/eventos/{eventoId}. */
public class Evento {
    @Exclude
    public String id;
    public String tipo;
    public String status;
    public Date dataHora;
    /** Nulo quando o GPS não respondeu a tempo. */
    public Localizacao localizacao;
    public Float precisaoM;
    public Integer bateriaPct;
    public String trajetoId;
    public String perfilAtividade;
    public Leituras leituras;
    public Envios envios = new Envios();
    public boolean teste;
    /** Somente no modo de teste: o que teria sido enviado (texto do SMS, destinatários, payload). */
    public java.util.Map<String, Object> previaTeste;

    public Evento() {
    }
}
