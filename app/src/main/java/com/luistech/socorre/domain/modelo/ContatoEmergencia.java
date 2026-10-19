package com.luistech.socorre.domain.modelo;

public class ContatoEmergencia {
    public static final int MAXIMO = 5;

    @com.google.firebase.firestore.Exclude
    public String id;
    public String nome;
    public String telefone;
    public String relacao;
    public int prioridade;
    public boolean usaApp;
    public String uidContato;

    public ContatoEmergencia() {
    }

    public ContatoEmergencia(String nome, String telefone, String relacao, int prioridade) {
        this.nome = nome;
        this.telefone = telefone;
        this.relacao = relacao;
        this.prioridade = prioridade;
    }
}
