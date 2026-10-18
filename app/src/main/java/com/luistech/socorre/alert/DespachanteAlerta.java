package com.luistech.socorre.alert;

import android.content.Context;
import android.os.BatteryManager;
import android.os.Build;
import android.provider.Settings;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.firestore.FieldValue;
import com.luistech.socorre.R;
import com.luistech.socorre.data.local.Preferencias;
import com.luistech.socorre.data.repositorio.RepositorioEventos;
import com.luistech.socorre.data.repositorio.RepositorioUsuario;
import com.luistech.socorre.domain.modelo.ContatoEmergencia;
import com.luistech.socorre.domain.modelo.Envios;
import com.luistech.socorre.domain.modelo.Evento;
import com.luistech.socorre.domain.modelo.FichaMedica;
import com.luistech.socorre.domain.modelo.Leituras;
import com.luistech.socorre.domain.modelo.Localizacao;
import com.luistech.socorre.domain.modelo.StatusEnvio;
import com.luistech.socorre.domain.modelo.StatusEvento;
import com.luistech.socorre.domain.modelo.TipoEvento;
import com.luistech.socorre.detection.LeitorPressao;
import com.luistech.socorre.location.GerenciadorLocalizacao;
import com.luistech.socorre.weather.ServicoClima;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Executa o alerta: localização → evento → SMS → API Socorro (fila).
 * Cada canal é isolado em try/catch e tem status próprio: a falha de um nunca impede os outros.
 * O SMS usa apenas dados locais (Preferencias), então funciona sem internet.
 */
@Singleton
public class DespachanteAlerta {

    private static final String TAG = "Socorre.Alerta";
    /** Prazo do GPS para cumprir o RNF02 (SMS em até 10 s). */
    static final long PRAZO_GPS_MS = 7_000;
    static final long PRAZO_PRESSAO_MS = 1_000;

    private final Context contexto;
    private final Preferencias prefs;
    private final GerenciadorLocalizacao localizacao;
    private final RepositorioUsuario usuarios;
    private final RepositorioEventos eventos;
    private final EnviadorSms sms;
    private final EnviadorApi api;
    private final FirebaseAnalytics analytics;
    private final ServicoClima clima;

    @Inject
    public DespachanteAlerta(@ApplicationContext Context contexto, Preferencias prefs,
                             GerenciadorLocalizacao localizacao, RepositorioUsuario usuarios,
                             RepositorioEventos eventos, EnviadorSms sms, EnviadorApi api,
                             FirebaseAnalytics analytics, ServicoClima clima) {
        this.contexto = contexto;
        this.prefs = prefs;
        this.localizacao = localizacao;
        this.usuarios = usuarios;
        this.eventos = eventos;
        this.sms = sms;
        this.api = api;
        this.analytics = analytics;
        this.clima = clima;
    }

