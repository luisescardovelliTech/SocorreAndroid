package com.luistech.socorre.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.luistech.socorre.R;
import com.luistech.socorre.alert.AgendadorCheckin;
import com.luistech.socorre.data.local.Preferencias;
import com.luistech.socorre.util.Notificacoes;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * RF17: restaura o monitoramento após reiniciar o aparelho, se estava ativo.
 * Se o Android bloquear o início em segundo plano, avisa o usuário para reativar com um toque.
 */
@AndroidEntryPoint(BroadcastReceiver.class)
public class ReceptorBoot extends Hilt_ReceptorBoot {

    @Inject
    Preferencias prefs;
    @Inject
    AgendadorCheckin checkin;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String acao = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(acao) && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(acao)) return;
        if (!prefs.monitoramentoAtivo()) return;
        try {
            // Alarmes são apagados no reinício: se havia trajeto, retoma e reagenda o check-in.
            boolean trajeto = prefs.trajetoAtivoId() != null;
            if (trajeto) checkin.agendarPergunta(prefs.intervaloCheckinMin());
            ServicoMonitoramento.enviar(context, trajeto
                    ? ServicoMonitoramento.ACAO_INICIAR_TRAJETO : ServicoMonitoramento.ACAO_INICIAR);
        } catch (Exception e) {
            Log.w("Socorre.Boot", "nao foi possivel restaurar o monitoramento", e);
            Notificacoes.mostrar(context, Notificacoes.ID_AVISO,
                    Notificacoes.aviso(context, R.string.boot_falhou_titulo, R.string.boot_falhou_texto));
        }
    }
}
