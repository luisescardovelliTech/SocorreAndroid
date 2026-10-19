package com.luistech.socorre.domain.modelo;

public enum TipoEvento {
    QUEDA,
    PANICO,
    CHECKIN_SEM_RESPOSTA;

    public static TipoEvento deTexto(String texto) {
        if (texto != null) {
            for (TipoEvento t : values()) {
                if (t.name().equals(texto)) return t;
            }
        }
        return QUEDA;
    }
}
