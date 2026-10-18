package com.luistech.socorre.alert;

import java.util.Map;

/**
 * Monta o texto do SMS a partir de um modelo com marcadores {nome}, {situacao}, {tipo},
 * {dataHora} e {localizacao}. Java puro (testável); os textos vêm do strings.xml.
 */
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

    /** Resume a ficha médica em uma linha, ignorando campos vazios. Nulo se tudo vazio. */
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
