package com.luistech.socorre.alert;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.luistech.socorre.data.local.Preferencias;
import com.luistech.socorre.data.repositorio.RepositorioEventos;
import com.luistech.socorre.data.repositorio.RepositorioUsuario;
import com.luistech.socorre.domain.modelo.Evento;
import com.luistech.socorre.domain.modelo.Leituras;
import com.luistech.socorre.domain.modelo.StatusEvento;
import com.luistech.socorre.domain.modelo.TipoEvento;
import com.luistech.socorre.service.ServicoMonitoramento;
import com.luistech.socorre.ui.activity.AlertaEnviadoActivity;
import com.luistech.socorre.ui.activity.ContagemActivity;
import com.luistech.socorre.util.Notificacoes;

import java.util.Collections;
import java.util.Date;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class ControladorEmergencia {

    public enum Fase { OCIOSO, CONTANDO, ENVIANDO, ENVIADO }

    public static final class Estado {
        public final Fase fase;
        public final int segundos;
        public final TipoEvento tipo;
        public final boolean teste;
        @Nullable
        public final Evento evento;

        Estado(Fase fase, int segundos, TipoEvento tipo, boolean teste, @Nullable Evento evento) {
            this.fase = fase;
            this.segundos = segundos;
            this.tipo = tipo;
            this.teste = teste;
            this.evento = evento;
        }
    }

    private static final String TAG = "Socorre.Emergencia";
    private static final long WAKE_LOCK_MS = 5 * 60 * 1000;

    private final Context contexto;
    private final Preferencias prefs;
    private final DespachanteAlerta despachante;
    private final AlarmeSonoro alarme;
    private final RepositorioEventos eventos;
    private final RepositorioUsuario usuarios;
    private final ExecutorService executor;
    private final FirebaseAnalytics analytics;
    private final Handler principal = new Handler(Looper.getMainLooper());
    private final MutableLiveData<Estado> estado =
            new MutableLiveData<>(new Estado(Fase.OCIOSO, 0, TipoEvento.QUEDA, false, null));

    private Leituras leituras;
    private PowerManager.WakeLock wakeLock;

    private final Runnable tique = new Runnable() {
        @Override
        public void run() {
            Estado e = atual();
            if (e.fase != Fase.CONTANDO) return;
            int restante = e.segundos - 1;
            if (restante <= 0) {
                enviarAgora();
                return;
            }
            publicar(new Estado(Fase.CONTANDO, restante, e.tipo, e.teste, null));
            Notificacoes.mostrar(contexto, Notificacoes.ID_EMERGENCIA, Notificacoes.contagem(contexto, restante, e.teste));
            principal.postDelayed(this, 1000);
        }
    };

    @Inject
    public ControladorEmergencia(@ApplicationContext Context contexto, Preferencias prefs,
                                 DespachanteAlerta despachante, AlarmeSonoro alarme,
                                 RepositorioEventos eventos, RepositorioUsuario usuarios,
                                 ExecutorService executor, FirebaseAnalytics analytics) {
        this.contexto = contexto;
        this.prefs = prefs;
        this.despachante = despachante;
        this.alarme = alarme;
        this.eventos = eventos;
        this.usuarios = usuarios;
        this.executor = executor;
        this.analytics = analytics;
    }

    public LiveData<Estado> estado() {
        return estado;
    }

    public Estado atual() {
        Estado e = estado.getValue();
        return e != null ? e : new Estado(Fase.OCIOSO, 0, TipoEvento.QUEDA, false, null);
    }

    // abre a contagem regressiva antes de enviar o alerta
    @MainThread
    public boolean iniciarContagem(TipoEvento tipo, @Nullable Leituras leituras, boolean teste) {
        if (!podeIniciar()) return false;
        this.leituras = leituras;
        int segundos = prefs.contagemSeg();
        prepararEmergencia();
        publicar(new Estado(Fase.CONTANDO, segundos, tipo, teste, null));
        alarme.iniciar();
        Notificacoes.mostrar(contexto, Notificacoes.ID_EMERGENCIA, Notificacoes.contagem(contexto, segundos, teste));
        abrir(ContagemActivity.class);
        principal.postDelayed(tique, 1000);
        Log.i(TAG, "contagem iniciada tipo=" + tipo + " teste=" + teste + " seg=" + segundos);
        return true;
    }

    @MainThread
    public void dispararImediato(TipoEvento tipo, boolean teste) {
        if (!podeIniciar()) return;
        this.leituras = null;
        prepararEmergencia();
        publicar(new Estado(Fase.CONTANDO, 0, tipo, teste, null));
        enviarAgora();
    }

    private boolean podeIniciar() {
        Fase f = atual().fase;
        return f == Fase.OCIOSO || f == Fase.ENVIADO;
    }

    // botao estou bem
    @MainThread
    public void cancelar() {
        Estado e = atual();
        if (e.fase != Fase.CONTANDO) return;
        principal.removeCallbacks(tique);
        alarme.parar();
        Notificacoes.remover(contexto, Notificacoes.ID_EMERGENCIA);
        registrarCancelamento(e);
        publicar(new Estado(Fase.OCIOSO, 0, e.tipo, e.teste, null));
        liberarWakeLock();
        Bundle p = new Bundle();
        p.putString("tipo", e.tipo.name());
        analytics.logEvent("falso_positivo", p);
    }

    // envia o alerta (sms + api)
    @MainThread
    public void enviarAgora() {
        Estado e = atual();
        if (e.fase != Fase.CONTANDO) return;
        principal.removeCallbacks(tique);
        alarme.parar();
        publicar(new Estado(Fase.ENVIANDO, 0, e.tipo, e.teste, null));
        Leituras l = leituras;
        executor.execute(() -> {
            Evento evento = null;
            try {
                evento = despachante.disparar(e.tipo, l, e.teste);
            } catch (Exception ex) {
                Log.e(TAG, "erro inesperado no despacho", ex);
            }
            Evento finalEvento = evento;
            principal.post(() -> concluirEnvio(e, finalEvento));
        });
    }

    private void concluirEnvio(Estado anterior, @Nullable Evento evento) {
        publicar(new Estado(Fase.ENVIADO, 0, anterior.tipo, anterior.teste, evento));
        Notificacoes.mostrar(contexto, Notificacoes.ID_EMERGENCIA, Notificacoes.alertaEnviado(contexto, anterior.teste));
        abrir(AlertaEnviadoActivity.class);
        liberarWakeLock();
    }

    @MainThread
    public void encerrar() {
        Estado e = atual();
        if (e.fase != Fase.ENVIADO) return;
        String uid = usuarios.uid();
        if (uid != null && e.evento != null) {
            eventos.atualizar(uid, e.evento.id,
                    Collections.<String, Object>singletonMap("status", StatusEvento.ENCERRADO.name()));
        }
        Notificacoes.remover(contexto, Notificacoes.ID_EMERGENCIA);
        publicar(new Estado(Fase.OCIOSO, 0, e.tipo, e.teste, null));
    }

    private void registrarCancelamento(Estado e) {
        Leituras l = leituras;
        executor.execute(() -> {
            try {
                despachante.registrarNormal(e.tipo, l, e.teste);
            } catch (Exception ex) {
                Log.e(TAG, "erro ao registrar estou bem", ex);
            }
        });
    }

    private void prepararEmergencia() {
        adquirirWakeLock();
        try {
            Intent i = new Intent(contexto, ServicoMonitoramento.class).setAction(ServicoMonitoramento.ACAO_EMERGENCIA);
            ContextCompat.startForegroundService(contexto, i);
        } catch (Exception ex) {
            Log.w(TAG, "nao foi possivel iniciar o servico para a emergencia", ex);
        }
    }

    private void abrir(Class<?> tela) {
        try {
            contexto.startActivity(new Intent(contexto, tela).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception ex) {
            Log.w(TAG, "abertura da tela bloqueada: " + tela.getSimpleName());
        }
    }

    private void publicar(Estado e) {
        estado.setValue(e);
    }

    private void adquirirWakeLock() {
        if (wakeLock == null) {
            PowerManager pm = contexto.getSystemService(PowerManager.class);
            if (pm == null) return;
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Socorre:emergencia");
            wakeLock.setReferenceCounted(false);
        }
        wakeLock.acquire(WAKE_LOCK_MS);
    }

    private void liberarWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
    }
}
