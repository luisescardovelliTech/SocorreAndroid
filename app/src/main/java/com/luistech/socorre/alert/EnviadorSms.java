package com.luistech.socorre.alert;

import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.telephony.SmsManager;
import android.util.Log;

import com.luistech.socorre.domain.modelo.ContatoEmergencia;
import com.luistech.socorre.domain.modelo.Envios;
import com.luistech.socorre.domain.modelo.StatusEnvio;
import com.luistech.socorre.security.GerenciadorPermissoes;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class EnviadorSms {

    private static final String TAG = "Socorre.Sms";
    private final Context contexto;

    @Inject
    public EnviadorSms(@ApplicationContext Context contexto) {
        this.contexto = contexto;
    }

    // manda o sms para todos os contatos
    public Envios.Sms enviar(List<ContatoEmergencia> contatos, String mensagem, String uid, String eventoId) {
        Envios.Sms resultado = new Envios.Sms();
        if (contatos == null || contatos.isEmpty()) {
            resultado.status = StatusEnvio.SEM_CONTATOS;
            return resultado;
        }
        if (!GerenciadorPermissoes.temSms(contexto)) {
            resultado.status = StatusEnvio.SEM_PERMISSAO;
            return resultado;
        }
        SmsManager sms = gerenciador();
        ArrayList<String> partes = sms.divideMessage(mensagem);
        for (int i = 0; i < contatos.size(); i++) {
            String telefone = contatos.get(i).telefone;
            try {
                ArrayList<PendingIntent> confirmacoes = new ArrayList<>();
                PendingIntent pi = ReceptorStatusSms.intencao(contexto, uid, eventoId, telefone, i);
                for (int p = 0; p < partes.size(); p++) confirmacoes.add(p == partes.size() - 1 ? pi : null);
                sms.sendMultipartTextMessage(telefone, null, partes, confirmacoes, null);
                resultado.enviadosPara.add(telefone);
            } catch (Exception e) {
                Log.w(TAG, "falha ao enviar SMS para contato prioridade=" + (i + 1), e);
                resultado.falhas.add(telefone);
            }
        }
        if (resultado.falhas.isEmpty()) resultado.status = StatusEnvio.ENVIADO;
        else if (resultado.enviadosPara.isEmpty()) resultado.status = StatusEnvio.FALHOU;
        else resultado.status = StatusEnvio.PARCIAL;
        return resultado;
    }

    public boolean enviarTeste(String telefone, String mensagem) {
        if (!GerenciadorPermissoes.temSms(contexto)) return false;
        try {
            SmsManager sms = gerenciador();
            sms.sendMultipartTextMessage(telefone, null, sms.divideMessage(mensagem), null, null);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "falha no SMS de teste", e);
            return false;
        }
    }

    @SuppressWarnings("deprecation")
    private SmsManager gerenciador() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return contexto.getSystemService(SmsManager.class);
        }
        return SmsManager.getDefault();
    }
}
