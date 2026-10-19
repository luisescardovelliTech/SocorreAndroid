package com.luistech.socorre.detection;

public class BufferCircular<T> {
    private final Object[] itens;
    private int inicio;
    private int tamanho;

    public BufferCircular(int capacidade) {
        if (capacidade <= 0) throw new IllegalArgumentException("capacidade deve ser positiva");
        itens = new Object[capacidade];
    }

    public void adicionar(T item) {
        int fim = (inicio + tamanho) % itens.length;
        itens[fim] = item;
        if (tamanho < itens.length) {
            tamanho++;
        } else {
            inicio = (inicio + 1) % itens.length;
        }
    }

    @SuppressWarnings("unchecked")
    public T obter(int indice) {
        if (indice < 0 || indice >= tamanho) throw new IndexOutOfBoundsException(String.valueOf(indice));
        return (T) itens[(inicio + indice) % itens.length];
    }

    public int tamanho() {
        return tamanho;
    }

    public int capacidade() {
        return itens.length;
    }

    public void limpar() {
        java.util.Arrays.fill(itens, null);
        inicio = 0;
        tamanho = 0;
    }
}
