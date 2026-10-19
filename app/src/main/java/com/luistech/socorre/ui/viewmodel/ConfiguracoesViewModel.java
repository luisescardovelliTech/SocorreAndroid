package com.luistech.socorre.ui.viewmodel;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.work.WorkManager;

import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.luistech.socorre.R;
import com.luistech.socorre.alert.AgendadorCheckin;
import com.luistech.socorre.data.local.Preferencias;
import com.luistech.socorre.data.repositorio.RepositorioUsuario;
import com.luistech.socorre.domain.caso_de_uso.IniciarMonitoramento;
import com.luistech.socorre.domain.caso_de_uso.IniciarTrajeto;
import com.luistech.socorre.domain.modelo.PerfilAtividade;
import com.luistech.socorre.domain.modelo.Sensibilidade;
import com.luistech.socorre.util.EventoUnico;
import com.luistech.socorre.util.GerenciadorTema;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;

@HiltViewModel
public class ConfiguracoesViewModel extends ViewModel {

    private final Context contexto;
    private final Preferencias prefs;
    private final RepositorioUsuario usuarios;
    private final IniciarMonitoramento monitoramento;
    private final IniciarTrajeto trajeto;
    private final AgendadorCheckin checkin;
    private final MutableLiveData<EventoUnico<Integer>> mensagem = new MutableLiveData<>();
    private final MutableLiveData<EventoUnico<Boolean>> contaEncerrada = new MutableLiveData<>();
    private final MutableLiveData<Boolean> carregando = new MutableLiveData<>(false);

    @Inject
    public ConfiguracoesViewModel(@ApplicationContext Context contexto, Preferencias prefs, RepositorioUsuario usuarios,
                                  IniciarMonitoramento monitoramento, IniciarTrajeto trajeto, AgendadorCheckin checkin) {
        this.contexto = contexto;
        this.prefs = prefs;
        this.usuarios = usuarios;
        this.monitoramento = monitoramento;
        this.trajeto = trajeto;
        this.checkin = checkin;
    }

    public LiveData<EventoUnico<Integer>> mensagem() {
        return mensagem;
    }

    public LiveData<EventoUnico<Boolean>> contaEncerrada() {
        return contaEncerrada;
    }

    public LiveData<Boolean> carregando() {
        return carregando;
    }

    public PerfilAtividade perfil() {
        return prefs.perfil();
    }

    public Sensibilidade sensibilidade() {
        return prefs.sensibilidade(prefs.perfil());
    }

    public void definirSensibilidade(Sensibilidade s) {
        prefs.definirSensibilidade(prefs.perfil(), s);
        monitoramento.atualizarPerfil();
        sincronizar();
    }

    public int contagemSeg() {
        return prefs.contagemSeg();
    }

    public void definirContagemSeg(int seg) {
        prefs.definirContagemSeg(seg);
        sincronizar();
    }

    public int intervaloCheckin() {
        return prefs.intervaloCheckinMin();
    }

    public void definirIntervaloCheckin(int min) {
        trajeto.alterarIntervaloCheckin(min);
        sincronizar();
    }

    public String modoTema() {
        return prefs.modoTema();
    }

    public void definirModoTema(String modo) {
        prefs.definirModoTema(modo);
        GerenciadorTema.aplicar(modo);
        sincronizar();
    }

    public String mensagemSms() {
        String m = prefs.mensagemSmsPersonalizada();
        return m != null ? m : contexto.getString(R.string.sms_modelo_padrao);
    }

    public void salvarMensagemSms(String modelo) {
        if (modelo == null || modelo.trim().isEmpty()) {
            mensagem.setValue(new EventoUnico<>(R.string.erro_mensagem_vazia));
            return;
        }
        prefs.definirMensagemSms(modelo.trim());
        sincronizar();
        mensagem.setValue(new EventoUnico<>(R.string.msg_mensagem_salva));
    }

    public void restaurarMensagemSms() {
        prefs.definirMensagemSms(null);
        sincronizar();
    }

    private void sincronizar() {
        if (!usuarios.logado()) return;
        Map<String, Object> c = new HashMap<>();
        c.put("contagemSeg", prefs.contagemSeg());
        c.put("checkinMin", prefs.intervaloCheckinMin());
        c.put("modoTema", prefs.modoTema());
        c.put("mensagemSms", prefs.mensagemSmsPersonalizada());
        Map<String, Object> sens = new HashMap<>();
        for (PerfilAtividade p : PerfilAtividade.values()) sens.put(p.name(), prefs.sensibilidade(p).name());
        c.put("sensibilidade", sens);
        usuarios.salvarConfig(c);
    }

    private void encerrarSessaoLocal() {
        monitoramento.pausar();
        checkin.cancelarTudo();
        WorkManager.getInstance(contexto).cancelAllWorkByTag("api_eventos");
    }

    public void sair() {
        encerrarSessaoLocal();
        usuarios.sair();
        contaEncerrada.setValue(new EventoUnico<>(true));
    }

    public void excluirConta() {
        carregando.setValue(true);
        usuarios.excluirConta().addOnCompleteListener(t -> {
            carregando.setValue(false);
            if (t.isSuccessful()) {
                encerrarSessaoLocal();
                usuarios.sair();
                mensagem.setValue(new EventoUnico<>(R.string.msg_conta_excluida));
                contaEncerrada.setValue(new EventoUnico<>(true));
            } else if (t.getException() instanceof FirebaseAuthRecentLoginRequiredException) {
                mensagem.setValue(new EventoUnico<>(R.string.erro_reautenticar));
            } else {
                mensagem.setValue(new EventoUnico<>(R.string.erro_excluir_conta));
            }
        });
    }
}
