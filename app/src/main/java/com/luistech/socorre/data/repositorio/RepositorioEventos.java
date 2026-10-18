package com.luistech.socorre.data.repositorio;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.luistech.socorre.domain.modelo.Evento;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Eventos em users/{uid}/eventos. As escritas não são aguardadas: sem internet o Firestore
 * grava no cache local e sincroniza depois, então o fluxo de emergência nunca espera a rede.
 */
@Singleton
public class RepositorioEventos {

    private static final String TAG = "Socorre.Eventos";
    private final FirebaseFirestore db;

    @Inject
    public RepositorioEventos(FirebaseFirestore db) {
        this.db = db;
    }

    public CollectionReference eventos(String uid) {
        return db.collection("users").document(uid).collection("eventos");
    }

    /** Gera o id localmente (funciona offline). Também é a chave de idempotência da API. */
    public String novoId(String uid) {
        return eventos(uid).document().getId();
    }

    public void salvar(String uid, Evento evento) {
        eventos(uid).document(evento.id).set(evento)
                .addOnFailureListener(e -> Log.w(TAG, "evento.salvar falhou id=" + evento.id, e));
    }

    /** Atualiza campos com notação de ponto, ex.: "envios.api.status". */
    public Task<Void> atualizar(String uid, String eventoId, Map<String, Object> campos) {
        return eventos(uid).document(eventoId).update(campos)
                .addOnFailureListener(e -> Log.w(TAG, "evento.atualizar falhou id=" + eventoId, e));
    }

    public ListenerRegistration ouvir(String uid, String eventoId,
                                      com.google.firebase.firestore.EventListener<com.google.firebase.firestore.DocumentSnapshot> ouvinte) {
        return eventos(uid).document(eventoId).addSnapshotListener(ouvinte);
    }

    public Query recentes(String uid) {
        return eventos(uid).orderBy("dataHora", Query.Direction.DESCENDING).limit(100);
    }

    public Query doTrajeto(String uid, String trajetoId) {
        return eventos(uid).whereEqualTo("trajetoId", trajetoId);
    }
}
