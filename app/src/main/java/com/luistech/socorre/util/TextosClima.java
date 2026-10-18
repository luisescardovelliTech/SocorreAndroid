package com.luistech.socorre.util;

import android.content.Context;

import com.luistech.socorre.R;

/** Descrição em pt-BR do código de tempo WMO do Open-Meteo. */
public final class TextosClima {

    private TextosClima() {
    }

    public static String descricao(Context c, int codigo) {
        int res;
        if (codigo == 0) res = R.string.clima_ceu_limpo;
        else if (codigo >= 1 && codigo <= 3) res = R.string.clima_nublado;
        else if (codigo == 45 || codigo == 48) res = R.string.clima_neblina;
        else if (codigo >= 51 && codigo <= 57) res = R.string.clima_garoa;
        else if ((codigo >= 61 && codigo <= 67) || (codigo >= 80 && codigo <= 82)) res = R.string.clima_com_chuva;
        else if ((codigo >= 71 && codigo <= 77) || codigo == 85 || codigo == 86) res = R.string.clima_neve;
        else if (codigo >= 95 && codigo <= 99) res = R.string.clima_tempestade;
        else res = R.string.clima_desconhecido;
        return c.getString(res);
    }
}
