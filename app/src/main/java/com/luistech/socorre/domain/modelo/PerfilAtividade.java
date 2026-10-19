package com.luistech.socorre.domain.modelo;

public enum PerfilAtividade {
    BICICLETA,
    MOTO,
    CAMINHADA_CASA,
    TRABALHO_CAMPO;

    public static PerfilAtividade deTexto(String texto) {
        if (texto != null) {
            for (PerfilAtividade p : values()) {
                if (p.name().equals(texto)) return p;
            }
        }
        return CAMINHADA_CASA;
    }
}
