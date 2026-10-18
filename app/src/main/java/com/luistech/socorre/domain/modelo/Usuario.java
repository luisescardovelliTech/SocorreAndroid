package com.luistech.socorre.domain.modelo;

import java.util.Date;
import java.util.Map;

/** users/{uid}. Campos públicos para o Firestore. */
public class Usuario {
    public String nome;
    public String email;
    public String telefone;
    public String dataNascimento; // ISO yyyy-MM-dd
    /** Caminho local da foto (Storage exige plano Blaze; ver README). */
    public String fotoUrl;
    public String perfilAtividade;
    public FichaMedica fichaMedica;
    public Map<String, Object> config;
    public Date consentimentoLgpdEm;
    public Date criadoEm;

    public Usuario() {
    }

    public boolean perfilCompleto() {
        return nome != null && !nome.trim().isEmpty() && telefone != null && !telefone.trim().isEmpty();
    }
}
