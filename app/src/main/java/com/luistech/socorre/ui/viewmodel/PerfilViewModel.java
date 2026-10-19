package com.luistech.socorre.ui.viewmodel;

import android.content.Context;
import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.luistech.socorre.R;
import com.luistech.socorre.data.local.Preferencias;
import com.luistech.socorre.data.repositorio.RepositorioUsuario;
import com.luistech.socorre.domain.caso_de_uso.ValidadorTelefone;
import com.luistech.socorre.domain.modelo.FichaMedica;
import com.luistech.socorre.domain.modelo.PerfilAtividade;
import com.luistech.socorre.domain.modelo.Usuario;
import com.luistech.socorre.service.ServicoMonitoramento;
import com.luistech.socorre.util.EventoUnico;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;

@HiltViewModel
public class PerfilViewModel extends ViewModel {

    public static final class Formulario {
        public String nome;
        public String telefone;
        public String dataNascimento;
        public String fotoUrl;
        public PerfilAtividade perfil;
        public boolean guardarFicha;
        public FichaMedica ficha;
    }

    public static final class Erros {
        public final int nome;
        public final int telefone;

        Erros(int nome, int telefone) {
            this.nome = nome;
            this.telefone = telefone;
        }
    }

    private final Context contexto;
    private final RepositorioUsuario repo;
    private final Preferencias prefs;
    private final MutableLiveData<Usuario> usuario = new MutableLiveData<>();
    private final MutableLiveData<Erros> erros = new MutableLiveData<>(new Erros(0, 0));
    private final MutableLiveData<EventoUnico<Integer>> mensagem = new MutableLiveData<>();
    private final MutableLiveData<EventoUnico<Boolean>> salvo = new MutableLiveData<>();
    private final MutableLiveData<String> foto = new MutableLiveData<>();
    private final ExecutorService executor;

    @Inject
    public PerfilViewModel(@ApplicationContext Context contexto, RepositorioUsuario repo, Preferencias prefs,
                           ExecutorService executor) {
        this.contexto = contexto;
        this.executor = executor;
        this.repo = repo;
        this.prefs = prefs;
        carregar();
    }

    public LiveData<Usuario> usuario() {
        return usuario;
    }

    public LiveData<Erros> erros() {
        return erros;
    }

    public LiveData<EventoUnico<Integer>> mensagem() {
        return mensagem;
    }

    public LiveData<EventoUnico<Boolean>> salvo() {
        return salvo;
    }

    public LiveData<String> foto() {
        return foto;
    }

    public void definirFoto(Uri origem) {
        executor.execute(() -> {
            File destino = new File(contexto.getFilesDir(), "foto_perfil.jpg");
            try (InputStream in = contexto.getContentResolver().openInputStream(origem);
                 OutputStream out = new FileOutputStream(destino)) {
                if (in == null) throw new IOException("sem conteudo");
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                foto.postValue(destino.getAbsolutePath());
            } catch (IOException e) {
                mensagem.postValue(new EventoUnico<>(R.string.erro_foto));
            }
        });
    }

    private void carregar() {
        repo.carregarUsuario().addOnCompleteListener(t -> {
            Usuario u = t.isSuccessful() ? t.getResult() : null;
            if (u == null) {
                u = new Usuario();
                u.nome = prefs.nomeUsuario();
                u.telefone = prefs.telefoneUsuario();
            }
            if (u.perfilAtividade == null) u.perfilAtividade = prefs.perfil().name();
            usuario.setValue(u);
        });
    }

    public void salvar(Formulario f) {
        String telefone = ValidadorTelefone.normalizar(f.telefone);
        int eNome = f.nome == null || f.nome.trim().isEmpty() ? R.string.erro_nome_obrigatorio : 0;
        int eTel = telefone == null ? R.string.erro_telefone_invalido : 0;
        erros.setValue(new Erros(eNome, eTel));
        if (eNome != 0 || eTel != 0) return;

        prefs.definirPerfil(f.perfil);
        ServicoMonitoramento.enviar(contexto, ServicoMonitoramento.ACAO_ATUALIZAR_PERFIL);

        FichaMedica ficha = null;
        if (f.guardarFicha && f.ficha != null) {
            ficha = f.ficha;
            Usuario atual = usuario.getValue();
            Date anterior = atual != null && atual.fichaMedica != null ? atual.fichaMedica.consentimentoEm : null;
            ficha.consentimentoEm = anterior != null ? anterior : new Date();
        }
        Task<Void> perfil = repo.salvarPerfil(f.nome.trim(), telefone, f.dataNascimento, f.fotoUrl, f.perfil.name());
        Task<Void> fichaTask = repo.salvarFichaMedica(ficha);
        mensagem.setValue(new EventoUnico<>(R.string.msg_perfil_salvo));
        salvo.setValue(new EventoUnico<>(true));
        Tasks.whenAll(perfil, fichaTask).addOnFailureListener(e ->
                mensagem.setValue(new EventoUnico<>(R.string.erro_salvar_nuvem)));
    }
}
