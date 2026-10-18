package com.luistech.socorre.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.luistech.socorre.R;
import com.luistech.socorre.alert.EnviadorSms;
import com.luistech.socorre.data.local.Preferencias;
import com.luistech.socorre.data.repositorio.RepositorioUsuario;
import com.luistech.socorre.domain.caso_de_uso.ValidadorTelefone;
import com.luistech.socorre.domain.modelo.ContatoEmergencia;
import com.luistech.socorre.util.EventoUnico;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/** Lista de 1 a 5 contatos. Cada alteração é gravada na hora (nuvem + cache local do SMS). */
@HiltViewModel
public class ContatosViewModel extends ViewModel {

    private final RepositorioUsuario repo;
    private final Preferencias prefs;
    private final EnviadorSms sms;
    private final ExecutorService executor;
    private final MutableLiveData<List<ContatoEmergencia>> contatos = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<EventoUnico<Integer>> mensagem = new MutableLiveData<>();

    @Inject
    public ContatosViewModel(RepositorioUsuario repo, Preferencias prefs, EnviadorSms sms, ExecutorService executor) {
        this.repo = repo;
        this.prefs = prefs;
        this.sms = sms;
        this.executor = executor;
        contatos.setValue(prefs.contatos()); // mostra o cache na hora
        repo.carregarContatos().addOnSuccessListener(contatos::setValue);
    }

    public LiveData<List<ContatoEmergencia>> contatos() {
        return contatos;
    }

    public LiveData<EventoUnico<Integer>> mensagem() {
        return mensagem;
    }

    public boolean podeAdicionar() {
        return lista().size() < ContatoEmergencia.MAXIMO;
    }

    /**
     * Valida e grava (indice -1 = novo).
     * @return recurso de erro do telefone/nome, ou 0 se gravou.
     */
    public int salvar(int indice, String nome, String telefone, String relacao) {
        if (nome == null || nome.trim().isEmpty()) return R.string.erro_nome_obrigatorio;
        String tel = ValidadorTelefone.normalizar(telefone);
        if (tel == null) return R.string.erro_telefone_invalido;
        List<ContatoEmergencia> l = lista();
        if (indice < 0) {
            if (l.size() >= ContatoEmergencia.MAXIMO) return R.string.erro_maximo_contatos;
            l.add(new ContatoEmergencia(nome.trim(), tel, relacao == null ? "" : relacao.trim(), l.size() + 1));
        } else {
            ContatoEmergencia c = l.get(indice);
            c.nome = nome.trim();
            c.telefone = tel;
            c.relacao = relacao == null ? "" : relacao.trim();
        }
        gravar(l, Collections.<String>emptyList());
        return 0;
    }

    public void remover(int indice) {
        List<ContatoEmergencia> l = lista();
        ContatoEmergencia c = l.remove(indice);
        gravar(l, c.id != null ? Collections.singletonList(c.id) : Collections.<String>emptyList());
    }

    /** Muda a prioridade (delta -1 = subir, +1 = descer). */
    public void mover(int indice, int delta) {
        List<ContatoEmergencia> l = lista();
        int destino = indice + delta;
        if (destino < 0 || destino >= l.size()) return;
        Collections.swap(l, indice, destino);
        gravar(l, Collections.<String>emptyList());
    }

    public void testarSms(ContatoEmergencia c, String mensagemTeste) {
        executor.execute(() -> {
            boolean ok = sms.enviarTeste(c.telefone, mensagemTeste);
            mensagem.postValue(new EventoUnico<>(ok ? R.string.msg_sms_teste_enviado : R.string.erro_sms_teste));
        });
    }

    private List<ContatoEmergencia> lista() {
        List<ContatoEmergencia> atual = contatos.getValue();
        return atual == null ? new ArrayList<>() : new ArrayList<>(atual);
    }

    private void gravar(List<ContatoEmergencia> l, List<String> removidos) {
        contatos.setValue(l);
        repo.salvarContatos(l, removidos).addOnFailureListener(e ->
                mensagem.setValue(new EventoUnico<>(R.string.erro_salvar_nuvem)));
    }

    public String nomeUsuario() {
        return prefs.nomeUsuario();
    }
}
