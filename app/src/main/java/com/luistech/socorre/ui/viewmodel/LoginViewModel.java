package com.luistech.socorre.ui.viewmodel;

import android.util.Patterns;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.luistech.socorre.R;
import com.luistech.socorre.data.repositorio.RepositorioUsuario;
import com.luistech.socorre.domain.modelo.Usuario;
import com.luistech.socorre.util.EventoUnico;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class LoginViewModel extends ViewModel {

    public enum Destino { PERFIL, INICIO }

    /** Erros de validação por campo (recurso de string ou 0). */
    public static final class ErrosCampos {
        public final int email;
        public final int senha;

        ErrosCampos(int email, int senha) {
            this.email = email;
            this.senha = senha;
        }
    }

    private final RepositorioUsuario repo;
    private final MutableLiveData<Boolean> carregando = new MutableLiveData<>(false);
    private final MutableLiveData<EventoUnico<Integer>> mensagem = new MutableLiveData<>();
    private final MutableLiveData<EventoUnico<Destino>> navegar = new MutableLiveData<>();
    private final MutableLiveData<ErrosCampos> erros = new MutableLiveData<>(new ErrosCampos(0, 0));

    @Inject
    public LoginViewModel(RepositorioUsuario repo) {
        this.repo = repo;
    }

    public LiveData<Boolean> carregando() {
        return carregando;
    }

    public LiveData<EventoUnico<Integer>> mensagem() {
        return mensagem;
    }

    public LiveData<EventoUnico<Destino>> navegar() {
        return navegar;
    }

    public LiveData<ErrosCampos> erros() {
        return erros;
    }

    public void entrar(String email, String senha) {
        if (!validar(email, senha)) return;
        carregando.setValue(true);
        repo.entrar(email.trim(), senha).addOnCompleteListener(this::aposAutenticar);
    }

    public void cadastrar(String email, String senha) {
        if (!validar(email, senha)) return;
        carregando.setValue(true);
        repo.cadastrar(email.trim(), senha).addOnCompleteListener(this::aposAutenticar);
    }

    public void recuperarSenha(String email) {
        if (!emailValido(email)) {
            erros.setValue(new ErrosCampos(R.string.erro_email_invalido, 0));
            return;
        }
        carregando.setValue(true);
        repo.recuperarSenha(email.trim()).addOnCompleteListener(t -> {
            carregando.setValue(false);
            mensagem.setValue(new EventoUnico<>(t.isSuccessful() ? R.string.msg_email_recuperacao : erroAuth(t.getException())));
        });
    }

    private boolean validar(String email, String senha) {
        int e = emailValido(email) ? 0 : R.string.erro_email_invalido;
        int s = senha != null && senha.length() >= 6 ? 0 : R.string.erro_senha_curta;
        erros.setValue(new ErrosCampos(e, s));
        return e == 0 && s == 0;
    }

    private static boolean emailValido(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches();
    }

    private void aposAutenticar(Task<AuthResult> t) {
        if (!t.isSuccessful()) {
            carregando.setValue(false);
            mensagem.setValue(new EventoUnico<>(erroAuth(t.getException())));
            return;
        }
        repo.garantirDocumento(); // não aguarda: offline a escrita fica na fila do Firestore
        repo.carregarContatos()
                .continueWithTask(x -> repo.carregarUsuario())
                .addOnCompleteListener(u -> {
                    carregando.setValue(false);
                    Usuario usuario = u.isSuccessful() ? u.getResult() : null;
                    navegar.setValue(new EventoUnico<>(usuario != null && usuario.perfilCompleto()
                            ? Destino.INICIO : Destino.PERFIL));
                });
    }

    static int erroAuth(Exception e) {
        if (e instanceof FirebaseAuthWeakPasswordException) return R.string.erro_senha_fraca;
        if (e instanceof FirebaseAuthUserCollisionException) return R.string.erro_email_em_uso;
        if (e instanceof FirebaseAuthInvalidCredentialsException || e instanceof FirebaseAuthInvalidUserException) {
            return R.string.erro_login_credenciais;
        }
        if (e instanceof FirebaseNetworkException) return R.string.erro_sem_internet;
        return R.string.erro_generico;
    }
}
