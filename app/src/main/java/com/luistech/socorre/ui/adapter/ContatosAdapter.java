package com.luistech.socorre.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.luistech.socorre.R;
import com.luistech.socorre.databinding.ItemContatoBinding;
import com.luistech.socorre.domain.modelo.ContatoEmergencia;
import com.luistech.socorre.util.Formatos;

import java.util.ArrayList;
import java.util.List;

public class ContatosAdapter extends RecyclerView.Adapter<ContatosAdapter.Vh> {

    public interface Acoes {
        void editar(int posicao);

        void remover(int posicao);

        void mover(int posicao, int delta);

        void testarSms(int posicao);
    }

    private final Acoes acoes;
    private List<ContatoEmergencia> itens = new ArrayList<>();

    public ContatosAdapter(Acoes acoes) {
        this.acoes = acoes;
    }

    public void definir(List<ContatoEmergencia> lista) {
        itens = lista == null ? new ArrayList<>() : lista;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Vh onCreateViewHolder(@NonNull ViewGroup pai, int tipo) {
        return new Vh(ItemContatoBinding.inflate(LayoutInflater.from(pai.getContext()), pai, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Vh h, int pos) {
        ContatoEmergencia c = itens.get(pos);
        Context ctx = h.b.getRoot().getContext();
        h.b.textoPrioridade.setText(ctx.getString(R.string.contato_prioridade, pos + 1));
        h.b.textoNome.setText(c.nome);
        String tel = Formatos.telefone(c.telefone);
        h.b.textoDetalhe.setText(c.relacao == null || c.relacao.isEmpty()
                ? tel : ctx.getString(R.string.contato_detalhe, c.relacao, tel));
        h.b.getRoot().setContentDescription(ctx.getString(R.string.contato_descricao, pos + 1, c.nome, tel));
        h.b.botaoSubir.setEnabled(pos > 0);
        h.b.botaoDescer.setEnabled(pos < itens.size() - 1);
        h.b.botaoSubir.setContentDescription(ctx.getString(R.string.contato_subir_desc, c.nome));
        h.b.botaoDescer.setContentDescription(ctx.getString(R.string.contato_descer_desc, c.nome));
        h.b.botaoRemover.setContentDescription(ctx.getString(R.string.contato_remover_desc, c.nome));
        h.b.botaoTestar.setContentDescription(ctx.getString(R.string.contato_testar_desc, c.nome));
        h.b.getRoot().setOnClickListener(v -> acoes.editar(h.getBindingAdapterPosition()));
        h.b.botaoSubir.setOnClickListener(v -> acoes.mover(h.getBindingAdapterPosition(), -1));
        h.b.botaoDescer.setOnClickListener(v -> acoes.mover(h.getBindingAdapterPosition(), 1));
        h.b.botaoRemover.setOnClickListener(v -> acoes.remover(h.getBindingAdapterPosition()));
        h.b.botaoTestar.setOnClickListener(v -> acoes.testarSms(h.getBindingAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    static final class Vh extends RecyclerView.ViewHolder {
        final ItemContatoBinding b;

        Vh(ItemContatoBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
