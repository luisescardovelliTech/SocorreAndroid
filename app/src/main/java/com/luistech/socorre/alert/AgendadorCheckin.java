package com.luistech.socorre.alert;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.content.Context;
import android.os.SystemClock;

import com.luistech.socorre.security.GerenciadorPermissoes;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class AgendadorCheckin {

    public static final long TEMPO_RESPOSTA_MS = 2 * 60 * 1000;
    private final Context contexto;

    @Inject
    public AgendadorCheckin(@ApplicationContext Context contexto) {
        this.contexto = contexto;
    }

    public void agendarPergunta(int intervaloMin) {
        if (intervaloMin <= 0) return;
        agendar(ReceptorCheckin.ACAO_PERGUNTAR, intervaloMin * 60_000L);
    }

    public void agendarPrazoResposta() {
        agendar(ReceptorCheckin.ACAO_PRAZO, TEMPO_RESPOSTA_MS);
    }

    public void cancelarPrazoResposta() {
        alarmes().cancel(ReceptorCheckin.intencao(contexto, ReceptorCheckin.ACAO_PRAZO));
    }

    public void cancelarTudo() {
        cancelarPrazoResposta();
        alarmes().cancel(ReceptorCheckin.intencao(contexto, ReceptorCheckin.ACAO_PERGUNTAR));
    }

    @SuppressLint("MissingPermission")
    private void agendar(String acao, long atrasoMs) {
        long quando = SystemClock.elapsedRealtime() + atrasoMs;
        AlarmManager am = alarmes();
        if (GerenciadorPermissoes.podeAlarmeExato(contexto)) {
            am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, quando, ReceptorCheckin.intencao(contexto, acao));
        } else {
            am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, quando, ReceptorCheckin.intencao(contexto, acao));
        }
    }

    private AlarmManager alarmes() {
        return contexto.getSystemService(AlarmManager.class);
    }
}
