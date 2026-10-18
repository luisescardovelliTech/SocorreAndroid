package com.luistech.socorre.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.ListenerRegistration;
import com.luistech.socorre.R;
import com.luistech.socorre.alert.ControladorEmergencia;
import com.luistech.socorre.data.repositorio.RepositorioEventos;
import com.luistech.socorre.data.repositorio.RepositorioUsuario;
import com.luistech.socorre.domain.modelo.Evento;
import com.luistech.socorre.domain.modelo.Leituras;
import com.luistech.socorre.domain.modelo.TipoEvento;
import com.luistech.socorre.util.EventoUnico;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/** Contagem regressiva, alerta enviado e modo de teste. */
@HiltViewModel
public class EmergenciaViewModel extends ViewModel {

    private final ControladorEmergencia controlador;
    private final RepositorioUsuario usuarios;
    private final RepositorioEventos eventos;
    private final MutableLiveData<Evento> eventoAoVivo = new MutableLiveData<>();
    private final MutableLiveData<EventoUnico<Integer>> mensagem = new MutableLiveData<>();
    private ListenerRegistration ouvinte;
    private String eventoOuvido;

    @Inject
    public EmergenciaViewModel(ControladorEmergencia controlador, RepositorioUsuario usuarios, RepositorioEventos eventos) {
        this.controlador = controlador;
        this.usuarios = usuarios;
        this.eventos = eventos;
    }

    public LiveData<ControladorEmergencia.Estado> estado() {
        return controlador.estado();
    }

    public LiveData<Evento> eventoAoVivo() {
        return eventoAoVivo;
    }

    public LiveData<EventoUnico<Integer>> mensagem() {
        return mensagem;
    }

    public void estouBem() {
        controlador.cancelar();
    }

    public void pedirAjudaAgora() {
        controlador.enviarAgora();
    }

    public void encerrar() {
        controlador.encerrar();
    }

    /** Acompanha o documento do evento para mostrar confirmações de SMS e tentativas da API. */
    public void acompanhar(Evento evento) {
        if (evento == null || evento.id == null) return;
        eventoAoVivo.setValue(evento);
        String uid = usuarios.uid();
        if (uid == null || evento.id.equals(eventoOuvido)) return;
        eventoOuvido = evento.id;
        if (ouvinte != null) ouvinte.remove();
        ouvinte = eventos.ouvir(uid, evento.id, (d, e) -> {
            if (d == null || !d.exists()) return;
            Evento atualizado = d.toObject(Evento.class);
            if (atualizado != null) {
                atualizado.id = d.getId();
                eventoAoVivo.setValue(atualizado);
            }
        });
    }

    /** Modo de teste: nada real é enviado (SMS simulado; o registro vai para a API Socorro). */
    public void simular(TipoEvento tipo) {
        Leituras l = null;
        if (tipo == TipoEvento.QUEDA) {
            l = new Leituras();
            l.picoG = 4.3;
            l.rotacaoGraus = 85;
        }
        if (!controlador.iniciarContagem(tipo, l, true)) {
            mensagem.setValue(new EventoUnico<>(R.string.erro_emergencia_em_andamento));
        }
    }

    @Override
    protected void onCleared() {
        if (ouvinte != null) ouvinte.remove();
    }
}
