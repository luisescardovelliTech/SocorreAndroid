package com.luistech.socorre.ui.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;
import com.luistech.socorre.R;
import com.luistech.socorre.alert.DespachanteAlerta;
import com.luistech.socorre.databinding.FragmentDetalheBinding;
import com.luistech.socorre.domain.modelo.Evento;
import com.luistech.socorre.domain.modelo.TipoEvento;
import com.luistech.socorre.domain.modelo.Trajeto;
import com.luistech.socorre.ui.adapter.HistoricoAdapter;
import com.luistech.socorre.ui.viewmodel.DetalheViewModel;
import com.luistech.socorre.util.Formatos;
import com.luistech.socorre.util.TextosClima;
import com.luistech.socorre.util.TextosEnvio;

import java.util.ArrayList;
import java.util.List;

import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import dagger.hilt.android.AndroidEntryPoint;

/** Tela 11 - Detalhe do trajeto (rota + eventos no mapa) ou do evento (leituras e envios). */
@AndroidEntryPoint(Fragment.class)
public class DetalheFragment extends Hilt_DetalheFragment {

    public static final String ARG_TIPO = "tipo";
    public static final String ARG_ID = "id";

    private FragmentDetalheBinding b;
    private DetalheViewModel vm;
    private Evento evento;
    private Trajeto trajeto;
    private List<Evento> eventosTrajeto = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentDetalheBinding.inflate(i, c, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        vm = new ViewModelProvider(this).get(DetalheViewModel.class);
        Bundle args = requireArguments();
        vm.carregar(args.getString(ARG_TIPO), args.getString(ARG_ID));

        b.mapa.setTileSource(TileSourceFactory.MAPNIK);
        b.mapa.setMultiTouchControls(true);
        b.mapa.getController().setZoom(4.0);
        b.mapa.getController().setCenter(new GeoPoint(-15.8, -47.9)); // Brasil, até carregar os dados
        // Arrastar o mapa não deve rolar a tela.
        b.mapa.setOnTouchListener((x, ev) -> {
            x.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });

        vm.evento().observe(getViewLifecycleOwner(), e -> {
            evento = e;
            mostrarEvento();
            desenharMapa();
        });
        vm.trajeto().observe(getViewLifecycleOwner(), t -> {
            trajeto = t;
            mostrarTrajeto();
            desenharMapa();
        });
        vm.eventosDoTrajeto().observe(getViewLifecycleOwner(), l -> {
            eventosTrajeto = l;
            mostrarTrajeto();
            desenharMapa();
        });
    }

