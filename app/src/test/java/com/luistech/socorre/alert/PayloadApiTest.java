package com.luistech.socorre.alert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.luistech.socorre.domain.modelo.Evento;
import com.luistech.socorre.domain.modelo.Leituras;
import com.luistech.socorre.domain.modelo.Localizacao;

import org.junit.Test;

import java.util.Date;

public class PayloadApiTest {

    @Test
    public void quedaComTodosOsCampos() {
        Evento e = new Evento();
        e.tipo = "QUEDA";
        e.dataHora = new Date();
        e.localizacao = new Localizacao(-21.2089, -50.4328, 12f, 0, "ATUAL");
        e.leituras = new Leituras();
        e.leituras.picoG = 4.3;
        e.leituras.velocidadeAntesKmh = 32.5;
        e.bateriaPct = 64;

        JsonObject j = JsonParser.parseString(
                PayloadApi.montar(e, "Ana", "abc123", "Samsung SM-A155", 1013.2f, 28.1)).getAsJsonObject();
        assertEquals("Ana", j.get("usuario").getAsString());
        assertEquals(2, j.get("tipo_registro").getAsInt());
        assertEquals("abc123", j.get("device_id").getAsString());
        assertEquals("Samsung SM-A155", j.get("modelo").getAsString());
        assertEquals(64, j.get("bateria").getAsInt());
        assertEquals(-21.2089, j.get("latitude").getAsDouble(), 1e-9);
        assertEquals(-50.4328, j.get("longitude").getAsDouble(), 1e-9);
        assertEquals(32.5, j.get("velocidade").getAsDouble(), 1e-9);
        assertEquals(28.1, j.get("temperatura").getAsDouble(), 1e-9);
        assertEquals(1013.2, j.get("pressao").getAsDouble(), 1e-3);
        assertEquals(4.3, j.get("nivel_queda").getAsDouble(), 1e-9);
    }

    @Test
    public void panicoSemGpsOmiteCamposVazios() {
        Evento e = new Evento();
        e.tipo = "PANICO";
        e.dataHora = new Date();
        JsonObject j = JsonParser.parseString(PayloadApi.montar(e, "Ana", "id", "m", null, null)).getAsJsonObject();
        assertEquals(2, j.get("tipo_registro").getAsInt());
        assertFalse(j.has("latitude"));
        assertFalse(j.has("nivel_queda"));
        assertFalse(j.has("pressao"));
        assertFalse(j.has("temperatura"));
        assertFalse(j.has("bateria"));
    }

    @Test
    public void respeitaTamanhoMaximoDoUsuario() {
        Evento e = new Evento();
        e.dataHora = new Date();
        StringBuilder longo = new StringBuilder();
        for (int i = 0; i < 200; i++) longo.append('a');
        JsonObject j = JsonParser.parseString(PayloadApi.montar(e, longo.toString(), "id", "m", null, null)).getAsJsonObject();
        assertEquals(150, j.get("usuario").getAsString().length());
    }

    @Test
    public void estouBemVaiComoRotina() {
        JsonObject j = JsonParser.parseString(PayloadApi.montar(new Evento(), "Ana", "id", "m", null, null,
                PayloadApi.REGISTRO_ROTINA)).getAsJsonObject();
        assertEquals(1, j.get("tipo_registro").getAsInt());
    }
}
