package com.luistech.socorre.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.luistech.socorre.data.repositorio.RepositorioEventos;
import com.luistech.socorre.data.repositorio.RepositorioTrajetos;
import com.luistech.socorre.data.repositorio.RepositorioUsuario;
import com.luistech.socorre.domain.modelo.Evento;
import com.luistech.socorre.domain.modelo.Trajeto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/** Eventos e trajetos em uma linha do tempo. Os listeners servem o cache quando offline. */
@HiltViewModel
public class HistoricoViewModel extends ViewModel {

    /** Item da lista: um evento ou um trajeto. */
    public static final class Item {
        public final Evento evento;
        public final Trajeto trajeto;
        public final Date data;

        Item(Evento evento, Trajeto trajeto) {
            this.evento = evento;
            this.trajeto = trajeto;
            this.data = evento != null ? evento.dataHora : trajeto.inicio;
        }
    }

    private final MutableLiveData<List<Item>> itens = new MutableLiveData<>(new ArrayList<>());
    private final List<Item> eventos = new ArrayList<>();
    private final List<Item> trajetos = new ArrayList<>();
    private ListenerRegistration ouvinteEventos;
    private ListenerRegistration ouvinteTrajetos;

    @Inject
    public HistoricoViewModel(RepositorioUsuario usuarios, RepositorioEventos repoEventos, RepositorioTrajetos repoTrajetos) {
        String uid = usuarios.uid();
        if (uid == null) return;
        ouvinteEventos = repoEventos.recentes(uid).addSnapshotListener((q, e) -> {
            if (q == null) return;
            eventos.clear();
            for (QueryDocumentSnapshot d : q) {
                Evento ev = d.toObject(Evento.class);
                ev.id = d.getId();
                if (ev.dataHora != null) eventos.add(new Item(ev, null));
            }
            publicar();
        });
        ouvinteTrajetos = repoTrajetos.recentes(uid).addSnapshotListener((q, e) -> {
            if (q == null) return;
            trajetos.clear();
            for (QueryDocumentSnapshot d : q) {
                Trajeto t = d.toObject(Trajeto.class);
                t.id = d.getId();
                if (t.inicio != null) trajetos.add(new Item(null, t));
            }
            publicar();
        });
    }

    public LiveData<List<Item>> itens() {
        return itens;
    }

    private void publicar() {
        List<Item> todos = new ArrayList<>(eventos);
        todos.addAll(trajetos);
        Collections.sort(todos, (a, b) -> b.data.compareTo(a.data));
        itens.setValue(todos);
    }

    @Override
    protected void onCleared() {
        if (ouvinteEventos != null) ouvinteEventos.remove();
        if (ouvinteTrajetos != null) ouvinteTrajetos.remove();
    }
}
