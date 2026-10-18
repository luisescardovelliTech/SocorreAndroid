package com.luistech.socorre.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.luistech.socorre.R;
import com.luistech.socorre.databinding.FragmentHistoricoBinding;
import com.luistech.socorre.ui.adapter.HistoricoAdapter;
import com.luistech.socorre.ui.viewmodel.DetalheViewModel;
import com.luistech.socorre.ui.viewmodel.HistoricoViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/** Tela 10 - Histórico de eventos e trajetos (disponível offline pelo cache do Firestore). */
@AndroidEntryPoint(Fragment.class)
public class HistoricoFragment extends Hilt_HistoricoFragment {

    private FragmentHistoricoBinding b;

    @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentHistoricoBinding.inflate(i, c, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        HistoricoViewModel vm = new ViewModelProvider(this).get(HistoricoViewModel.class);
        HistoricoAdapter adapter = new HistoricoAdapter(item -> {
            Bundle args = new Bundle();
            args.putString(DetalheFragment.ARG_TIPO, item.evento != null ? DetalheViewModel.TIPO_EVENTO : DetalheViewModel.TIPO_TRAJETO);
            args.putString(DetalheFragment.ARG_ID, item.evento != null ? item.evento.id : item.trajeto.id);
            Navegacao.ir(this, R.id.detalheFragment, args);
        });
        b.lista.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.lista.setAdapter(adapter);
        vm.itens().observe(getViewLifecycleOwner(), itens -> {
            adapter.definir(itens);
            b.textoVazio.setVisibility(itens.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
