package com.luistech.socorre.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.luistech.socorre.R;
import com.luistech.socorre.alert.ControladorEmergencia;
import com.luistech.socorre.data.local.Preferencias;
import com.luistech.socorre.data.repositorio.RepositorioTrajetos;
import com.luistech.socorre.domain.caso_de_uso.IniciarMonitoramento;
import com.luistech.socorre.domain.caso_de_uso.IniciarTrajeto;
import com.luistech.socorre.domain.modelo.Clima;
import com.luistech.socorre.domain.modelo.Localizacao;
import com.luistech.socorre.domain.modelo.PerfilAtividade;
import com.luistech.socorre.domain.modelo.TipoEvento;
import com.luistech.socorre.location.GerenciadorLocalizacao;
import com.luistech.socorre.service.ServicoMonitoramento;
import com.luistech.socorre.util.EventoUnico;
import com.luistech.socorre.weather.AvaliadorClima;
import com.luistech.socorre.weather.ServicoClima;

import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class InicioViewModel extends ViewModel {

    /** Resultado da checagem de clima antes do trajeto. */
    public static final class PreparoTrajeto {
        public final Clima clima;
        public final List<AvaliadorClima.Aviso> avisos;

        PreparoTrajeto(Clima clima, List<AvaliadorClima.Aviso> avisos) {
            this.clima = clima;
            this.avisos = avisos;
        }
    }

    /** Clima atual para o cartão da tela inicial: dados ou a mensagem do motivo de não ter. */
    public static final class EstadoClima {
        public final Clima clima;
        public final int mensagem;

        EstadoClima(Clima clima, int mensagem) {
            this.clima = clima;
            this.mensagem = mensagem;
        }
    }

    private final Preferencias prefs;
    private final IniciarMonitoramento monitoramento;
    private final IniciarTrajeto trajeto;
    private final ControladorEmergencia controlador;
    private final GerenciadorLocalizacao localizacao;
    private final ServicoClima clima;
    private final RepositorioTrajetos trajetos;
    private final ExecutorService executor;
    private final MutableLiveData<PerfilAtividade> perfil = new MutableLiveData<>();
    private final MutableLiveData<Boolean> preparando = new MutableLiveData<>(false);
    private final MutableLiveData<EventoUnico<Integer>> mensagem = new MutableLiveData<>();
    private final MutableLiveData<EventoUnico<PreparoTrajeto>> preparo = new MutableLiveData<>();
    private final MutableLiveData<EventoUnico<Boolean>> abrirTrajeto = new MutableLiveData<>();
    private final MutableLiveData<EstadoClima> climaAtual = new MutableLiveData<>(new EstadoClima(null, R.string.clima_carregando));
    private volatile boolean buscandoClima;

    @Inject
    public InicioViewModel(Preferencias prefs, IniciarMonitoramento monitoramento, IniciarTrajeto trajeto,
                           ControladorEmergencia controlador, GerenciadorLocalizacao localizacao,
                           ServicoClima clima, RepositorioTrajetos trajetos, ExecutorService executor) {
        this.prefs = prefs;
        this.monitoramento = monitoramento;
        this.trajeto = trajeto;
        this.controlador = controlador;
        this.localizacao = localizacao;
        this.clima = clima;
        this.trajetos = trajetos;
        this.executor = executor;
        perfil.setValue(prefs.perfil());
    }

    public LiveData<Boolean> monitorando() {
        return ServicoMonitoramento.monitorando();
    }

    public LiveData<ControladorEmergencia.Estado> emergencia() {
        return controlador.estado();
    }

    public LiveData<RepositorioTrajetos.Resumo> trajetoAoVivo() {
        return trajetos.resumoAoVivo();
    }

    public LiveData<PerfilAtividade> perfil() {
        return perfil;
    }

    public LiveData<Boolean> preparando() {
        return preparando;
    }

    public LiveData<EventoUnico<Integer>> mensagem() {
        return mensagem;
    }

    public LiveData<EventoUnico<PreparoTrajeto>> preparo() {
        return preparo;
    }

    public LiveData<EventoUnico<Boolean>> abrirTrajeto() {
        return abrirTrajeto;
    }

    public LiveData<EstadoClima> climaAtual() {
        return climaAtual;
    }

    /** Open-Meteo pela posição atual. O cache de 30 min evita chamadas repetidas ao voltar à tela. */
    public void atualizarClima() {
        if (buscandoClima) return;
        buscandoClima = true;
        executor.execute(() -> {
            EstadoClima resultado;
            Localizacao loc = localizacao.obterMelhorLocalizacao(5_000);
            if (loc == null) {
                resultado = new EstadoClima(null, R.string.clima_sem_localizacao);
            } else {
                Clima c = clima.obter(loc.lat, loc.lng);
                resultado = c != null ? new EstadoClima(c, 0) : new EstadoClima(null, R.string.clima_indisponivel);
            }
            climaAtual.postValue(resultado);
            buscandoClima = false;
        });
    }

    public boolean temContatos() {
        return !prefs.contatos().isEmpty();
    }

    public boolean trajetoAtivo() {
        return prefs.trajetoAtivoId() != null;
    }

    public void alternarMonitoramento(boolean ativoAgora) {
        if (ativoAgora) {
            monitoramento.pausar();
            return;
        }
        switch (monitoramento.executar()) {
            case SEM_CONTATOS:
                mensagem.setValue(new EventoUnico<>(R.string.erro_sem_contatos));
                break;
            case FALHOU:
                mensagem.setValue(new EventoUnico<>(R.string.erro_iniciar_monitoramento));
                break;
            case OK:
            default:
                break;
        }
    }

    public void definirPerfil(PerfilAtividade p) {
        if (p == perfil.getValue()) return;
        prefs.definirPerfil(p);
        perfil.setValue(p);
        monitoramento.atualizarPerfil();
    }

    /** Botão de pânico segurado por 3 s: envia na hora. */
    public void panico() {
        controlador.dispararImediato(TipoEvento.PANICO, false);
    }

    /** Busca localização e clima em segundo plano; falhas não impedem o trajeto. */
    public void prepararTrajeto() {
        if (!temContatos()) {
            mensagem.setValue(new EventoUnico<>(R.string.erro_sem_contatos));
            return;
        }
        preparando.setValue(true);
        executor.execute(() -> {
            Clima c = null;
            try {
                Localizacao loc = localizacao.obterMelhorLocalizacao(5_000);
                if (loc != null) c = clima.obter(loc.lat, loc.lng);
            } catch (Exception ignorada) {
                // clima é opcional
            }
            preparando.postValue(false);
            preparo.postValue(new EventoUnico<>(new PreparoTrajeto(c, AvaliadorClima.avaliar(c))));
        });
    }

    public void iniciarTrajeto(Clima c) {
        switch (trajeto.executar(c)) {
            case OK:
                abrirTrajeto.setValue(new EventoUnico<>(true));
                break;
            case SEM_CONTATOS:
                mensagem.setValue(new EventoUnico<>(R.string.erro_sem_contatos));
                break;
            default:
                mensagem.setValue(new EventoUnico<>(R.string.erro_generico));
                break;
        }
    }
}