    /** Bloqueante (GPS + SMS). Chamar fora da thread principal. Devolve o evento criado. */
    @WorkerThread
    public Evento disparar(TipoEvento tipo, @Nullable Leituras leituras, boolean teste) {
        String uid = usuarios.uid();
        Evento e = new Evento();
        e.tipo = tipo.name();
        e.status = StatusEvento.ALERTA_ENVIADO.name();
        e.dataHora = new Date();
        e.leituras = leituras;
        e.teste = teste;
        e.perfilAtividade = prefs.perfil().name();
        e.trajetoId = prefs.trajetoAtivoId();
        e.bateriaPct = bateria();

        // 1-3. Localização (melhor esforço), horário e precisão
        try {
            Localizacao loc = localizacao.obterMelhorLocalizacao(PRAZO_GPS_MS);
            e.localizacao = loc;
            e.precisaoM = loc != null ? loc.precisaoM : null;
        } catch (Exception ex) {
            registrarFalha("localizacao", ex);
        }

        // 4-5. Evento salvo antes dos envios (Firestore offline grava no cache local)
        e.id = uid != null ? eventos.novoId(uid) : "local-" + e.dataHora.getTime();
        String mensagem = montarMensagem(tipo, e);
        List<ContatoEmergencia> contatos = prefs.contatos();
        if (uid != null) {
            try {
                eventos.salvar(uid, e);
            } catch (Exception ex) {
                registrarFalha("firestore", ex);
            }
        }

        // 6. SMS - primeiro canal, independente de internet
        try {
            if (teste) {
                e.envios.sms.status = StatusEnvio.SIMULADO;
                for (ContatoEmergencia c : contatos) e.envios.sms.enviadosPara.add(c.telefone);
            } else {
                e.envios.sms = sms.enviar(contatos, mensagem, uid, e.id);
            }
        } catch (Exception ex) {
            e.envios.sms.status = StatusEnvio.FALHOU;
            registrarFalha("sms", ex);
        }

        // Montado só depois do SMS: a leitura do barômetro não pode atrasar o alerta.
        String payload = null;
        try {
            payload = montarPayload(e, PayloadApi.REGISTRO_QUEDA);
            if (teste) e.previaTeste = previa(mensagem, contatos, payload);
        } catch (Exception ex) {
            registrarFalha("payload", ex);
        }

        // 7. API externa - apenas enfileira; nunca atrasa o SMS
        try {
            // Também no modo de teste: a API Socorro recebe o registro (só o SMS é simulado).
            if (uid != null && payload != null) {
                api.enfileirar(uid, e.id, payload);
            } else {
                e.envios.api.status = StatusEnvio.NAO_CONFIGURADO;
            }
        } catch (Exception ex) {
            e.envios.api.status = StatusEnvio.FALHOU;
            e.envios.api.ultimoErro = ex.getClass().getSimpleName();
            registrarFalha("api", ex);
        }

        // 8. Resultado de cada canal
        if (uid != null) {
            try {
                Map<String, Object> campos = new HashMap<>();
                // Campos separados: as confirmações da operadora (arrayUnion) não são sobrescritas.
                campos.put("envios.sms.status", e.envios.sms.status);
                campos.put("envios.sms.enviadosPara", e.envios.sms.enviadosPara);
                if (!e.envios.sms.falhas.isEmpty()) {
                    campos.put("envios.sms.falhas", FieldValue.arrayUnion(e.envios.sms.falhas.toArray()));
                }
                if (e.previaTeste != null) campos.put("previaTeste", e.previaTeste);
                if (!StatusEnvio.PENDENTE.equals(e.envios.api.status)) {
                    campos.put("envios.api.status", e.envios.api.status);
                }
                eventos.atualizar(uid, e.id, campos);
            } catch (Exception ex) {
                registrarFalha("firestore_envios", ex);
            }
        }

        Bundle p = new Bundle();
        p.putString("tipo", e.tipo);
        p.putString("teste", String.valueOf(teste));
        p.putString("sms", e.envios.sms.status);
        analytics.logEvent("alerta_disparado", p);
        Log.i(TAG, "alerta evento=" + e.id + " tipo=" + e.tipo + " teste=" + teste
                + " sms=" + e.envios.sms.status + " gps=" + (e.localizacao != null));
        return e;
    }

    /** "ESTOU BEM": salva o evento com status NORMAL e envia à API como rotina (tipo_registro 1). Sem SMS. */
    @WorkerThread
    public void registrarNormal(TipoEvento tipo, @Nullable Leituras leituras, boolean teste) {
        String uid = usuarios.uid();
        if (uid == null) return;
        Evento e = new Evento();
        e.id = eventos.novoId(uid);
        e.tipo = tipo.name();
        e.status = StatusEvento.NORMAL.name();
        e.dataHora = new Date();
        e.leituras = leituras;
        e.teste = teste;
        e.perfilAtividade = prefs.perfil().name();
        e.trajetoId = prefs.trajetoAtivoId();
        e.bateriaPct = bateria();
        e.envios.sms = null; // nenhum SMS: estava tudo bem
        try {
            e.localizacao = localizacao.obterMelhorLocalizacao(PRAZO_GPS_MS);
            e.precisaoM = e.localizacao != null ? e.localizacao.precisaoM : null;
        } catch (Exception ex) {
            registrarFalha("localizacao", ex);
        }
        try {
            eventos.salvar(uid, e);
        } catch (Exception ex) {
            registrarFalha("firestore", ex);
        }
        try {
            api.enfileirar(uid, e.id, montarPayload(e, PayloadApi.REGISTRO_ROTINA));
        } catch (Exception ex) {
            registrarFalha("api", ex);
        }
    }

