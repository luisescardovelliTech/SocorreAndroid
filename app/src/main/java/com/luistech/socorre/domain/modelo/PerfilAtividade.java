package com.luistech.socorre.domain.modelo;

/** Perfis de atividade suportados. O nome do enum é o valor gravado no Firestore e na API. */
public enum PerfilAtividade {
    BICICLETA,
    MOTO,
    CAMINHADA_CASA,
    TRABALHO_CAMPO;

    /** Converte texto salvo em enum, com valor padrão seguro. */
    public static PerfilAtividade deTexto(String texto) {
        if (texto != null) {
            for (PerfilAtividade p : values()) {
                if (p.name().equals(texto)) return p;
            }
        }
        return CAMINHADA_CASA;
    }
}
