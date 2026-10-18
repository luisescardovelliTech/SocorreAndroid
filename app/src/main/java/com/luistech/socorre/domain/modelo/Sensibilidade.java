package com.luistech.socorre.domain.modelo;

/**
 * Sensibilidade da detecção. O fator multiplica os limiares de impacto e rotação:
 * menor fator = limiares menores = detecta mais (e mais falsos positivos).
 */
public enum Sensibilidade {
    BAIXA(1.20),
    MEDIA(1.00),
    ALTA(0.85);

    public final double fator;

    Sensibilidade(double fator) {
        this.fator = fator;
    }

    public static Sensibilidade deTexto(String texto) {
        if (texto != null) {
            for (Sensibilidade s : values()) {
                if (s.name().equals(texto)) return s;
            }
        }
        return MEDIA;
    }
}
