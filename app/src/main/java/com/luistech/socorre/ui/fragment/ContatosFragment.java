package com.luistech.socorre.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.luistech.socorre.R;
import com.luistech.socorre.databinding.FragmentContatosBinding;
import com.luistech.socorre.domain.modelo.ContatoEmergencia;
import com.luistech.socorre.ui.adapter.ContatosAdapter;
import com.luistech.socorre.ui.dialog.DialogoContato;
import com.luistech.socorre.ui.viewmodel.ContatosViewModel;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint(Fragment.class)
public class ContatosFragment extends Hilt_ContatosFragment implements ContatosAdapter.Acoes {

    private FragmentContatosBinding b;
    private ContatosViewModel vm;
    private List<ContatoEmergencia> atuais;

    private final ActivityResultLauncher<Intent> importar =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() == Activity.RESULT_OK && r.getData() != null && r.getData().getData() != null) {
                    lerContatoEscolhido(r.getData().getData());
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentContatosBinding.inflate(i, c, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        vm = new ViewModelProvider(this).get(ContatosViewModel.class);
        ContatosAdapter adapter = new ContatosAdapter(this);
        b.lista.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.lista.setAdapter(adapter);
        boolean inicial = Navegacao.emConfigInicial(this);

        b.botaoAdicionar.setOnClickListener(x -> DialogoContato.mostrar(requireContext(), null, null, null, false,
                (nome, tel, rel) -> vm.salvar(-1, nome, tel, rel)));
        b.botaoImportar.setOnClickListener(x -> {
            try {
                importar.launch(new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI));
            } catch (Exception e) {
                Snackbar.make(b.getRoot(), R.string.erro_agenda, Snackbar.LENGTH_LONG).show();
            }
        });
        b.botaoContinuar.setVisibility(inicial ? View.VISIBLE : View.GONE);
        b.botaoContinuar.setOnClickListener(x -> {
            if (atuais == null || atuais.isEmpty()) {
                Snackbar.make(b.getRoot(), R.string.erro_sem_contatos, Snackbar.LENGTH_LONG).show();
            } else {
                Navegacao.ir(this, R.id.permissoesFragment, Navegacao.configInicial());
            }
        });

        vm.contatos().observe(getViewLifecycleOwner(), lista -> {
            atuais = lista;
            adapter.definir(lista);
            boolean vazio = lista.isEmpty();
            b.textoVazio.setVisibility(vazio ? View.VISIBLE : View.GONE);
            boolean pode = vm.podeAdicionar();
            b.botaoAdicionar.setEnabled(pode);
            b.botaoImportar.setEnabled(pode);
            b.textoLimite.setText(getString(R.string.contatos_limite, lista.size(), ContatoEmergencia.MAXIMO));
        });
        vm.mensagem().observe(getViewLifecycleOwner(), ev -> {
            Integer m = ev.consumir();
            if (m != null) Snackbar.make(b.getRoot(), m, Snackbar.LENGTH_LONG).show();
        });
    }

    private void lerContatoEscolhido(Uri uri) {
        String[] colunas = {ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER};
        try (Cursor c = requireContext().getContentResolver().query(uri, colunas, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                String nome = c.getString(0);
                String tel = c.getString(1);
                DialogoContato.mostrar(requireContext(), nome, tel, null, false,
                        (n, t, r) -> vm.salvar(-1, n, t, r));
            }
        } catch (Exception e) {
            Snackbar.make(b.getRoot(), R.string.erro_agenda, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void editar(int posicao) {
        if (atuais == null || posicao < 0 || posicao >= atuais.size()) return;
        ContatoEmergencia c = atuais.get(posicao);
        DialogoContato.mostrar(requireContext(), c.nome, c.telefone, c.relacao, true,
                (nome, tel, rel) -> vm.salvar(posicao, nome, tel, rel));
    }

    @Override
    public void remover(int posicao) {
        if (atuais == null || posicao < 0 || posicao >= atuais.size()) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.contato_remover_titulo)
                .setMessage(getString(R.string.contato_remover_texto, atuais.get(posicao).nome))
                .setPositiveButton(R.string.acao_remover, (d, w) -> vm.remover(posicao))
                .setNegativeButton(R.string.acao_cancelar, null)
                .show();
    }

    @Override
    public void mover(int posicao, int delta) {
        if (posicao >= 0) vm.mover(posicao, delta);
    }

    @Override
    public void testarSms(int posicao) {
        if (atuais == null || posicao < 0 || posicao >= atuais.size()) return;
        ContatoEmergencia c = atuais.get(posicao);
        String nome = vm.nomeUsuario().isEmpty() ? getString(R.string.sms_usuario_sem_nome) : vm.nomeUsuario();
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.contato_testar_titulo)
                .setMessage(getString(R.string.contato_testar_texto, c.nome, c.telefone))
                .setPositiveButton(R.string.acao_enviar, (d, w) -> vm.testarSms(c, getString(R.string.sms_teste, nome)))
                .setNegativeButton(R.string.acao_cancelar, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
