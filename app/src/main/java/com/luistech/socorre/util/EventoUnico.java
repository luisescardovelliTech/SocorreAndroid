package com.luistech.socorre.util;

/** Valor para LiveData consumido uma única vez (mensagens, navegação). */
public class EventoUnico<T> {
    private final T conteudo;
    private boolean tratado;

    public EventoUnico(T conteudo) {
        this.conteudo = conteudo;
    }

    /** Devolve o conteúdo na primeira chamada e nulo nas seguintes. */
    public T consumir() {
        if (tratado) return null;
        tratado = true;
        return conteudo;
    }
}