    String montarMensagem(TipoEvento tipo, Evento e) {
        String modelo = prefs.mensagemSmsPersonalizada();
        if (modelo == null || modelo.trim().isEmpty()) modelo = contexto.getString(R.string.sms_modelo_padrao);
        Map<String, String> valores = new HashMap<>();
        String nome = prefs.nomeUsuario();
        valores.put("nome", nome.isEmpty() ? contexto.getString(R.string.sms_usuario_sem_nome) : nome);
        valores.put("situacao", contexto.getString(situacao(tipo)));
        valores.put("tipo", contexto.getString(nomeTipo(tipo)));
        valores.put("dataHora", new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("pt", "BR")).format(e.dataHora));
        valores.put("localizacao", e.localizacao != null
                ? contexto.getString(R.string.sms_localizacao, e.localizacao.linkMapa(), Math.round(e.localizacao.precisaoM))
                : contexto.getString(R.string.sms_localizacao_indisponivel));
        String ficha = null;
        FichaMedica f = prefs.fichaMedica();
        if (f != null && f.compartilharNosAlertas) {
            ficha = MontadorMensagem.resumirFicha(contexto.getString(R.string.sms_ficha_rotulo),
                    f.tipoSanguineo, f.alergias, f.medicamentos, f.condicoes);
        }
        return MontadorMensagem.montar(modelo, valores, ficha);
    }

    /** Corpo da API Socorro; pressão lida na hora e temperatura do cache do clima. */
    private String montarPayload(Evento e, int tipoRegistro) {
        Float pressao = null;
        try {
            pressao = LeitorPressao.lerHpa(contexto, PRAZO_PRESSAO_MS);
        } catch (Exception ex) {
            registrarFalha("barometro", ex);
        }
        String deviceId = Settings.Secure.getString(contexto.getContentResolver(), Settings.Secure.ANDROID_ID);
        return PayloadApi.montar(e, prefs.nomeUsuario(), deviceId, Build.MANUFACTURER + " " + Build.MODEL,
                pressao, clima.temperaturaRecente(), tipoRegistro);
    }

    private Map<String, Object> previa(String mensagem, List<ContatoEmergencia> contatos, String payload) {
        Map<String, Object> m = new HashMap<>();
        m.put("sms", mensagem);
        List<String> dest = new ArrayList<>();
        for (ContatoEmergencia c : contatos) dest.add(c.nome + " - " + c.telefone);
        m.put("destinatarios", dest);
        m.put("payload", payload);
        return m;
    }

    public static int nomeTipo(TipoEvento t) {
        switch (t) {
            case PANICO:
                return R.string.tipo_panico;
            case CHECKIN_SEM_RESPOSTA:
                return R.string.tipo_checkin;
            case QUEDA:
            default:
                return R.string.tipo_queda;
        }
    }

    private static int situacao(TipoEvento t) {
        switch (t) {
            case PANICO:
                return R.string.situacao_panico;
            case CHECKIN_SEM_RESPOSTA:
                return R.string.situacao_checkin;
            case QUEDA:
            default:
                return R.string.situacao_queda;
        }
    }

    private Integer bateria() {
        BatteryManager bm = contexto.getSystemService(BatteryManager.class);
        if (bm == null) return null;
        int v = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        return v > 0 ? v : null;
    }

    private void registrarFalha(String canal, Exception ex) {
        Log.e(TAG, "falha no canal " + canal, ex);
        try {
            FirebaseCrashlytics.getInstance().recordException(ex);
        } catch (Exception ignorada) {
            // Crashlytics indisponível (Firebase não configurado)
        }
    }
}
