package com.luistech.socorre.alert;

import java.util.Map;

public final class MontadorMensagem {

    private MontadorMensagem() {
    }

    public static String montar(String modelo, Map<String, String> valores, String fichaMedicaOuNulo) {
        String texto = modelo == null ? "" : modelo;
        for (Map.Entry<String, String> v : valores.entrySet()) {
            texto = texto.replace("{" + v.getKey() + "}", v.getValue() == null ? "" : v.getValue());
        }
        if (fichaMedicaOuNulo != null && !fichaMedicaOuNulo.trim().isEmpty()) {
            texto = texto.trim() + " " + fichaMedicaOuNulo.trim();
        }
        return texto.trim();
    }

    public static String resumirFicha(String rotulo, String tipoSanguineo, String alergias,
                                      String medicamentos, String condicoes) {
        StringBuilder sb = new StringBuilder();
        anexar(sb, tipoSanguineo);
        anexar(sb, alergias);
        anexar(sb, medicamentos);
        anexar(sb, condicoes);
        return sb.length() == 0 ? null : rotulo + " " + sb;
    }

    private static void anexar(StringBuilder sb, String valor) {
        if (valor == null || valor.trim().isEmpty()) return;
        if (sb.length() > 0) sb.append("; ");
        sb.append(valor.trim());
    }
}
