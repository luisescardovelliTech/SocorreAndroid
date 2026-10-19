package com.luistech.socorre.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.luistech.socorre.domain.modelo.ContatoEmergencia;
import com.luistech.socorre.domain.modelo.FichaMedica;
import com.luistech.socorre.domain.modelo.PerfilAtividade;
import com.luistech.socorre.domain.modelo.Sensibilidade;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class Preferencias {

    public static final int CONTAGEM_PADRAO_SEG = 30;
    public static final int CONTAGEM_MIN_SEG = 15;
    public static final int CONTAGEM_MAX_SEG = 60;

    public static final String TEMA_SISTEMA = "SISTEMA";
    public static final String TEMA_CLARO = "CLARO";
    public static final String TEMA_ESCURO = "ESCURO";
    public static final String TEMA_SENSOR = "SENSOR";

    private static final String K_ONBOARDING = "onboarding_concluido";
    private static final String K_CONSENT_LOCALIZACAO = "consentimento_localizacao_em";
    private static final String K_MONITORANDO = "monitoramento_ativo";
    private static final String K_PERFIL = "perfil_atividade";
    private static final String K_SENSIBILIDADE = "sensibilidade_";
    private static final String K_CONTAGEM = "contagem_seg";
    private static final String K_TEMA = "modo_tema";
    private static final String K_CHECKIN = "checkin_min";
    private static final String K_MENSAGEM = "mensagem_sms";
    private static final String K_NOME = "usuario_nome";
    private static final String K_TELEFONE = "usuario_telefone";
    private static final String K_CONTATOS = "contatos_json";
    private static final String K_FICHA = "ficha_json";
    private static final String K_TRAJETO = "trajeto_ativo_id";
    private static final String K_CHECKIN_PENDENTE = "checkin_pendente";
    private static final String K_CLIMA = "clima_cache_json";

    private final SharedPreferences sp;
    private final Gson gson = new Gson();

    @Inject
    public Preferencias(@ApplicationContext Context contexto) {
        sp = contexto.getSharedPreferences("socorre", Context.MODE_PRIVATE);
    }

    public boolean onboardingConcluido() {
        return sp.getBoolean(K_ONBOARDING, false);
    }

    public void concluirOnboarding(long consentimentoLocalizacaoEm) {
        sp.edit().putBoolean(K_ONBOARDING, true).putLong(K_CONSENT_LOCALIZACAO, consentimentoLocalizacaoEm).apply();
    }

    public long consentimentoLocalizacaoEm() {
        return sp.getLong(K_CONSENT_LOCALIZACAO, 0);
    }

    public boolean gatilhoBrusco() {
        return sp.getBoolean("gatilho_brusco", false);
    }

    public void definirGatilhoBrusco(boolean ativo) {
        sp.edit().putBoolean("gatilho_brusco", ativo).apply();
    }

    public boolean monitoramentoAtivo() {
        return sp.getBoolean(K_MONITORANDO, false);
    }

    public void definirMonitoramentoAtivo(boolean ativo) {
        sp.edit().putBoolean(K_MONITORANDO, ativo).apply();
    }

    public PerfilAtividade perfil() {
        return PerfilAtividade.deTexto(sp.getString(K_PERFIL, null));
    }

    public void definirPerfil(PerfilAtividade p) {
        sp.edit().putString(K_PERFIL, p.name()).apply();
    }

    public Sensibilidade sensibilidade(PerfilAtividade p) {
        return Sensibilidade.deTexto(sp.getString(K_SENSIBILIDADE + p.name(), null));
    }

    public void definirSensibilidade(PerfilAtividade p, Sensibilidade s) {
        sp.edit().putString(K_SENSIBILIDADE + p.name(), s.name()).apply();
    }

    public int contagemSeg() {
        int v = sp.getInt(K_CONTAGEM, CONTAGEM_PADRAO_SEG);
        return Math.max(CONTAGEM_MIN_SEG, Math.min(CONTAGEM_MAX_SEG, v));
    }

    public void definirContagemSeg(int seg) {
        sp.edit().putInt(K_CONTAGEM, seg).apply();
    }

    public String modoTema() {
        return sp.getString(K_TEMA, TEMA_SISTEMA);
    }

    public void definirModoTema(String modo) {
        sp.edit().putString(K_TEMA, modo).apply();
    }

    public int intervaloCheckinMin() {
        return sp.getInt(K_CHECKIN, 30);
    }

    public void definirIntervaloCheckinMin(int min) {
        sp.edit().putInt(K_CHECKIN, min).apply();
    }

    public String mensagemSmsPersonalizada() {
        return sp.getString(K_MENSAGEM, null);
    }

    public void definirMensagemSms(String modeloOuNulo) {
        sp.edit().putString(K_MENSAGEM, modeloOuNulo).apply();
    }

    public String nomeUsuario() {
        return sp.getString(K_NOME, "");
    }

    public String telefoneUsuario() {
        return sp.getString(K_TELEFONE, "");
    }

    public void salvarUsuario(String nome, String telefone) {
        sp.edit().putString(K_NOME, nome).putString(K_TELEFONE, telefone).apply();
    }

    public List<ContatoEmergencia> contatos() {
        String json = sp.getString(K_CONTATOS, null);
        if (json == null) return new ArrayList<>();
        List<ContatoEmergencia> lista = gson.fromJson(json, new TypeToken<List<ContatoEmergencia>>() { }.getType());
        return lista != null ? lista : new ArrayList<>();
    }

    public void salvarContatos(List<ContatoEmergencia> contatos) {
        sp.edit().putString(K_CONTATOS, gson.toJson(contatos)).apply();
    }

    public FichaMedica fichaMedica() {
        String json = sp.getString(K_FICHA, null);
        return json == null ? null : gson.fromJson(json, FichaMedica.class);
    }

    public void salvarFichaMedica(FichaMedica ficha) {
        sp.edit().putString(K_FICHA, ficha == null ? null : gson.toJson(ficha)).apply();
    }

    public String trajetoAtivoId() {
        return sp.getString(K_TRAJETO, null);
    }

    public void definirTrajetoAtivoId(String id) {
        sp.edit().putString(K_TRAJETO, id).putLong("trajeto_inicio_ms", System.currentTimeMillis()).apply();
    }

    public long trajetoInicioMs() {
        return sp.getLong("trajeto_inicio_ms", System.currentTimeMillis());
    }

    public boolean checkinPendente() {
        return sp.getBoolean(K_CHECKIN_PENDENTE, false);
    }

    public void definirCheckinPendente(boolean pendente) {
        sp.edit().putBoolean(K_CHECKIN_PENDENTE, pendente).apply();
    }

    public String climaCache() {
        return sp.getString(K_CLIMA, null);
    }

    public void salvarClimaCache(String json) {
        sp.edit().putString(K_CLIMA, json).apply();
    }

    public void limparDadosUsuario() {
        boolean onboarding = onboardingConcluido();
        long consentimento = consentimentoLocalizacaoEm();
        sp.edit().clear().apply();
        if (onboarding) concluirOnboarding(consentimento);
    }
}
