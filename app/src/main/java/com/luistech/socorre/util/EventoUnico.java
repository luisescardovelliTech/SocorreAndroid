package com.luistech.socorre.util;

public class EventoUnico<T> {
    private final T conteudo;
    private boolean tratado;

    public EventoUnico(T conteudo) {
        this.conteudo = conteudo;
    }

    public T consumir() {
        if (tratado) return null;
        tratado = true;
        return conteudo;
    }
}
