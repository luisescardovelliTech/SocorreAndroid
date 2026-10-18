package com.luistech.socorre.domain.caso_de_uso;

import android.content.Context;

import com.luistech.socorre.data.local.Preferencias;
import com.luistech.socorre.service.ServicoMonitoramento;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;

/** RF09: o monitoramento só começa com pelo menos um contato de emergência. */
public class IniciarMonitoramento {

    public enum Resultado { OK, SEM_CONTATOS, FALHOU }

    private final Context contexto;
    private final Preferencias prefs;

    @Inject
    public IniciarMonitoramento(@ApplicationContext Context contexto, Preferencias prefs) {
        this.contexto = contexto;
        this.prefs = prefs;
    }

    public Resultado executar() {
        if (prefs.contatos().isEmpty()) return Resultado.SEM_CONTATOS;
        try {
            ServicoMonitoramento.enviar(contexto, ServicoMonitoramento.ACAO_INICIAR);
            return Resultado.OK;
        } catch (Exception e) {
            return Resultado.FALHOU;
        }
    }

    public void pausar() {
        ServicoMonitoramento.enviar(contexto, ServicoMonitoramento.ACAO_PARAR);
    }

    /** Reaplica perfil/sensibilidade no detector, se o serviço estiver rodando. */
    public void atualizarPerfil() {
        ServicoMonitoramento.enviar(contexto, ServicoMonitoramento.ACAO_ATUALIZAR_PERFIL);
    }
}
