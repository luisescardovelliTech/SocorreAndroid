package com.luistech.socorre.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.luistech.socorre.R;
import com.luistech.socorre.data.local.Preferencias;
import com.luistech.socorre.databinding.FragmentConfiguracoesBinding;
import com.luistech.socorre.domain.modelo.Sensibilidade;
import com.luistech.socorre.ui.viewmodel.ConfiguracoesViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/** Tela 12 - Configurações, privacidade (LGPD) e conta. */
@AndroidEntryPoint(Fragment.class)
public class ConfiguracoesFragment extends Hilt_ConfiguracoesFragment {

    private FragmentConfiguracoesBinding b;
    private ConfiguracoesViewModel vm;

    @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentConfiguracoesBinding.inflate(i, c, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        vm = new ViewModelProvider(this).get(ConfiguracoesViewModel.class);

        // Sensibilidade (por perfil de atividade)
        b.textoSensibilidade.setText(getString(R.string.config_sensibilidade_perfil,
                getString(InicioFragment.nomePerfil(vm.perfil()))));
        Sensibilidade sens = vm.sensibilidade();
        b.grupoSensibilidade.check(sens == Sensibilidade.BAIXA ? R.id.botaoSensBaixa
                : sens == Sensibilidade.ALTA ? R.id.botaoSensAlta : R.id.botaoSensMedia);
        b.grupoSensibilidade.addOnButtonCheckedListener((g, id, marcado) -> {
            if (!marcado) return;
            vm.definirSensibilidade(id == R.id.botaoSensBaixa ? Sensibilidade.BAIXA
                    : id == R.id.botaoSensAlta ? Sensibilidade.ALTA : Sensibilidade.MEDIA);
        });

        // Contagem regressiva 15-60 s
        b.sliderContagem.setValue(vm.contagemSeg());
        b.textoContagem.setText(getString(R.string.config_contagem_valor, vm.contagemSeg()));
        b.sliderContagem.addOnChangeListener((sl, valor, doUsuario) -> {
            b.textoContagem.setText(getString(R.string.config_contagem_valor, (int) valor));
            if (doUsuario) vm.definirContagemSeg((int) valor);
        });

        // Check-in
        int ci = vm.intervaloCheckin();
        b.grupoCheckin.check(ci == 15 ? R.id.botaoCheckin15 : ci == 60 ? R.id.botaoCheckin60 : R.id.botaoCheckin30);
        b.grupoCheckin.addOnButtonCheckedListener((g, id, marcado) -> {
            if (marcado) vm.definirIntervaloCheckin(id == R.id.botaoCheckin15 ? 15 : id == R.id.botaoCheckin60 ? 60 : 30);
        });

        // Mensagem do SMS
        b.campoMensagem.setText(vm.mensagemSms());
        b.botaoSalvarMensagem.setOnClickListener(x ->
                vm.salvarMensagemSms(b.campoMensagem.getText() == null ? "" : b.campoMensagem.getText().toString()));
        b.botaoRestaurarMensagem.setOnClickListener(x -> {
            vm.restaurarMensagemSms();
            b.campoMensagem.setText(vm.mensagemSms());
        });

        // Tema
        String tema = vm.modoTema();
        b.grupoTema.check(Preferencias.TEMA_SENSOR.equals(tema) ? R.id.radioTemaSensor
                : Preferencias.TEMA_CLARO.equals(tema) ? R.id.radioTemaClaro
                : Preferencias.TEMA_ESCURO.equals(tema) ? R.id.radioTemaEscuro : R.id.radioTemaSistema);
        b.grupoTema.setOnCheckedChangeListener((g, id) -> vm.definirModoTema(
                id == R.id.radioTemaSensor ? Preferencias.TEMA_SENSOR
                        : id == R.id.radioTemaClaro ? Preferencias.TEMA_CLARO
                        : id == R.id.radioTemaEscuro ? Preferencias.TEMA_ESCURO : Preferencias.TEMA_SISTEMA));

        // Atalhos
        b.botaoPerfil.setOnClickListener(x -> Navegacao.ir(this, R.id.perfilFragment));
        b.botaoPermissoes.setOnClickListener(x -> Navegacao.ir(this, R.id.permissoesFragment));
        b.botaoModoTeste.setOnClickListener(x -> Navegacao.ir(this, R.id.modoTesteFragment));
        b.botaoDiagnostico.setOnClickListener(x -> Navegacao.ir(this, R.id.diagnosticoFragment));
        b.botaoPrivacidade.setOnClickListener(x -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.privacidade_titulo)
                .setMessage(R.string.privacidade_texto)
                .setPositiveButton(R.string.acao_ok, null)
                .show());

        // Conta
        b.botaoSair.setOnClickListener(x -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.conta_sair_titulo)
                .setMessage(R.string.conta_sair_texto)
                .setPositiveButton(R.string.acao_sair, (d, w) -> vm.sair())
                .setNegativeButton(R.string.acao_cancelar, null)
                .show());
        b.botaoExcluirConta.setOnClickListener(x -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.conta_excluir_titulo)
                .setMessage(R.string.conta_excluir_texto)
                .setPositiveButton(R.string.acao_excluir, (d, w) -> vm.excluirConta())
                .setNegativeButton(R.string.acao_cancelar, null)
                .show());

        vm.carregando().observe(getViewLifecycleOwner(), c -> b.progresso.setVisibility(c ? View.VISIBLE : View.GONE));
        vm.mensagem().observe(getViewLifecycleOwner(), ev -> {
            Integer m = ev.consumir();
            if (m != null) Snackbar.make(b.getRoot(), m, Snackbar.LENGTH_LONG).show();
        });
        vm.contaEncerrada().observe(getViewLifecycleOwner(), ev -> {
            if (ev.consumir() != null) Navegacao.substituirTudo(this, R.id.loginFragment, null);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
