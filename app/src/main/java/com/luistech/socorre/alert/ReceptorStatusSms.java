package com.luistech.socorre.alert;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/** Recebe da operadora o resultado de cada SMS e registra em evento.envios.sms. */
@AndroidEntryPoint(BroadcastReceiver.class)
public class ReceptorStatusSms extends Hilt_ReceptorStatusSms {

    @Inject
    FirebaseFirestore db;

    private static final String ACAO = "com.luistech.socorre.SMS_ENVIADO";
    private static final String EXTRA_UID = "uid";
    private static final String EXTRA_EVENTO = "eventoId";
    private static final String EXTRA_TELEFONE = "telefone";

    static PendingIntent intencao(Context c, String uid, String eventoId, String telefone, int indice) {
        Intent i = new Intent(c, ReceptorStatusSms.class)
                .setAction(ACAO)
                .putExtra(EXTRA_UID, uid)
                .putExtra(EXTRA_EVENTO, eventoId)
                .putExtra(EXTRA_TELEFONE, telefone);
        int codigo = (eventoId + indice).hashCode();
        return PendingIntent.getBroadcast(c, codigo, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent); // injeção do Hilt
        String uid = intent.getStringExtra(EXTRA_UID);
        String eventoId = intent.getStringExtra(EXTRA_EVENTO);
        String telefone = intent.getStringExtra(EXTRA_TELEFONE);
        if (uid == null || eventoId == null || telefone == null) return;
        boolean ok = getResultCode() == Activity.RESULT_OK;
        Log.i("Socorre.Sms", "confirmacao sms evento=" + eventoId + " ok=" + ok);
        Map<String, Object> campos = new HashMap<>();
        campos.put(ok ? "envios.sms.confirmados" : "envios.sms.falhas", FieldValue.arrayUnion(telefone));
        PendingResult pendente = goAsync();
        db.collection("users").document(uid)
                .collection("eventos").document(eventoId).update(campos)
                .addOnCompleteListener(t -> pendente.finish());
    }
}
