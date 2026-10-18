package com.luistech.socorre.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.luistech.socorre.R;
import com.luistech.socorre.alert.DespachanteAlerta;
import com.luistech.socorre.databinding.ItemHistoricoBinding;
import com.luistech.socorre.domain.modelo.Evento;
import com.luistech.socorre.domain.modelo.StatusEvento;
import com.luistech.socorre.domain.modelo.TipoEvento;
import com.luistech.socorre.domain.modelo.Trajeto;
import com.luistech.socorre.ui.viewmodel.HistoricoViewModel;
import com.luistech.socorre.util.Formatos;

import java.util.ArrayList;
import java.util.List;

public class HistoricoAdapter extends RecyclerView.Adapter<HistoricoAdapter.Vh> {

    public interface AoClicar {
        void abrir(HistoricoViewModel.Item item);
    }

    private final AoClicar aoClicar;
    private List<HistoricoViewModel.Item> itens = new ArrayList<>();

    public HistoricoAdapter(AoClicar aoClicar) {
        this.aoClicar = aoClicar;
    }

    public void definir(List<HistoricoViewModel.Item> lista) {
        itens = lista == null ? new ArrayList<>() : lista;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Vh onCreateViewHolder(@NonNull ViewGroup pai, int tipo) {
        return new Vh(ItemHistoricoBinding.inflate(LayoutInflater.from(pai.getContext()), pai, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Vh h, int pos) {
        HistoricoViewModel.Item item = itens.get(pos);
        Context c = h.b.getRoot().getContext();
        if (item.evento != null) {
            Evento e = item.evento;
            String tipo = c.getString(DespachanteAlerta.nomeTipo(TipoEvento.deTexto(e.tipo)));
            h.b.textoTitulo.setText(e.teste ? c.getString(R.string.historico_teste, tipo) : tipo);
            h.b.textoStatus.setText(textoStatus(c, e.status));
            boolean emergencia = StatusEvento.ALERTA_ENVIADO.name().equals(e.status) && !e.teste;
            h.b.icone.setImageResource(R.drawable.ic_alerta);
            h.b.icone.setColorFilter(ContextCompat.getColor(c, emergencia ? R.color.emergencia : R.color.pausado));
        } else {
            Trajeto t = item.trajeto;
            h.b.textoTitulo.setText(c.getString(R.string.historico_trajeto, Formatos.km(c, t.distanciaM)));
            h.b.textoStatus.setText(t.fim == null ? c.getString(R.string.historico_trajeto_aberto)
                    : c.getString(R.string.historico_trajeto_duracao, Formatos.duracao(t.duracaoSeg)));
            h.b.icone.setImageResource(R.drawable.ic_rota);
            h.b.icone.setColorFilter(ContextCompat.getColor(c, R.color.texto_secundario));
        }
        h.b.textoData.setText(Formatos.dataHora(item.data));
        h.b.getRoot().setOnClickListener(v -> aoClicar.abrir(itens.get(h.getBindingAdapterPosition())));
    }

    public static String textoStatus(Context c, String status) {
        if (StatusEvento.NORMAL.name().equals(status)) return c.getString(R.string.status_evento_normal);
        if (StatusEvento.CANCELADO.name().equals(status)) return c.getString(R.string.status_evento_cancelado);
        if (StatusEvento.ENCERRADO.name().equals(status)) return c.getString(R.string.status_evento_encerrado);
        return c.getString(R.string.status_evento_enviado);
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    static final class Vh extends RecyclerView.ViewHolder {
        final ItemHistoricoBinding b;

        Vh(ItemHistoricoBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
