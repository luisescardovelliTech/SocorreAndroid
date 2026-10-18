package com.luistech.socorre.ui.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.luistech.socorre.R;
import com.luistech.socorre.alert.ControladorEmergencia;
import com.luistech.socorre.alert.DespachanteAlerta;
import com.luistech.socorre.databinding.ActivityContagemBinding;
import com.luistech.socorre.ui.viewmodel.EmergenciaViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Tela 8 - Contagem regressiva. Aparece sobre a tela de bloqueio. A contagem em si roda no
 * ControladorEmergencia: fechar esta tela não cancela o alerta.
 */
@AndroidEntryPoint(AppCompatActivity.class)
public class ContagemActivity extends Hilt_ContagemActivity {

    private ActivityContagemBinding b;
    private EmergenciaViewModel vm;
    private int ultimoAnuncio = -1;

    @Override
    protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        mostrarSobreBloqueio();
        b = ActivityContagemBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        vm = new ViewModelProvider(this).get(EmergenciaViewModel.class);

        b.botaoEstouBem.setOnClickListener(v -> vm.estouBem());
        b.botaoPedirAjuda.setOnClickListener(v -> vm.pedirAjudaAgora());
        vm.estado().observe(this, this::renderizar);
    }

    @SuppressWarnings("deprecation")
    private void mostrarSobreBloqueio() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
    }

    private void renderizar(ControladorEmergencia.Estado e) {
        switch (e.fase) {
            case CONTANDO:
                b.grupoContagem.setVisibility(View.VISIBLE);
                b.grupoEnviando.setVisibility(View.GONE);
                b.textoTipo.setText(getString(R.string.contagem_tipo, getString(DespachanteAlerta.nomeTipo(e.tipo))));
                b.textoNumero.setText(String.valueOf(e.segundos));
                b.textoNumero.setContentDescription(getResources().getQuantityString(
                        R.plurals.contagem_restante_desc, e.segundos, e.segundos));
                b.faixaTeste.setVisibility(e.teste ? View.VISIBLE : View.GONE);
                anunciar(e.segundos);
                break;
            case ENVIANDO:
                b.grupoContagem.setVisibility(View.GONE);
                b.grupoEnviando.setVisibility(View.VISIBLE);
                break;
            case ENVIADO:
                startActivity(new Intent(this, AlertaEnviadoActivity.class));
                finish();
                break;
            case OCIOSO:
            default:
                finish();
                break;
        }
    }

    /** TalkBack: anuncia a cada 10 s e nos últimos 5 s, sem poluir a cada segundo. */
    private void anunciar(int segundos) {
        if (segundos == ultimoAnuncio) return;
        if (ultimoAnuncio == -1 || segundos % 10 == 0 || segundos <= 5) {
            b.textoNumero.announceForAccessibility(b.textoNumero.getContentDescription());
        }
        ultimoAnuncio = segundos;
    }
}
