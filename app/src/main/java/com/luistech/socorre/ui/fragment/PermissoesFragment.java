package com.luistech.socorre.ui.fragment;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.luistech.socorre.R;
import com.luistech.socorre.databinding.FragmentPermissoesBinding;
import com.luistech.socorre.databinding.ItemPermissaoBinding;
import com.luistech.socorre.security.GerenciadorPermissoes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint(Fragment.class)
public class PermissoesFragment extends Hilt_PermissoesFragment {

    private static final class Item {
        final int titulo;
        final int explicacao;
        final BooleanSupplier concedida;
        final Runnable pedir;

        Item(int titulo, int explicacao, BooleanSupplier concedida, Runnable pedir) {
            this.titulo = titulo;
            this.explicacao = explicacao;
            this.concedida = concedida;
            this.pedir = pedir;
        }
    }

    private FragmentPermissoesBinding b;
    private final List<Item> itens = new ArrayList<>();
    private String ultimaPedida;

    private final ActivityResultLauncher<String[]> pedirVarias =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), r -> aposPedido());
    private final ActivityResultLauncher<String> pedirUma =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), r -> aposPedido());

    @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentPermissoesBinding.inflate(i, c, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        montarItens();
        boolean inicial = Navegacao.emConfigInicial(this);
        b.botaoConcluir.setVisibility(inicial ? View.VISIBLE : View.GONE);
        b.botaoConcluir.setOnClickListener(x -> Navegacao.substituirTudo(this, R.id.inicioFragment, null));
        b.botaoFabricante.setOnClickListener(x -> abrir(new Intent(Intent.ACTION_VIEW,
                Uri.parse(getString(R.string.url_dontkillmyapp, Build.MANUFACTURER.toLowerCase(Locale.ROOT))))));
    }

    private void montarItens() {
        itens.clear();
        android.content.Context c = requireContext();
        itens.add(new Item(R.string.perm_localizacao_titulo, R.string.perm_localizacao_texto,
                () -> GerenciadorPermissoes.temLocalizacaoPrecisa(c),
                () -> {
                    ultimaPedida = Manifest.permission.ACCESS_FINE_LOCATION;
                    pedirVarias.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION});
                }));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            itens.add(new Item(R.string.perm_segundo_plano_titulo, R.string.perm_segundo_plano_texto,
                    () -> GerenciadorPermissoes.temLocalizacaoSegundoPlano(c),
                    () -> {
                        if (!GerenciadorPermissoes.temLocalizacao(c)) {
                            Snackbar.make(b.getRoot(), R.string.perm_segundo_plano_antes, Snackbar.LENGTH_LONG).show();
                            return;
                        }
                        pedir(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
                    }));
        }
        itens.add(new Item(R.string.perm_sms_titulo, R.string.perm_sms_texto,
                () -> GerenciadorPermissoes.temSms(c), () -> pedir(Manifest.permission.SEND_SMS)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            itens.add(new Item(R.string.perm_notificacao_titulo, R.string.perm_notificacao_texto,
                    () -> GerenciadorPermissoes.temNotificacoes(c), () -> pedir(Manifest.permission.POST_NOTIFICATIONS)));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            itens.add(new Item(R.string.perm_alarme_titulo, R.string.perm_alarme_texto,
                    () -> GerenciadorPermissoes.podeAlarmeExato(c),
                    () -> abrir(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pacote()))));
        }
        if (Build.VERSION.SDK_INT >= 34) {
            itens.add(new Item(R.string.perm_tela_cheia_titulo, R.string.perm_tela_cheia_texto,
                    () -> GerenciadorPermissoes.podeTelaCheia(c),
                    () -> abrir(new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, pacote()))));
        }
        itens.add(new Item(R.string.perm_bateria_titulo, R.string.perm_bateria_texto,
                () -> GerenciadorPermissoes.ignoraOtimizacaoBateria(c),
                () -> abrir(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))));

        b.listaPermissoes.removeAllViews();
        for (Item item : itens) {
            ItemPermissaoBinding ib = ItemPermissaoBinding.inflate(getLayoutInflater(), b.listaPermissoes, true);
            ib.textoTitulo.setText(item.titulo);
            ib.textoExplicacao.setText(item.explicacao);
            ib.botaoPermitir.setOnClickListener(x -> item.pedir.run());
            ib.getRoot().setTag(item);
        }
        atualizarEstados();
    }

    private void atualizarEstados() {
        if (b == null) return;
        for (int i = 0; i < b.listaPermissoes.getChildCount(); i++) {
            View filho = b.listaPermissoes.getChildAt(i);
            Item item = (Item) filho.getTag();
            ItemPermissaoBinding ib = ItemPermissaoBinding.bind(filho);
            boolean ok = item.concedida.getAsBoolean();
            ib.textoEstado.setText(ok ? R.string.perm_concedida : R.string.perm_pendente);
            ib.textoEstado.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    ok ? R.drawable.ic_check : R.drawable.ic_aviso, 0, 0, 0);
            ib.textoEstado.setTextColor(ContextCompat.getColor(requireContext(), ok ? R.color.ativo_texto : R.color.aviso_texto));
            ib.botaoPermitir.setVisibility(ok ? View.GONE : View.VISIBLE);
            ib.botaoPermitir.setContentDescription(getString(R.string.perm_permitir_desc, getString(item.titulo)));
        }
    }

    private void pedir(String permissao) {
        ultimaPedida = permissao;
        pedirUma.launch(permissao);
    }

    private void aposPedido() {
        atualizarEstados();
        if (ultimaPedida == null || b == null) return;
        boolean concedida = ContextCompat.checkSelfPermission(requireContext(), ultimaPedida)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
        if (!concedida && !shouldShowRequestPermissionRationale(ultimaPedida)) {
            Snackbar.make(b.getRoot(), R.string.perm_negada_configuracoes, Snackbar.LENGTH_LONG)
                    .setAction(R.string.acao_abrir_configuracoes, x ->
                            abrir(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pacote())))
                    .show();
        }
    }

    private Uri pacote() {
        return Uri.parse("package:" + requireContext().getPackageName());
    }

    private void abrir(Intent i) {
        try {
            startActivity(i);
        } catch (Exception e) {
            Snackbar.make(b.getRoot(), R.string.erro_abrir_configuracoes, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        atualizarEstados();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
