package com.luistech.socorre.domain.caso_de_uso;

/** Valida e normaliza telefones para SMS. Java puro. */
public final class ValidadorTelefone {

    private ValidadorTelefone() {
    }

    /**
     * Remove formatação mantendo o "+" inicial. Aceita de 8 a 15 dígitos (E.164).
     * @return telefone normalizado ou nulo se inválido.
     */
    public static String normalizar(String entrada) {
        if (entrada == null) return null;
        String t = entrada.trim();
        boolean internacional = t.startsWith("+");
        String digitos = t.replaceAll("[^0-9]", "");
        if (digitos.length() < 8 || digitos.length() > 15) return null;
        return internacional ? "+" + digitos : digitos;
    }

    /** Máscara brasileira enquanto digita: (18) 99999-0000 ou (18) 3333-0000. Ignora +55. */
    public static String mascarar(String entrada) {
        if (entrada == null) return "";
        String d = entrada.replaceAll("[^0-9]", "");
        if (d.length() > 11 && d.startsWith("55")) d = d.substring(2);
        if (d.length() > 11) d = d.substring(0, 11);
        if (d.isEmpty()) return "";
        if (d.length() <= 2) return "(" + d;
        String ddd = "(" + d.substring(0, 2) + ") ";
        if (d.length() <= 6) return ddd + d.substring(2);
        int corte = d.length() == 11 ? 7 : 6;
        return ddd + d.substring(2, corte) + "-" + d.substring(corte);
    }
}
