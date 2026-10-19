package com.luistech.socorre.domain.caso_de_uso;

public final class ValidadorTelefone {

    private ValidadorTelefone() {
    }

    public static String normalizar(String entrada) {
        if (entrada == null) return null;
        String t = entrada.trim();
        boolean internacional = t.startsWith("+");
        String digitos = t.replaceAll("[^0-9]", "");
        if (digitos.length() < 8 || digitos.length() > 15) return null;
        return internacional ? "+" + digitos : digitos;
    }

    // mascara do telefone: (18) 99999-0000
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
