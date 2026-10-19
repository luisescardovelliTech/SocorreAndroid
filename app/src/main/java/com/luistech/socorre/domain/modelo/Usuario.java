package com.luistech.socorre.domain.modelo;

import java.util.Date;
import java.util.Map;

public class Usuario {
    public String nome;
    public String email;
    public String telefone;
    public String dataNascimento;
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
