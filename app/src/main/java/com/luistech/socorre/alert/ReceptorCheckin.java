package com.luistech.socorre.alert;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.luistech.socorre.data.local.Preferencias;
import com.luistech.socorre.domain.modelo.TipoEvento;
import com.luistech.socorre.util.Notificacoes;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint(BroadcastReceiver.class)
public class ReceptorCheckin extends Hilt_ReceptorCheckin {

    public static final String ACAO_PERGUNTAR = "com.luistech.socorre.CHECKIN_PERGUNTAR";
    public static final String ACAO_OK = "com.luistech.socorre.CHECKIN_OK";
    public static final String ACAO_PRAZO = "com.luistech.socorre.CHECKIN_PRAZO";

    @Inject
    Preferencias prefs;
    @Inject
    AgendadorCheckin agendador;
    @Inject
    ControladorEmergencia controlador;

    public static PendingIntent intencao(Context c, String acao) {
        Intent i = new Intent(c, ReceptorCheckin.class).setAction(acao);
        return PendingIntent.getBroadcast(c, acao.hashCode(), i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    // perguntar, ok e prazo do check-in
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String acao = intent.getAction();
        if (acao == null) return;
        if (prefs.trajetoAtivoId() == null) {
            agendador.cancelarTudo();
            return;
        }
        switch (acao) {
            case ACAO_PERGUNTAR:
                prefs.definirCheckinPendente(true);
                Notificacoes.mostrar(context, Notificacoes.ID_CHECKIN, Notificacoes.checkin(context));
                agendador.agendarPrazoResposta();
                agendador.agendarPergunta(prefs.intervaloCheckinMin());
                break;
            case ACAO_OK:
                prefs.definirCheckinPendente(false);
                agendador.cancelarPrazoResposta();
                Notificacoes.remover(context, Notificacoes.ID_CHECKIN);
                break;
            case ACAO_PRAZO:
                if (prefs.checkinPendente()) {
                    prefs.definirCheckinPendente(false);
                    Notificacoes.remover(context, Notificacoes.ID_CHECKIN);
                    Log.i("Socorre.Checkin", "check-in sem resposta: iniciando contagem");
                    controlador.iniciarContagem(TipoEvento.CHECKIN_SEM_RESPOSTA, null, false);
                }
                break;
            default:
                break;
        }
    }
}
