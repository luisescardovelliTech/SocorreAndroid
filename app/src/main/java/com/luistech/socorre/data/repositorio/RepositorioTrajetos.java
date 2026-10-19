package com.luistech.socorre.data.repositorio;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.luistech.socorre.domain.modelo.Trajeto;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RepositorioTrajetos {

    private static final String TAG = "Socorre.Trajetos";

    public static class Resumo {
        public final String id;
        public final double distanciaM;
        public final long duracaoSeg;
        public final double velocidadeAtualKmh;
        public final double velocidadeMediaKmh;
        public final boolean gpsDisponivel;

        public Resumo(String id, double distanciaM, long duracaoSeg, double velocidadeAtualKmh,
                      double velocidadeMediaKmh, boolean gpsDisponivel) {
            this.id = id;
            this.distanciaM = distanciaM;
            this.duracaoSeg = duracaoSeg;
            this.velocidadeAtualKmh = velocidadeAtualKmh;
            this.velocidadeMediaKmh = velocidadeMediaKmh;
            this.gpsDisponivel = gpsDisponivel;
        }
    }

    private final FirebaseFirestore db;
    private final MutableLiveData<Resumo> resumo = new MutableLiveData<>(null);

    @Inject
    public RepositorioTrajetos(FirebaseFirestore db) {
        this.db = db;
    }

    public CollectionReference trajetos(String uid) {
        return db.collection("users").document(uid).collection("trajetos");
    }

    public String novoId(String uid) {
        return trajetos(uid).document().getId();
    }

    public void salvar(String uid, Trajeto t) {
        trajetos(uid).document(t.id).set(t)
                .addOnFailureListener(e -> Log.w(TAG, "trajeto.salvar falhou id=" + t.id, e));
    }

    public void atualizar(String uid, String id, String polyline, double distanciaM, long duracaoSeg,
                          double mediaKmh, java.util.Date fim) {
        Map<String, Object> campos = new HashMap<>();
        campos.put("polyline", polyline);
        campos.put("distanciaM", distanciaM);
        campos.put("duracaoSeg", duracaoSeg);
        campos.put("velocidadeMediaKmh", mediaKmh);
        if (fim != null) campos.put("fim", fim);
        trajetos(uid).document(id).update(campos)
                .addOnFailureListener(e -> Log.w(TAG, "trajeto.atualizar falhou id=" + id, e));
    }

    public Query recentes(String uid) {
        return trajetos(uid).orderBy("inicio", Query.Direction.DESCENDING).limit(50);
    }

    public LiveData<Resumo> resumoAoVivo() {
        return resumo;
    }

    public void publicarResumo(Resumo r) {
        resumo.postValue(r);
    }
}
