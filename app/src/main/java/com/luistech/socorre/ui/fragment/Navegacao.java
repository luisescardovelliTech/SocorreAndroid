package com.luistech.socorre.ui.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

final class Navegacao {

    static final String ARG_CONFIG_INICIAL = "configuracaoInicial";

    private Navegacao() {
    }

    static void ir(Fragment f, int destino) {
        ir(f, destino, null);
    }

    static void ir(Fragment f, int destino, Bundle args) {
        NavHostFragment.findNavController(f).navigate(destino, args);
    }

    static void substituirTudo(Fragment f, int destino, Bundle args) {
        androidx.navigation.NavController nav = NavHostFragment.findNavController(f);
        NavOptions opcoes = new NavOptions.Builder().setPopUpTo(nav.getGraph().getId(), true).build();
        nav.navigate(destino, args, opcoes);
    }

    static Bundle configInicial() {
        Bundle b = new Bundle();
        b.putBoolean(ARG_CONFIG_INICIAL, true);
        return b;
    }

    static boolean emConfigInicial(Fragment f) {
        return f.getArguments() != null && f.getArguments().getBoolean(ARG_CONFIG_INICIAL, false);
    }
}
