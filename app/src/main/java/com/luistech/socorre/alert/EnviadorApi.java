package com.luistech.socorre.alert;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.luistech.socorre.worker.TrabalhadorReenvioApi;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Enfileira o envio para a API no WorkManager. A fila é persistida em disco, então o pedido
 * sobrevive a falta de internet, fechamento do app e reinício. Nunca bloqueia o SMS.
 */
@Singleton
public class EnviadorApi {

    private final Context contexto;

    @Inject
    public EnviadorApi(@ApplicationContext Context contexto) {
        this.contexto = contexto;
    }

    public void enfileirar(String uid, String eventoId, String payload) {
        agendar(contexto, uid, eventoId, payload, 1, 0, ExistingWorkPolicy.KEEP);
    }

    public static void agendar(Context c, String uid, String eventoId, String payload,
                               int tentativa, long atrasoSeg, ExistingWorkPolicy politica) {
        Data dados = new Data.Builder()
                .putString(TrabalhadorReenvioApi.UID, uid)
                .putString(TrabalhadorReenvioApi.EVENTO_ID, eventoId)
                .putString(TrabalhadorReenvioApi.PAYLOAD, payload)
                .putInt(TrabalhadorReenvioApi.TENTATIVA, tentativa)
                .build();
        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(TrabalhadorReenvioApi.class)
                .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setInitialDelay(atrasoSeg, TimeUnit.SECONDS)
                .setInputData(dados)
                .addTag("api_eventos")
                .build();
        // Nome único por evento: o mesmo alerta nunca é enfileirado duas vezes.
        WorkManager.getInstance(c).enqueueUniqueWork("api_" + eventoId, politica, req);
    }
}
