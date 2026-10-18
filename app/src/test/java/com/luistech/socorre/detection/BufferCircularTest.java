package com.luistech.socorre.detection;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BufferCircularTest {

    @Test
    public void descartaMaisAntigoQuandoCheio() {
        BufferCircular<Integer> b = new BufferCircular<>(3);
        for (int i = 1; i <= 5; i++) b.adicionar(i);
        assertEquals(3, b.tamanho());
        assertEquals(3, (int) b.obter(0));
        assertEquals(5, (int) b.obter(2));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void indiceInvalidoLancaExcecao() {
        new BufferCircular<Integer>(2).obter(0);
    }

    @Test
    public void limparZeraTamanho() {
        BufferCircular<Integer> b = new BufferCircular<>(2);
        b.adicionar(1);
        b.limpar();
        assertEquals(0, b.tamanho());
    }
}
