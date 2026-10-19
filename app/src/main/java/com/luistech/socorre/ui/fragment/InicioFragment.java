package com.luistech.socorre.ui.fragment;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.luistech.socorre.R;
import com.luistech.socorre.alert.ControladorEmergencia;
import com.luistech.socorre.databinding.FragmentInicioBinding;
import com.luistech.socorre.domain.modelo.PerfilAtividade;
import com.luistech.socorre.security.GerenciadorPermissoes;
import com.luistech.socorre.ui.activity.AlertaEnviadoActivity;
import com.luistech.socorre.ui.activity.ContagemActivity;
import com.luistech.socorre.ui.viewmodel.InicioViewModel;
import com.luistech.socorre.util.TextosClima;
import com.luistech.socorre.weather.AvaliadorClima;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint(Fragment.class)
public class InicioFragment extends Hilt_InicioFragment {

    private FragmentInicioBinding b;
    private InicioViewModel vm;
    private boolean monitorando;
    private ControladorEmergencia.Estado emergencia;

    @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentInicioBinding.inflate(i, c, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        vm = new ViewModelProvider(this).get(InicioViewModel.class);

        b.botaoMonitoramento.setOnClickListener(x -> vm.alternarMonitoramento(monitorando));
        b.botaoPanico.definirOuvinte(() -> vm.panico());
        b.grupoPerfil.setOnCheckedStateChangeListener((g, ids) -> {
            if (!ids.isEmpty()) vm.definirPerfil(perfilDoChip(ids.get(0)));
        });
        b.botaoTrajeto.setOnClickListener(x -> {
            if (vm.trajetoAtivo()) Navegacao.ir(this, R.id.trajetoAtivoFragment);
            else vm.prepararTrajeto();
        });
        b.avisoEmergencia.setOnClickListener(x -> abrirEmergencia());
        b.avisoContatos.setOnClickListener(x -> Navegacao.ir(this, R.id.contatosFragment));
        b.avisoPermissoes.setOnClickListener(x -> Navegacao.ir(this, R.id.permissoesFragment));

        vm.monitorando().observe(getViewLifecycleOwner(), ativo -> {
            monitorando = Boolean.TRUE.equals(ativo);
            renderizarStatus();
        });
        vm.emergencia().observe(getViewLifecycleOwner(), e -> {
            emergencia = e;
            renderizarStatus();
        });
        vm.perfil().observe(getViewLifecycleOwner(), this::marcarPerfil);
        vm.trajetoAoVivo().observe(getViewLifecycleOwner(), r -> atualizarBotaoTrajeto());
        vm.preparando().observe(getViewLifecycleOwner(), p -> {
            b.progressoTrajeto.setVisibility(p ? View.VISIBLE : View.GONE);
            b.botaoTrajeto.setEnabled(!p);
        });
        vm.mensagem().observe(getViewLifecycleOwner(), ev -> {
            Integer m = ev.consumir();
            if (m != null) Snackbar.make(b.getRoot(), m, Snackbar.LENGTH_LONG).show();
        });
        vm.preparo().observe(getViewLifecycleOwner(), ev -> {
            InicioViewModel.PreparoTrajeto p = ev.consumir();
            if (p != null) confirmarTrajeto(p);
        });
        vm.climaAtual().observe(getViewLifecycleOwner(), this::mostrarClima);
        vm.abrirTrajeto().observe(getViewLifecycleOwner(), ev -> {
            if (ev.consumir() != null) Navegacao.ir(this, R.id.trajetoAtivoFragment);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        b.avisoContatos.setVisibility(vm.temContatos() ? View.GONE : View.VISIBLE);
        boolean faltaPermissao = !GerenciadorPermissoes.temSms(requireContext())
                || !GerenciadorPermissoes.temLocalizacao(requireContext())
                || !GerenciadorPermissoes.temNotificacoes(requireContext());
        b.avisoPermissoes.setVisibility(faltaPermissao ? View.VISIBLE : View.GONE);
        atualizarBotaoTrajeto();
        vm.atualizarClima();
    }

    private void mostrarClima(InicioViewModel.EstadoClima e) {
        if (b == null) return;
        if (e.clima == null) {
            b.textoClima.setText(e.mensagem);
            b.textoClimaAvisos.setVisibility(View.GONE);
            return;
        }
        b.textoClima.setText(resumoClima(e.clima));
        List<String> linhas = new ArrayList<>();
        for (AvaliadorClima.Aviso a : AvaliadorClima.avaliar(e.clima)) linhas.add(getString(textoAviso(a)));
        b.textoClimaAvisos.setText(linhas.isEmpty() ? getString(R.string.clima_sem_avisos)
                : android.text.TextUtils.join("\n", linhas));
        b.textoClimaAvisos.setTextColor(ContextCompat.getColor(requireContext(),
                linhas.isEmpty() ? R.color.texto_secundario : R.color.aviso_texto));
        b.textoClimaAvisos.setVisibility(View.VISIBLE);
    }

    private String resumoClima(com.luistech.socorre.domain.modelo.Clima c) {
        return getString(R.string.clima_resumo, Math.round(c.temperaturaC),
                TextosClima.descricao(requireContext(), c.codigoTempo), Math.round(c.ventoKmh));
    }

    private void renderizarStatus() {
        if (b == null) return;
        boolean emEmergencia = emergencia != null && emergencia.fase != ControladorEmergencia.Fase.OCIOSO;
        int fundo, texto, icone, rotulo;
        if (emEmergencia) {
            fundo = R.color.emergencia;
            texto = R.color.sobre_emergencia;
            icone = R.drawable.ic_alerta;
            rotulo = R.string.status_emergencia;
        } else if (monitorando) {
            fundo = R.color.ativo_container;
            texto = R.color.ativo_texto;
            icone = R.drawable.ic_escudo;
            rotulo = R.string.status_ativo;
        } else {
            fundo = R.color.pausado_container;
            texto = R.color.pausado_texto;
            icone = R.drawable.ic_pausa;
            rotulo = R.string.status_pausado;
        }
        int corTexto = ContextCompat.getColor(requireContext(), texto);
        b.cartaoStatus.setCardBackgroundColor(ContextCompat.getColor(requireContext(), fundo));
        b.textoStatus.setText(rotulo);
        b.textoStatus.setTextColor(corTexto);
        b.textoPerfilAtual.setTextColor(corTexto);
        b.iconeStatus.setImageResource(icone);
        b.iconeStatus.setColorFilter(corTexto);
        b.avisoEmergencia.setVisibility(emEmergencia ? View.VISIBLE : View.GONE);

        b.botaoMonitoramento.setText(monitorando ? R.string.acao_pausar_monitoramento : R.string.acao_ativar_monitoramento);
        b.botaoMonitoramento.setIconResource(monitorando ? R.drawable.ic_pausa : R.drawable.ic_escudo);
        int corBotao = monitorando ? R.color.pausado : R.color.ativo;
        b.botaoMonitoramento.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), corBotao)));
        int branco = ContextCompat.getColor(requireContext(), R.color.sobre_estado);
        b.botaoMonitoramento.setTextColor(branco);
        b.botaoMonitoramento.setIconTint(ColorStateList.valueOf(branco));
    }

    private void atualizarBotaoTrajeto() {
        if (b == null) return;
        b.botaoTrajeto.setText(vm.trajetoAtivo() ? R.string.acao_ver_trajeto : R.string.acao_iniciar_trajeto);
    }

    private void confirmarTrajeto(InicioViewModel.PreparoTrajeto p) {
        if (p.avisos.isEmpty()) {
            vm.iniciarTrajeto(p.clima);
            if (p.clima != null) {
                Snackbar.make(b.getRoot(), getString(R.string.clima_trajeto_ok, resumoClima(p.clima)), Snackbar.LENGTH_LONG).show();
            }
            return;
        }
        List<String> linhas = new ArrayList<>();
        for (AvaliadorClima.Aviso a : p.avisos) linhas.add(getString(textoAviso(a)));
        if (p.clima != null) {
            linhas.add(0, getString(R.string.clima_agora, TextosClima.descricao(requireContext(), p.clima.codigoTempo),
                    Math.round(p.clima.temperaturaC)));
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.clima_titulo_aviso)
                .setMessage(android.text.TextUtils.join("\n\n", linhas))
                .setPositiveButton(R.string.acao_iniciar_mesmo_assim, (d, w) -> vm.iniciarTrajeto(p.clima))
                .setNegativeButton(R.string.acao_cancelar, null)
                .show();
    }

    private static int textoAviso(AvaliadorClima.Aviso a) {
        switch (a) {
            case CHUVA:
                return R.string.clima_chuva;
            case VENTO_FORTE:
                return R.string.clima_vento;
            case FRIO_EXTREMO:
                return R.string.clima_frio;
            case CALOR_EXTREMO:
            default:
                return R.string.clima_calor;
        }
    }

    private void abrirEmergencia() {
        if (emergencia == null) return;
        Class<?> tela = emergencia.fase == ControladorEmergencia.Fase.ENVIADO
                ? AlertaEnviadoActivity.class : ContagemActivity.class;
        startActivity(new Intent(requireContext(), tela));
    }

    private void marcarPerfil(PerfilAtividade p) {
        int id;
        switch (p) {
            case BICICLETA:
                id = R.id.chipBicicleta;
                break;
            case MOTO:
                id = R.id.chipMoto;
                break;
            case TRABALHO_CAMPO:
                id = R.id.chipCampo;
                break;
            case CAMINHADA_CASA:
            default:
                id = R.id.chipCasa;
                break;
        }
        if (b.grupoPerfil.getCheckedChipId() != id) b.grupoPerfil.check(id);
        b.textoPerfilAtual.setText(getString(R.string.inicio_perfil_atual, getString(nomePerfil(p))));
    }

    public static int nomePerfil(PerfilAtividade p) {
        switch (p) {
            case BICICLETA:
                return R.string.perfil_bicicleta;
            case MOTO:
                return R.string.perfil_moto;
            case TRABALHO_CAMPO:
                return R.string.perfil_campo;
            case CAMINHADA_CASA:
            default:
                return R.string.perfil_casa;
        }
    }

    private static PerfilAtividade perfilDoChip(int id) {
        if (id == R.id.chipBicicleta) return PerfilAtividade.BICICLETA;
        if (id == R.id.chipMoto) return PerfilAtividade.MOTO;
        if (id == R.id.chipCampo) return PerfilAtividade.TRABALHO_CAMPO;
        return PerfilAtividade.CAMINHADA_CASA;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
