package com.luistech.socorre.alert;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint(BroadcastReceiver.class)
public class ReceptorAcoes extends Hilt_ReceptorAcoes {

    public static final String ACAO_CANCELAR = "com.luistech.socorre.CANCELAR_EMERGENCIA";
    public static final String ACAO_ENVIAR_AGORA = "com.luistech.socorre.ENVIAR_AGORA";

    @Inject
    ControladorEmergencia controlador;

    public static PendingIntent intencao(Context c, String acao) {
        Intent i = new Intent(c, ReceptorAcoes.class).setAction(acao);
        return PendingIntent.getBroadcast(c, acao.hashCode(), i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACAO_CANCELAR.equals(intent.getAction())) controlador.cancelar();
        else if (ACAO_ENVIAR_AGORA.equals(intent.getAction())) controlador.enviarAgora();
    }
}
