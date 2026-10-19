package com.luistech.socorre.util;

import android.content.Context;

import com.luistech.socorre.R;
import com.luistech.socorre.domain.modelo.Envios;
import com.luistech.socorre.domain.modelo.StatusEnvio;

public final class TextosEnvio {

    private TextosEnvio() {
    }

    public static String status(Context c, String status) {
        if (status == null) return c.getString(R.string.envio_pendente);
        switch (status) {
            case StatusEnvio.ENVIADO:
                return c.getString(R.string.envio_enviado);
            case StatusEnvio.FALHOU:
                return c.getString(R.string.envio_falhou);
            case StatusEnvio.PARCIAL:
                return c.getString(R.string.envio_parcial);
            case StatusEnvio.SEM_PERMISSAO:
                return c.getString(R.string.envio_sem_permissao);
            case StatusEnvio.SEM_CONTATOS:
                return c.getString(R.string.envio_sem_contatos);
            case StatusEnvio.NAO_CONFIGURADO:
                return c.getString(R.string.envio_nao_configurado);
            case StatusEnvio.SIMULADO:
                return c.getString(R.string.envio_simulado);
            case StatusEnvio.PENDENTE:
            default:
                return c.getString(R.string.envio_pendente);
        }
    }

    public static String sms(Context c, Envios.Sms sms) {
        if (sms == null) return status(c, null);
        String base = status(c, sms.status);
        int confirmados = sms.confirmados != null ? sms.confirmados.size() : 0;
        int total = sms.enviadosPara != null ? sms.enviadosPara.size() : 0;
        if (total == 0 || StatusEnvio.SIMULADO.equals(sms.status)) return base;
        return c.getString(R.string.envio_sms_detalhe, base, confirmados, total);
    }

    public static String api(Context c, Envios.Api api) {
        if (api == null) return status(c, null);
        String base = status(c, api.status);
        if (api.tentativas == 0) return base;
        return c.getString(R.string.envio_api_detalhe, base, api.tentativas,
                api.ultimoErro != null ? api.ultimoErro : c.getString(R.string.nenhum));
    }
}
