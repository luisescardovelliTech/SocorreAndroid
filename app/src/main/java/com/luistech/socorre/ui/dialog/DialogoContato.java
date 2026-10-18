package com.luistech.socorre.ui.dialog;

import android.content.Context;
import android.view.LayoutInflater;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.luistech.socorre.R;
import com.luistech.socorre.databinding.DialogContatoBinding;

/** Diálogo de inclusão/edição de contato. Mantém-se aberto enquanto houver erro de validação. */
public final class DialogoContato {

    public interface AoSalvar {
        /** @return recurso de erro ou 0 se salvou. */
        int salvar(String nome, String telefone, String relacao);
    }

    private DialogoContato() {
    }

    public static void mostrar(Context c, @Nullable String nome, @Nullable String telefone,
                               @Nullable String relacao, boolean edicao, AoSalvar aoSalvar) {
        DialogContatoBinding b = DialogContatoBinding.inflate(LayoutInflater.from(c));
        b.campoNome.setText(nome);
        com.luistech.socorre.util.Formatos.mascararTelefone(b.campoTelefone); // máscara antes do setText
        b.campoTelefone.setText(telefone);
        b.campoRelacao.setText(relacao);
        AlertDialog d = new MaterialAlertDialogBuilder(c)
                .setTitle(edicao ? R.string.contato_editar : R.string.contato_adicionar)
                .setView(b.getRoot())
                .setPositiveButton(R.string.acao_salvar, null)
                .setNegativeButton(R.string.acao_cancelar, null)
                .create();
        d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            b.layoutNome.setError(null);
            b.layoutTelefone.setError(null);
            int erro = aoSalvar.salvar(texto(b.campoNome.getText()), texto(b.campoTelefone.getText()),
                    texto(b.campoRelacao.getText()));
            if (erro == 0) {
                d.dismiss();
            } else if (erro == R.string.erro_nome_obrigatorio) {
                b.layoutNome.setError(c.getString(erro));
            } else {
                b.layoutTelefone.setError(c.getString(erro));
            }
        }));
        d.show();
    }

    private static String texto(@Nullable CharSequence s) {
        return s == null ? "" : s.toString();
    }
}
