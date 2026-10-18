package com.luistech.socorre.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.luistech.socorre.R;
import com.luistech.socorre.databinding.FragmentLoginBinding;
import com.luistech.socorre.ui.viewmodel.LoginViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/** Tela 2 - Login e cadastro (e-mail e senha) e recuperação de senha. */
@AndroidEntryPoint(Fragment.class)
public class LoginFragment extends Hilt_LoginFragment {

    private FragmentLoginBinding b;
    private LoginViewModel vm;

    @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentLoginBinding.inflate(i, c, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        vm = new ViewModelProvider(this).get(LoginViewModel.class);

        b.botaoEntrar.setOnClickListener(x -> vm.entrar(email(), senha()));
        b.botaoCadastrar.setOnClickListener(x -> vm.cadastrar(email(), senha()));
        b.botaoRecuperar.setOnClickListener(x -> vm.recuperarSenha(email()));

        vm.carregando().observe(getViewLifecycleOwner(), carregando -> {
            b.progresso.setVisibility(carregando ? View.VISIBLE : View.GONE);
            b.botaoEntrar.setEnabled(!carregando);
            b.botaoCadastrar.setEnabled(!carregando);
        });
        vm.erros().observe(getViewLifecycleOwner(), e -> {
            b.layoutEmail.setError(e.email == 0 ? null : getString(e.email));
            b.layoutSenha.setError(e.senha == 0 ? null : getString(e.senha));
        });
        vm.mensagem().observe(getViewLifecycleOwner(), ev -> {
            Integer m = ev.consumir();
            if (m != null) Snackbar.make(b.getRoot(), m, Snackbar.LENGTH_LONG).show();
        });
        vm.navegar().observe(getViewLifecycleOwner(), ev -> {
            LoginViewModel.Destino d = ev.consumir();
            if (d == LoginViewModel.Destino.INICIO) {
                Navegacao.substituirTudo(this, R.id.inicioFragment, null);
            } else if (d == LoginViewModel.Destino.PERFIL) {
                Navegacao.substituirTudo(this, R.id.perfilFragment, Navegacao.configInicial());
            }
        });
    }

    private String email() {
        return b.campoEmail.getText() == null ? "" : b.campoEmail.getText().toString();
    }

    private String senha() {
        return b.campoSenha.getText() == null ? "" : b.campoSenha.getText().toString();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
