package com.luistech.socorre.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.luistech.socorre.data.repositorio.RepositorioEventos;
import com.luistech.socorre.data.repositorio.RepositorioTrajetos;
import com.luistech.socorre.data.repositorio.RepositorioUsuario;
import com.luistech.socorre.domain.modelo.Evento;
import com.luistech.socorre.domain.modelo.Trajeto;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class DetalheViewModel extends ViewModel {

    public static final String TIPO_EVENTO = "evento";
    public static final String TIPO_TRAJETO = "trajeto";

    private final RepositorioUsuario usuarios;
    private final RepositorioEventos eventos;
    private final RepositorioTrajetos trajetos;
    private final MutableLiveData<Evento> evento = new MutableLiveData<>();
    private final MutableLiveData<Trajeto> trajeto = new MutableLiveData<>();
    private final MutableLiveData<List<Evento>> eventosDoTrajeto = new MutableLiveData<>(new ArrayList<>());
    private ListenerRegistration ouvinte;
    private boolean carregado;

    @Inject
    public DetalheViewModel(RepositorioUsuario usuarios, RepositorioEventos eventos, RepositorioTrajetos trajetos) {
        this.usuarios = usuarios;
        this.eventos = eventos;
        this.trajetos = trajetos;
    }

    public LiveData<Evento> evento() {
        return evento;
    }

    public LiveData<Trajeto> trajeto() {
        return trajeto;
    }

    public LiveData<List<Evento>> eventosDoTrajeto() {
        return eventosDoTrajeto;
    }

    public void carregar(String tipo, String id) {
        String uid = usuarios.uid();
        if (carregado || uid == null || id == null) return;
        carregado = true;
        if (TIPO_EVENTO.equals(tipo)) {
            ouvinte = eventos.ouvir(uid, id, (d, e) -> {
                if (d != null && d.exists()) evento.setValue(converter(d));
            });
        } else {
            trajetos.trajetos(uid).document(id).get().addOnSuccessListener(d -> {
                if (!d.exists()) return;
                Trajeto t = d.toObject(Trajeto.class);
                if (t != null) t.id = d.getId();
                trajeto.setValue(t);
            });
            eventos.doTrajeto(uid, id).get().addOnSuccessListener(q -> {
                List<Evento> l = new ArrayList<>();
                for (QueryDocumentSnapshot d : q) l.add(converter(d));
                eventosDoTrajeto.setValue(l);
            });
        }
    }

    private static Evento converter(DocumentSnapshot d) {
        Evento ev = d.toObject(Evento.class);
        if (ev != null) ev.id = d.getId();
        return ev;
    }

    @Override
    protected void onCleared() {
        if (ouvinte != null) ouvinte.remove();
    }
}
