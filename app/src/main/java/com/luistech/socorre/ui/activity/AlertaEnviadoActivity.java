package com.luistech.socorre.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.luistech.socorre.R;
import com.luistech.socorre.alert.ControladorEmergencia;
import com.luistech.socorre.alert.DespachanteAlerta;
import com.luistech.socorre.databinding.ActivityAlertaEnviadoBinding;
import com.luistech.socorre.domain.modelo.Evento;
import com.luistech.socorre.domain.modelo.TipoEvento;
import com.luistech.socorre.ui.viewmodel.EmergenciaViewModel;
import com.luistech.socorre.util.Formatos;
import com.luistech.socorre.util.TextosEnvio;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/** Tela 9 - Alerta enviado: resultado de cada canal e, no modo de teste, o que seria enviado. */
@AndroidEntryPoint(AppCompatActivity.class)
public class AlertaEnviadoActivity extends Hilt_AlertaEnviadoActivity {

    private ActivityAlertaEnviadoBinding b;
    private EmergenciaViewModel vm;

    @Override
    protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) setShowWhenLocked(true);
        b = ActivityAlertaEnviadoBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        vm = new ViewModelProvider(this).get(EmergenciaViewModel.class);

        b.botaoEncerrar.setOnClickListener(v -> {
            vm.encerrar();
            finish();
        });
        vm.estado().observe(this, e -> {
            if (e.fase == ControladorEmergencia.Fase.ENVIADO) vm.acompanhar(e.evento);
            else if (e.fase == ControladorEmergencia.Fase.OCIOSO && vm.eventoAoVivo().getValue() == null) finish();
        });
        vm.eventoAoVivo().observe(this, this::renderizar);
    }

    private void renderizar(Evento e) {
        if (e == null) return;
        TipoEvento tipo = TipoEvento.deTexto(e.tipo);
        b.textoTitulo.setText(e.teste ? R.string.alerta_titulo_teste : R.string.alerta_titulo);
        b.faixaTeste.setVisibility(e.teste ? View.VISIBLE : View.GONE);
        b.textoResumo.setText(getString(R.string.alerta_resumo, getString(DespachanteAlerta.nomeTipo(tipo)),
                Formatos.dataHora(e.dataHora)));

        if (e.localizacao != null) {
            b.textoLocalizacao.setText(getString(R.string.alerta_localizacao_precisao, Math.round(e.localizacao.precisaoM)));
            b.botaoMapa.setVisibility(View.VISIBLE);
            String link = e.localizacao.linkMapa();
            b.botaoMapa.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link))));
        } else {
            b.textoLocalizacao.setText(R.string.alerta_localizacao_indisponivel);
            b.botaoMapa.setVisibility(View.GONE);
        }

        if (e.envios != null) {
            b.textoSms.setText(TextosEnvio.sms(this, e.envios.sms));
            b.textoApi.setText(TextosEnvio.api(this, e.envios.api));
        }

        if (e.teste && e.previaTeste != null) {
            b.grupoPrevia.setVisibility(View.VISIBLE);
            b.textoPreviaSms.setText(String.valueOf(e.previaTeste.get("sms")));
            Object dest = e.previaTeste.get("destinatarios");
            b.textoPreviaDestinatarios.setText(dest instanceof List
                    ? android.text.TextUtils.join("\n", (List<?>) dest) : getString(R.string.nenhum));
            b.textoPreviaPayload.setText(String.valueOf(e.previaTeste.get("payload")));
        } else {
            b.grupoPrevia.setVisibility(View.GONE);
        }
    }
}
