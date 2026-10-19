package com.luistech.socorre.service;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;
import com.luistech.socorre.alert.AgendadorCheckin;
import com.luistech.socorre.alert.ControladorEmergencia;
import com.luistech.socorre.data.local.Preferencias;
import com.luistech.socorre.data.repositorio.RepositorioTrajetos;
import com.luistech.socorre.data.repositorio.RepositorioUsuario;
import com.luistech.socorre.detection.ColetorSensores;
import com.luistech.socorre.detection.DetectorQueda;
import com.luistech.socorre.detection.ProvedorLimiares;
import com.luistech.socorre.domain.modelo.PerfilAtividade;
import com.luistech.socorre.domain.modelo.TipoEvento;
import com.luistech.socorre.location.GerenciadorLocalizacao;
import com.luistech.socorre.security.GerenciadorPermissoes;
import com.luistech.socorre.util.Notificacoes;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint(Service.class)
public class ServicoMonitoramento extends Hilt_ServicoMonitoramento {

    public static final String ACAO_INICIAR = "com.luistech.socorre.MONITORAR";
    public static final String ACAO_PARAR = "com.luistech.socorre.PAUSAR";
    public static final String ACAO_INICIAR_TRAJETO = "com.luistech.socorre.INICIAR_TRAJETO";
    public static final String ACAO_PARAR_TRAJETO = "com.luistech.socorre.PARAR_TRAJETO";
    public static final String ACAO_PANICO = "com.luistech.socorre.PANICO";
    public static final String ACAO_EMERGENCIA = "com.luistech.socorre.EMERGENCIA";
    public static final String ACAO_ATUALIZAR_PERFIL = "com.luistech.socorre.ATUALIZAR_PERFIL";

    private static final String TAG = "Socorre.Servico";
    private static final int PONTOS_POR_GRAVACAO = 15;
    private static final float PRECISAO_MAX_TRAJETO_M = 30;

    private static final MutableLiveData<Boolean> MONITORANDO = new MutableLiveData<>(false);
    private static volatile boolean emExecucao;

    @Inject
    Preferencias prefs;
    @Inject
    ControladorEmergencia controlador;
    @Inject
    ProvedorLimiares limiares;
    @Inject
    GerenciadorLocalizacao localizacao;
    @Inject
    RepositorioTrajetos trajetos;
    @Inject
    RepositorioUsuario usuarios;
    @Inject
    AgendadorCheckin checkin;

    private final Handler principal = new Handler(Looper.getMainLooper());
    private HandlerThread threadSensores;
    private Handler handlerSensores;
    private ColetorSensores coletor;
    private PowerManager.WakeLock wakeLock;
    private boolean monitorando;
    private ControladorEmergencia.Fase faseAnterior = ControladorEmergencia.Fase.OCIOSO;

    private String trajetoId;
    private long trajetoInicioMs;
    private final List<LatLng> pontos = new ArrayList<>();
    private Location ultimaPosicao;
    private double distanciaM;
    private boolean recebendoGps;
    private double ultimaVelocidadeKmh;
    private long ultimoFixMs;
    // atualiza o tempo do trajeto a cada 1s
    private final Runnable relogioTrajeto = new Runnable() {
        @Override
        public void run() {
            if (trajetoId == null) return;
            boolean fixRecente = System.currentTimeMillis() - ultimoFixMs < 15_000;
            publicarResumo(fixRecente ? ultimaVelocidadeKmh : 0);
            handlerSensores.postDelayed(this, 1000);
        }
    };

    public static LiveData<Boolean> monitorando() {
        return MONITORANDO;
    }

    public static void enviar(Context c, String acao) {
        Intent i = new Intent(c, ServicoMonitoramento.class).setAction(acao);
        if (ACAO_ATUALIZAR_PERFIL.equals(acao)) {
            if (emExecucao) c.startService(i);
            return;
        }
        ContextCompat.startForegroundService(c, i);
    }

