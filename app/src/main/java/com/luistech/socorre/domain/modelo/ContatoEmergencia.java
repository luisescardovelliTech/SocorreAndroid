package com.luistech.socorre.domain.modelo;

/** users/{uid}/contatos/{contatoId}. Campos públicos para o Firestore. */
public class ContatoEmergencia {
    public static final int MAXIMO = 5;

    /** Id do documento (não é gravado como campo). */
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
