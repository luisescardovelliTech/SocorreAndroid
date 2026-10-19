package com.luistech.socorre.domain.caso_de_uso;

import android.content.Context;

import androidx.annotation.Nullable;

import com.luistech.socorre.alert.AgendadorCheckin;
import com.luistech.socorre.data.local.Preferencias;
import com.luistech.socorre.data.repositorio.RepositorioTrajetos;
import com.luistech.socorre.data.repositorio.RepositorioUsuario;
import com.luistech.socorre.domain.modelo.Clima;
import com.luistech.socorre.domain.modelo.Trajeto;
import com.luistech.socorre.service.ServicoMonitoramento;

import java.util.Date;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;

public class IniciarTrajeto {

    public enum Resultado { OK, SEM_CONTATOS, SEM_LOGIN }

    private final Context contexto;
    private final Preferencias prefs;
    private final RepositorioUsuario usuarios;
    private final RepositorioTrajetos trajetos;
    private final AgendadorCheckin checkin;

    @Inject
    public IniciarTrajeto(@ApplicationContext Context contexto, Preferencias prefs, RepositorioUsuario usuarios,
                          RepositorioTrajetos trajetos, AgendadorCheckin checkin) {
        this.contexto = contexto;
        this.prefs = prefs;
        this.usuarios = usuarios;
        this.trajetos = trajetos;
        this.checkin = checkin;
    }

    public Resultado executar(@Nullable Clima clima) {
        if (prefs.contatos().isEmpty()) return Resultado.SEM_CONTATOS;
        String uid = usuarios.uid();
        if (uid == null) return Resultado.SEM_LOGIN;
        Trajeto t = new Trajeto();
        t.id = trajetos.novoId(uid);
        t.inicio = new Date();
        t.perfilAtividade = prefs.perfil().name();
        t.climaInicio = clima;
        trajetos.salvar(uid, t);
        prefs.definirTrajetoAtivoId(t.id);
        checkin.agendarPergunta(prefs.intervaloCheckinMin());
        ServicoMonitoramento.enviar(contexto, ServicoMonitoramento.ACAO_INICIAR_TRAJETO);
        return Resultado.OK;
    }

    public void encerrar() {
        ServicoMonitoramento.enviar(contexto, ServicoMonitoramento.ACAO_PARAR_TRAJETO);
    }

    public void alterarIntervaloCheckin(int minutos) {
        prefs.definirIntervaloCheckinMin(minutos);
        if (prefs.trajetoAtivoId() == null) return;
        checkin.cancelarTudo();
        checkin.agendarPergunta(minutos);
    }
}
