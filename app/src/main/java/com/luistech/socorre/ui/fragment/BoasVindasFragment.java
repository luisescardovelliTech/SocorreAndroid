package com.luistech.socorre.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.luistech.socorre.R;
import com.luistech.socorre.data.local.Preferencias;
import com.luistech.socorre.databinding.FragmentBoasVindasBinding;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint(Fragment.class)
public class BoasVindasFragment extends Hilt_BoasVindasFragment {

    @Inject
    Preferencias prefs;
    private FragmentBoasVindasBinding b;

    @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentBoasVindasBinding.inflate(i, c, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        b.botaoComecar.setEnabled(b.checkConsentimento.isChecked());
        b.checkConsentimento.setOnCheckedChangeListener((x, marcado) -> b.botaoComecar.setEnabled(marcado));
        b.botaoComecar.setOnClickListener(x -> {
            prefs.concluirOnboarding(System.currentTimeMillis());
            Navegacao.substituirTudo(this, R.id.loginFragment, null);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
