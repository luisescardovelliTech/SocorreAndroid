package com.luistech.socorre.alert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class MontadorMensagemTest {

    @Test
    public void substituiTodosOsMarcadores() {
        Map<String, String> v = new HashMap<>();
        v.put("nome", "Ana");
        v.put("situacao", "pode ter sofrido uma queda");
        v.put("tipo", "Queda");
        v.put("dataHora", "01/10/2026 19:49");
        v.put("localizacao", "https://www.google.com/maps/search/?api=1&query=-21.2,-50.4");
        String modelo = "ALERTA DE EMERGÊNCIA: {nome} {situacao}. Evento: {tipo}. Horário: {dataHora}. Localização: {localizacao}.";
        assertEquals("ALERTA DE EMERGÊNCIA: Ana pode ter sofrido uma queda. Evento: Queda. Horário: 01/10/2026 19:49. "
                        + "Localização: https://www.google.com/maps/search/?api=1&query=-21.2,-50.4.",
                MontadorMensagem.montar(modelo, v, null));
    }

    @Test
    public void anexaFichaSomenteQuandoInformada() {
        Map<String, String> v = new HashMap<>();
        v.put("nome", "Ana");
        assertEquals("Oi Ana Ficha médica: O+; penicilina",
                MontadorMensagem.montar("Oi {nome}", v, "Ficha médica: O+; penicilina"));
        assertEquals("Oi Ana", MontadorMensagem.montar("Oi {nome}", v, "  "));
    }

    @Test
    public void resumoDaFichaIgnoraCamposVazios() {
        assertEquals("Ficha: O+; dipirona", MontadorMensagem.resumirFicha("Ficha:", "O+", "", "dipirona", null));
        assertNull(MontadorMensagem.resumirFicha("Ficha:", null, " ", "", null));
    }

    @Test
    public void valorNuloViraVazio() {
        Map<String, String> v = new HashMap<>();
        v.put("nome", null);
        assertEquals("Oi", MontadorMensagem.montar("Oi {nome}", v, null));
    }
}
