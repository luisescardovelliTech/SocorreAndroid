package com.luistech.socorre.weather;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.luistech.socorre.domain.modelo.Clima;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class AvaliadorClimaTest {

    private static Clima clima(double temp, double vento, boolean chuva) {
        Clima c = new Clima();
        c.temperaturaC = temp;
        c.ventoKmh = vento;
        c.chuva = chuva;
        return c;
    }

    @Test
    public void climaBomNaoGeraAviso() {
        assertTrue(AvaliadorClima.avaliar(clima(24, 10, false)).isEmpty());
        assertTrue(AvaliadorClima.avaliar(null).isEmpty());
    }

    @Test
    public void detectaChuvaVentoECalor() {
        List<AvaliadorClima.Aviso> a = AvaliadorClima.avaliar(clima(36, 45, true));
        assertEquals(Arrays.asList(AvaliadorClima.Aviso.CHUVA, AvaliadorClima.Aviso.VENTO_FORTE,
                AvaliadorClima.Aviso.CALOR_EXTREMO), a);
    }

    @Test
    public void ventoNoLimiteNaoAvisa() {
        assertTrue(AvaliadorClima.avaliar(clima(20, 40, false)).isEmpty());
    }

    @Test
    public void codigosWmo() {
        assertTrue(AvaliadorClima.codigoIndicaChuva(61));
        assertTrue(AvaliadorClima.codigoIndicaChuva(53));
        assertTrue(AvaliadorClima.codigoIndicaChuva(81));
        assertTrue(AvaliadorClima.codigoIndicaChuva(95));
        assertTrue(!AvaliadorClima.codigoIndicaChuva(0));
        assertTrue(!AvaliadorClima.codigoIndicaChuva(3));
        assertTrue(!AvaliadorClima.codigoIndicaChuva(45));
        assertTrue(!AvaliadorClima.codigoIndicaChuva(71));
    }
}
