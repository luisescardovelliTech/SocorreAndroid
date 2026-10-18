package com.luistech.socorre.weather;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.luistech.socorre.domain.modelo.Clima;

import org.junit.Test;

/** Conversão da resposta do Open-Meteo (/v1/forecast?current=...). */
public class ServicoClimaTest {

    private static JsonObject resposta(double temp, double vento, int codigo, double chuvaMm) {
        return JsonParser.parseString("{\"current\":{\"time\":\"2026-10-12T14:00\",\"interval\":900,"
                + "\"temperature_2m\":" + temp + ",\"precipitation\":" + chuvaMm
                + ",\"weather_code\":" + codigo + ",\"wind_speed_10m\":" + vento + "}}").getAsJsonObject();
    }

    @Test
    public void converteCamposAtuais() {
        Clima c = ServicoClima.converter(resposta(27.4, 12.6, 2, 0));
        assertEquals(27.4, c.temperaturaC, 1e-9);
        assertEquals(12.6, c.ventoKmh, 1e-9);
        assertEquals(2, c.codigoTempo);
        assertFalse(c.chuva);
    }

    @Test
    public void chuvaPorCodigoOuPorPrecipitacao() {
        assertTrue(ServicoClima.converter(resposta(20, 5, 63, 0)).chuva);
        assertTrue(ServicoClima.converter(resposta(20, 5, 3, 0.4)).chuva);
    }

    @Test
    public void respostaSemCurrentNaoQuebra() {
        Clima c = ServicoClima.converter(new JsonObject());
        assertEquals(-1, c.codigoTempo);
    }
}
