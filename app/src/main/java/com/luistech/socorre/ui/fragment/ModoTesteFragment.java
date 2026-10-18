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
import com.luistech.socorre.databinding.FragmentModoTesteBinding;
import com.luistech.socorre.domain.modelo.TipoEvento;
import com.luistech.socorre.ui.viewmodel.EmergenciaViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Tela 13 - Modo de teste: percorre todo o fluxo (contagem, cancelamento, processamento) sem
 * enviar SMS nem notificações reais. O evento fica marcado como teste e a API recebe "teste": true.
 */
@AndroidEntryPoint(Fragment.class)
public class ModoTesteFragment extends Hilt_ModoTesteFragment {

    @javax.inject.Inject
    com.luistech.socorre.data.local.Preferencias prefs;

    private FragmentModoTesteBinding b;

    @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentModoTesteBinding.inflate(i, c, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        EmergenciaViewModel vm = new ViewModelProvider(this).get(EmergenciaViewModel.class);
        b.chaveBrusco.setChecked(prefs.gatilhoBrusco());
        b.chaveBrusco.setOnCheckedChangeListener((x, ativo) -> prefs.definirGatilhoBrusco(ativo));
        b.botaoSimularQueda.setOnClickListener(x -> vm.simular(TipoEvento.QUEDA));
        b.botaoSimularPanico.setOnClickListener(x -> vm.simular(TipoEvento.PANICO));
        b.botaoSimularCheckin.setOnClickListener(x -> vm.simular(TipoEvento.CHECKIN_SEM_RESPOSTA));
        vm.mensagem().observe(getViewLifecycleOwner(), ev -> {
            Integer m = ev.consumir();
            if (m != null) Snackbar.make(b.getRoot(), m, Snackbar.LENGTH_LONG).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
