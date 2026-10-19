package com.luistech.socorre.data.repositorio;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.luistech.socorre.data.local.Preferencias;
import com.luistech.socorre.domain.modelo.ContatoEmergencia;
import com.luistech.socorre.domain.modelo.FichaMedica;
import com.luistech.socorre.domain.modelo.Usuario;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RepositorioUsuario {

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final Preferencias prefs;

    @Inject
    public RepositorioUsuario(FirebaseAuth auth, FirebaseFirestore db, Preferencias prefs) {
        this.auth = auth;
        this.db = db;
        this.prefs = prefs;
    }

    @Nullable
    public String uid() {
        FirebaseUser u = auth.getCurrentUser();
        return u == null ? null : u.getUid();
    }

    public boolean logado() {
        return uid() != null;
    }

    public Task<AuthResult> entrar(String email, String senha) {
        return auth.signInWithEmailAndPassword(email, senha);
    }

    public Task<AuthResult> cadastrar(String email, String senha) {
        return auth.createUserWithEmailAndPassword(email, senha);
    }

    public Task<Void> recuperarSenha(String email) {
        return auth.sendPasswordResetEmail(email);
    }

    public void sair() {
        auth.signOut();
        prefs.limparDadosUsuario();
    }

    DocumentReference documento() {
        String uid = uid();
        if (uid == null) throw new IllegalStateException("Usuário não autenticado");
        return db.collection("users").document(uid);
    }

    CollectionReference contatosRef() {
        return documento().collection("contatos");
    }

    public Task<Void> garantirDocumento() {
        DocumentReference doc = documento();
        FirebaseUser u = auth.getCurrentUser();
        return doc.get().continueWithTask(t -> {
            Map<String, Object> dados = new HashMap<>();
            dados.put("email", u != null ? u.getEmail() : null);
            if (prefs.consentimentoLocalizacaoEm() > 0) {
                dados.put("consentimentoLgpdEm", new Date(prefs.consentimentoLocalizacaoEm()));
            }
            if (!t.isSuccessful() || t.getResult() == null || !t.getResult().exists()) {
                dados.put("criadoEm", FieldValue.serverTimestamp());
                if (u != null && u.getDisplayName() != null) dados.put("nome", u.getDisplayName());
            }
            return doc.set(dados, SetOptions.merge());
        });
    }

    public Task<Usuario> carregarUsuario() {
        return documento().get().continueWith(t -> {
            DocumentSnapshot s = t.getResult();
            Usuario usuario = s != null && s.exists() ? s.toObject(Usuario.class) : null;
            if (usuario != null) atualizarCache(usuario);
            return usuario;
        });
    }

    private void atualizarCache(Usuario u) {
        prefs.salvarUsuario(u.nome != null ? u.nome : "", u.telefone != null ? u.telefone : "");
        prefs.salvarFichaMedica(u.fichaMedica != null && u.fichaMedica.compartilharNosAlertas ? u.fichaMedica : null);
    }

    public Task<Void> salvarPerfil(String nome, String telefone, String dataNascimento,
                                   String fotoUrl, String perfilAtividade) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("nome", nome);
        dados.put("telefone", telefone);
        dados.put("dataNascimento", dataNascimento);
        dados.put("fotoUrl", fotoUrl);
        dados.put("perfilAtividade", perfilAtividade);
        prefs.salvarUsuario(nome, telefone);
        return documento().set(dados, SetOptions.merge());
    }

    public Task<Void> salvarFichaMedica(@Nullable FichaMedica ficha) {
        prefs.salvarFichaMedica(ficha != null && ficha.compartilharNosAlertas ? ficha : null);
        Map<String, Object> dados = new HashMap<>();
        dados.put("fichaMedica", ficha != null ? ficha : FieldValue.delete());
        return documento().set(dados, SetOptions.merge());
    }

    public Task<Void> salvarConfig(Map<String, Object> config) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("config", config);
        return documento().set(dados, SetOptions.merge());
    }

    public Task<List<ContatoEmergencia>> carregarContatos() {
        return contatosRef().orderBy("prioridade").get().continueWith(t -> {
            List<ContatoEmergencia> lista = new ArrayList<>();
            QuerySnapshot q = t.getResult();
            if (q != null) {
                for (QueryDocumentSnapshot d : q) {
                    ContatoEmergencia c = d.toObject(ContatoEmergencia.class);
                    c.id = d.getId();
                    lista.add(c);
                }
            }
            prefs.salvarContatos(lista);
            return lista;
        });
    }

    public Task<Void> salvarContatos(List<ContatoEmergencia> contatos, List<String> idsRemovidos) {
        if (contatos.size() > ContatoEmergencia.MAXIMO) {
            return Tasks.forException(new IllegalArgumentException("Máximo de " + ContatoEmergencia.MAXIMO + " contatos"));
        }
        WriteBatch lote = db.batch();
        for (String id : idsRemovidos) lote.delete(contatosRef().document(id));
        for (int i = 0; i < contatos.size(); i++) {
            ContatoEmergencia c = contatos.get(i);
            c.prioridade = i + 1;
            if (c.id == null) c.id = contatosRef().document().getId();
            lote.set(contatosRef().document(c.id), c);
        }
        prefs.salvarContatos(new ArrayList<>(contatos));
        return lote.commit();
    }

    public Task<Void> excluirConta() {
        DocumentReference doc = documento();
        FirebaseUser usuario = auth.getCurrentUser();
        List<Task<Void>> exclusoes = new ArrayList<>();
        for (String sub : new String[]{"contatos", "trajetos", "eventos"}) {
            exclusoes.add(apagarColecao(doc.collection(sub)));
        }
        return Tasks.whenAll(exclusoes)
                .continueWithTask(t -> doc.delete())
                .continueWithTask(t -> {
                    if (!t.isSuccessful() && t.getException() != null) throw t.getException();
                    return usuario != null ? usuario.delete() : Tasks.<Void>forResult(null);
                })
                .addOnSuccessListener(v -> prefs.limparDadosUsuario());
    }

    private Task<Void> apagarColecao(CollectionReference col) {
        return col.get().continueWithTask(t -> {
            WriteBatch lote = db.batch();
            QuerySnapshot q = t.getResult();
            List<QueryDocumentSnapshot> docs = q != null ? toList(q) : Collections.<QueryDocumentSnapshot>emptyList();
            for (QueryDocumentSnapshot d : docs) lote.delete(d.getReference());
            return lote.commit();
        });
    }

    private static List<QueryDocumentSnapshot> toList(QuerySnapshot q) {
        List<QueryDocumentSnapshot> l = new ArrayList<>();
        for (QueryDocumentSnapshot d : q) l.add(d);
        return l;
    }
}
