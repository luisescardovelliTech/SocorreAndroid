package com.luistech.socorre.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.luistech.socorre.R;
import com.luistech.socorre.databinding.FragmentTrajetoAtivoBinding;
import com.luistech.socorre.ui.viewmodel.TrajetoViewModel;
import com.luistech.socorre.util.Formatos;

import dagger.hilt.android.AndroidEntryPoint;

/** Tela 7 - Trajeto ativo: distância, tempo, velocidade, check-in e pânico. */
@AndroidEntryPoint(Fragment.class)
public class TrajetoAtivoFragment extends Hilt_TrajetoAtivoFragment {

    private FragmentTrajetoAtivoBinding b;
    private TrajetoViewModel vm;

    @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentTrajetoAtivoBinding.inflate(i, c, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        vm = new ViewModelProvider(this).get(TrajetoViewModel.class);

        int intervalo = vm.intervaloCheckin();
        b.grupoCheckin.check(intervalo == 15 ? R.id.botaoCheckin15 : intervalo == 60 ? R.id.botaoCheckin60 : R.id.botaoCheckin30);
        b.grupoCheckin.addOnButtonCheckedListener((g, id, marcado) -> {
            if (!marcado) return;
            vm.alterarIntervalo(id == R.id.botaoCheckin15 ? 15 : id == R.id.botaoCheckin60 ? 60 : 30);
        });
        b.botaoPanico.definirOuvinte(() -> vm.panico());
        b.botaoEncerrar.setOnClickListener(x -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.trajeto_encerrar_titulo)
                .setMessage(R.string.trajeto_encerrar_texto)
                .setPositiveButton(R.string.acao_encerrar, (d, w) -> {
                    vm.encerrar();
                    NavHostFragment.findNavController(this).popBackStack();
                })
                .setNegativeButton(R.string.acao_cancelar, null)
                .show());

        vm.resumo().observe(getViewLifecycleOwner(), r -> {
            boolean ativo = r != null || vm.ativo();
            b.grupoAtivo.setVisibility(ativo ? View.VISIBLE : View.GONE);
            b.textoSemTrajeto.setVisibility(ativo ? View.GONE : View.VISIBLE);
            if (r == null) return;
            b.textoDistancia.setText(Formatos.km(requireContext(), r.distanciaM));
            b.textoDuracao.setText(Formatos.duracao(r.duracaoSeg));
            b.textoVelocidade.setText(Formatos.kmh(requireContext(), r.velocidadeAtualKmh));
            b.textoMedia.setText(Formatos.kmh(requireContext(), r.velocidadeMediaKmh));
            b.textoGps.setText(r.gpsDisponivel ? R.string.trajeto_gps_ok : R.string.trajeto_gps_indisponivel);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
