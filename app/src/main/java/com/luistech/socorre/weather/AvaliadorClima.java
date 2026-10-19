package com.luistech.socorre.weather;

import com.luistech.socorre.domain.modelo.Clima;

import java.util.ArrayList;
import java.util.List;

public final class AvaliadorClima {

    public enum Aviso { CHUVA, VENTO_FORTE, FRIO_EXTREMO, CALOR_EXTREMO }

    static final double VENTO_MAX_KMH = 40;
    static final double FRIO_C = 5;
    static final double CALOR_C = 35;

    private AvaliadorClima() {
    }

    public static List<Aviso> avaliar(Clima c) {
        List<Aviso> avisos = new ArrayList<>();
        if (c == null) return avisos;
        if (c.chuva) avisos.add(Aviso.CHUVA);
        if (c.ventoKmh > VENTO_MAX_KMH) avisos.add(Aviso.VENTO_FORTE);
        if (c.temperaturaC <= FRIO_C) avisos.add(Aviso.FRIO_EXTREMO);
        if (c.temperaturaC >= CALOR_C) avisos.add(Aviso.CALOR_EXTREMO);
        return avisos;
    }

    public static boolean codigoIndicaChuva(int codigo) {
        return (codigo >= 51 && codigo <= 67) || (codigo >= 80 && codigo <= 82) || (codigo >= 95 && codigo <= 99);
    }
}
