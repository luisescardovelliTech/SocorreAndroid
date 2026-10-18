package com.luistech.socorre.domain.modelo;

/** Ciclo de vida. NORMAL = "Estou bem" (registro de rotina na API). CANCELADO = registros antigos. */
public enum StatusEvento {
    NORMAL,
    CANCELADO,
    ALERTA_ENVIADO,
    ENCERRADO
}