    private void mostrarEvento() {
        if (evento == null || b == null) return;
        String tipo = getString(DespachanteAlerta.nomeTipo(TipoEvento.deTexto(evento.tipo)));
        b.textoTitulo.setText(evento.teste ? getString(R.string.historico_teste, tipo) : tipo);
        StringBuilder sb = new StringBuilder();
        linha(sb, R.string.detalhe_data, Formatos.dataHora(evento.dataHora));
        linha(sb, R.string.detalhe_status, HistoricoAdapter.textoStatus(requireContext(), evento.status));
        if (evento.localizacao != null) {
            linha(sb, R.string.detalhe_precisao, getString(R.string.formato_metros, Math.round(evento.localizacao.precisaoM)));
        } else {
            linha(sb, R.string.detalhe_localizacao, getString(R.string.alerta_localizacao_indisponivel));
        }
        if (evento.bateriaPct != null) linha(sb, R.string.detalhe_bateria, getString(R.string.formato_porcento, evento.bateriaPct));
        if (evento.leituras != null) {
            linha(sb, R.string.detalhe_pico, getString(R.string.formato_g, Formatos.decimal(evento.leituras.picoG)));
            linha(sb, R.string.detalhe_rotacao, getString(R.string.formato_graus, Formatos.decimal(evento.leituras.rotacaoGraus)));
            if (evento.leituras.variacaoAltitudeM != null) {
                linha(sb, R.string.detalhe_altitude, getString(R.string.formato_metros_decimal, Formatos.decimal(evento.leituras.variacaoAltitudeM)));
            }
        }
        if (evento.envios != null) {
            linha(sb, R.string.alerta_canal_sms, TextosEnvio.sms(requireContext(), evento.envios.sms));
            linha(sb, R.string.alerta_canal_api, TextosEnvio.api(requireContext(), evento.envios.api));
        }
        b.textoDetalhes.setText(sb.toString().trim());
        b.botaoAbrirMapa.setVisibility(evento.localizacao != null ? View.VISIBLE : View.GONE);
        b.botaoAbrirMapa.setOnClickListener(x ->
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(evento.localizacao.linkMapa()))));
    }

    private void mostrarTrajeto() {
        if (trajeto == null || b == null) return;
        b.textoTitulo.setText(getString(R.string.historico_trajeto, Formatos.km(requireContext(), trajeto.distanciaM)));
        StringBuilder sb = new StringBuilder();
        linha(sb, R.string.detalhe_inicio, Formatos.dataHora(trajeto.inicio));
        linha(sb, R.string.detalhe_fim, trajeto.fim != null ? Formatos.dataHora(trajeto.fim) : getString(R.string.historico_trajeto_aberto));
        linha(sb, R.string.detalhe_duracao, Formatos.duracao(trajeto.duracaoSeg));
        linha(sb, R.string.detalhe_media, Formatos.kmh(requireContext(), trajeto.velocidadeMediaKmh));
        linha(sb, R.string.detalhe_perfil, getString(InicioFragment.nomePerfil(
                com.luistech.socorre.domain.modelo.PerfilAtividade.deTexto(trajeto.perfilAtividade))));
        if (trajeto.climaInicio != null) {
            linha(sb, R.string.detalhe_clima, getString(R.string.clima_agora,
                    TextosClima.descricao(requireContext(), trajeto.climaInicio.codigoTempo),
                    Math.round(trajeto.climaInicio.temperaturaC)));
        }
        linha(sb, R.string.detalhe_eventos, String.valueOf(eventosTrajeto.size()));
        b.textoDetalhes.setText(sb.toString().trim());
        b.botaoAbrirMapa.setVisibility(View.GONE);
    }

    private void linha(StringBuilder sb, int rotulo, String valor) {
        sb.append(getString(R.string.detalhe_linha, getString(rotulo), valor)).append('\n');
    }

    private void desenharMapa() {
        if (b == null) return;
        MapView mapa = b.mapa;
        mapa.getOverlays().clear();
        mapa.getOverlays().add(new CopyrightOverlay(requireContext()));
        List<GeoPoint> todos = new ArrayList<>();
        if (trajeto != null && trajeto.polyline != null && !trajeto.polyline.isEmpty()) {
            List<GeoPoint> pontos = new ArrayList<>();
            for (LatLng p : PolyUtil.decode(trajeto.polyline)) pontos.add(new GeoPoint(p.latitude, p.longitude));
            if (!pontos.isEmpty()) {
                Polyline linha = new Polyline(mapa);
                linha.setPoints(pontos);
                linha.getOutlinePaint().setColor(requireContext().getColor(R.color.rota));
                linha.getOutlinePaint().setStrokeWidth(10f);
                mapa.getOverlays().add(linha);
                todos.addAll(pontos);
            }
        }
        List<Evento> marcadores = new ArrayList<>(eventosTrajeto);
        if (evento != null) marcadores.add(evento);
        for (Evento e : marcadores) {
            if (e.localizacao == null) continue;
            GeoPoint p = new GeoPoint(e.localizacao.lat, e.localizacao.lng);
            Marker m = new Marker(mapa);
            m.setPosition(p);
            m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            m.setTitle(getString(DespachanteAlerta.nomeTipo(TipoEvento.deTexto(e.tipo))));
            mapa.getOverlays().add(m);
            todos.add(p);
        }
        boolean carregou = evento != null || trajeto != null;
        mapa.setVisibility(todos.isEmpty() && carregou ? View.GONE : View.VISIBLE);
        b.textoMapaIndisponivel.setVisibility(todos.isEmpty() && carregou ? View.VISIBLE : View.GONE);
        mapa.invalidate();
        if (todos.size() == 1) {
            mapa.getController().setZoom(17.0);
            mapa.getController().setCenter(todos.get(0));
        } else if (todos.size() > 1) {
            BoundingBox caixa = BoundingBox.fromGeoPointsSafe(todos);
            mapa.post(() -> mapa.zoomToBoundingBox(caixa, false, 80));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        b.mapa.onResume();
    }

    @Override
    public void onPause() {
        b.mapa.onPause();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