    private final Observer<ControladorEmergencia.Estado> observadorEmergencia = e -> {
        boolean terminou = (faseAnterior != ControladorEmergencia.Fase.OCIOSO && e.fase == ControladorEmergencia.Fase.OCIOSO)
                || (faseAnterior != ControladorEmergencia.Fase.ENVIADO && e.fase == ControladorEmergencia.Fase.ENVIADO);
        faseAnterior = e.fase;
        if (terminou && handlerSensores != null) {
            handlerSensores.post(() -> {
                if (coletor != null) coletor.detector().iniciarSilencio();
            });
        }
        if (terminou) {
            atualizarNotificacao();
            if (e.fase == ControladorEmergencia.Fase.OCIOSO) pararSeOcioso();
        }
    };

    private final LocationCallback callbackLocalizacao = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult resultado) {
            for (Location l : resultado.getLocations()) registrarPosicao(l);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        emExecucao = true;
        threadSensores = new HandlerThread("socorre-sensores");
        threadSensores.start();
        handlerSensores = new Handler(threadSensores.getLooper());
        controlador.estado().observeForever(observadorEmergencia);
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        entrarEmPrimeiroPlano();
        String acao = intent != null ? intent.getAction() : null;
        Log.i(TAG, "acao=" + acao);
        if (acao == null) {
            if (prefs.monitoramentoAtivo()) iniciarMonitoramento();
            if (prefs.trajetoAtivoId() != null) iniciarTrajeto();
        } else {
            switch (acao) {
                case ACAO_INICIAR:
                    iniciarMonitoramento();
                    break;
                case ACAO_PARAR:
                    pararTrajeto();
                    pararMonitoramento();
                    break;
                case ACAO_INICIAR_TRAJETO:
                    iniciarMonitoramento();
                    iniciarTrajeto();
                    break;
                case ACAO_PARAR_TRAJETO:
                    pararTrajeto();
                    break;
                case ACAO_PANICO:
                    controlador.iniciarContagem(TipoEvento.PANICO, null, false);
                    break;
                case ACAO_ATUALIZAR_PERFIL:
                    aplicarPerfil();
                    break;
                case ACAO_EMERGENCIA:
                default:
                    break;
            }
        }
        atualizarNotificacao();
        pararSeOcioso();
        return START_STICKY;
    }

    private void entrarEmPrimeiroPlano() {
        Notification n = Notificacoes.monitoramento(this, monitorando, trajetoId != null, emergenciaAtiva());
        int tipo = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && GerenciadorPermissoes.temLocalizacao(this)) {
            tipo |= ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;
        }
        if (Build.VERSION.SDK_INT >= 34) tipo |= ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE;
        try {
            ServiceCompat.startForeground(this, Notificacoes.ID_MONITORAMENTO, n, tipo);
        } catch (Exception e) {
            Log.w(TAG, "startForeground com localizacao negado, tentando sem: " + e.getClass().getSimpleName());
            int semLocalizacao = Build.VERSION.SDK_INT >= 34 ? ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE : 0;
            ServiceCompat.startForeground(this, Notificacoes.ID_MONITORAMENTO, n, semLocalizacao);
        }
    }

    // liga os sensores e comeca o monitoramento
    private void iniciarMonitoramento() {
        if (monitorando) return;
        SensorManager sm = getSystemService(SensorManager.class);
        DetectorQueda detector = new DetectorQueda(perfilAtual());
        coletor = new ColetorSensores(sm, detector, (leituras, brusco) ->
                principal.post(() -> controlador.iniciarContagem(TipoEvento.QUEDA, leituras, brusco)));
        coletor.usarModoBrusco(prefs::gatilhoBrusco);
        if (!coletor.iniciar(handlerSensores)) {
            Log.e(TAG, "aparelho sem acelerometro: monitoramento indisponivel");
            coletor = null;
            return;
        }
        PowerManager pm = getSystemService(PowerManager.class);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Socorre:monitoramento");
            wakeLock.acquire();
        }
        monitorando = true;
        prefs.definirMonitoramentoAtivo(true);
        MONITORANDO.postValue(true);
    }

    private void pararMonitoramento() {
        if (coletor != null) coletor.parar();
        coletor = null;
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        wakeLock = null;
        monitorando = false;
        prefs.definirMonitoramentoAtivo(false);
        MONITORANDO.postValue(false);
    }

    private void aplicarPerfil() {
        if (coletor == null) return;
        DetectorQueda d = coletor.detector();
        com.luistech.socorre.detection.PerfilDeteccao p = perfilAtual();
        handlerSensores.post(() -> d.atualizarPerfil(p));
    }

    private com.luistech.socorre.detection.PerfilDeteccao perfilAtual() {
        PerfilAtividade perfil = prefs.perfil();
        return limiares.perfil(perfil, prefs.sensibilidade(perfil));
    }

    private void iniciarTrajeto() {
        String id = prefs.trajetoAtivoId();
        if (id == null || id.equals(trajetoId)) return;
        trajetoId = id;
        trajetoInicioMs = prefs.trajetoInicioMs();
        handlerSensores.post(() -> {
            pontos.clear();
            distanciaM = 0;
            ultimaPosicao = null;
        });
        recebendoGps = localizacao.iniciarAtualizacoes(callbackLocalizacao, threadSensores.getLooper());
        ultimaVelocidadeKmh = 0;
        ultimoFixMs = 0;
        handlerSensores.removeCallbacks(relogioTrajeto);
        handlerSensores.post(relogioTrajeto);
    }

    private void pararTrajeto() {
        checkin.cancelarTudo();
        prefs.definirTrajetoAtivoId(null);
        prefs.definirCheckinPendente(false);
        trajetos.publicarResumo(null);
        if (trajetoId == null) return;
        localizacao.pararAtualizacoes(callbackLocalizacao);
        handlerSensores.removeCallbacks(relogioTrajeto);
        String id = trajetoId;
        trajetoId = null;
        handlerSensores.post(() -> gravarTrajeto(id, new Date()));
    }

    // guarda cada posicao do gps no trajeto
    private void registrarPosicao(Location l) {
        if (trajetoId == null) return;
        if (l.hasSpeed() && coletor != null) {
            coletor.detector().atualizarVelocidade(l.getElapsedRealtimeNanos() / 1_000_000L, l.getSpeed() * 3.6);
        }
        ultimoFixMs = System.currentTimeMillis();
        ultimaVelocidadeKmh = l.hasSpeed() ? l.getSpeed() * 3.6 : 0;
        if (l.getAccuracy() > PRECISAO_MAX_TRAJETO_M) return;
        if (ultimaPosicao != null) distanciaM += ultimaPosicao.distanceTo(l);
        ultimaPosicao = l;
        pontos.add(new LatLng(l.getLatitude(), l.getLongitude()));
        publicarResumo(l.hasSpeed() ? l.getSpeed() * 3.6 : 0);
        if (pontos.size() % PONTOS_POR_GRAVACAO == 0) gravarTrajeto(trajetoId, null);
    }

    private void gravarTrajeto(String id, @Nullable Date fim) {
        String uid = usuarios.uid();
        if (uid == null) return;
        long duracao = Math.max(1, (System.currentTimeMillis() - trajetoInicioMs) / 1000);
        trajetos.atualizar(uid, id, PolyUtil.encode(pontos), distanciaM, duracao,
                distanciaM / duracao * 3.6, fim);
    }

    private void publicarResumo(double velocidadeKmh) {
        String id = trajetoId;
        if (id == null) return;
        long duracao = Math.max(1, (System.currentTimeMillis() - trajetoInicioMs) / 1000);
        boolean gps = recebendoGps && System.currentTimeMillis() - ultimoFixMs < 15_000;
        trajetos.publicarResumo(new RepositorioTrajetos.Resumo(id, distanciaM, duracao, velocidadeKmh,
                distanciaM / duracao * 3.6, gps));
    }

    private boolean emergenciaAtiva() {
        return controlador.atual().fase != ControladorEmergencia.Fase.OCIOSO;
    }

    private void atualizarNotificacao() {
        Notificacoes.mostrar(this, Notificacoes.ID_MONITORAMENTO,
                Notificacoes.monitoramento(this, monitorando, trajetoId != null, emergenciaAtiva()));
    }

    private void pararSeOcioso() {
        if (!monitorando && trajetoId == null && !emergenciaAtiva()) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        controlador.estado().removeObserver(observadorEmergencia);
        if (trajetoId != null) localizacao.pararAtualizacoes(callbackLocalizacao);
        if (coletor != null) coletor.parar();
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        threadSensores.quitSafely();
        emExecucao = false;
        MONITORANDO.postValue(false);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
