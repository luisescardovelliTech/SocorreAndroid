package com.luistech.socorre.domain.modelo;

public class FichaMedica {
    public String tipoSanguineo;
    public String alergias;
    public String medicamentos;
    public String condicoes;
    public boolean compartilharNosAlertas;
    public java.util.Date consentimentoEm;

    public FichaMedica() {
    }

    public boolean vazia() {
        return vazio(tipoSanguineo) && vazio(alergias) && vazio(medicamentos) && vazio(condicoes);
    }

    private static boolean vazio(String s) {
        return s == null || s.trim().isEmpty();
    }
}
