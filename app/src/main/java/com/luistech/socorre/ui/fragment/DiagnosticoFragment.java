package com.luistech.socorre.ui.fragment;

import android.hardware.Sensor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.luistech.socorre.R;
import com.luistech.socorre.databinding.FragmentDiagnosticoBinding;
import com.luistech.socorre.detection.DetectorQueda;
import com.luistech.socorre.domain.modelo.PerfilAtividade;
import com.luistech.socorre.ui.viewmodel.DiagnosticoViewModel;
import com.luistech.socorre.util.Formatos;

import dagger.hilt.android.AndroidEntryPoint;

/** Tela 14 - Diagnóstico de sensores em tempo real (para testes em aparelho físico). */
@AndroidEntryPoint(Fragment.class)
public class DiagnosticoFragment extends Hilt_DiagnosticoFragment {

    private FragmentDiagnosticoBinding b;
    private DiagnosticoViewModel vm;

    @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentDiagnosticoBinding.inflate(i, c, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        vm = new ViewModelProvider(this).get(DiagnosticoViewModel.class);
        b.textoDisponiveis.setText(getString(R.string.diag_disponiveis,
                sim(vm.tem(Sensor.TYPE_ACCELEROMETER)), sim(vm.tem(Sensor.TYPE_GYROSCOPE)),
                sim(vm.tem(Sensor.TYPE_PROXIMITY)), sim(vm.tem(Sensor.TYPE_PRESSURE)), sim(vm.tem(Sensor.TYPE_LIGHT))));
        b.botaoZerarPico.setOnClickListener(x -> vm.zerarPico());
        vm.leitura().observe(getViewLifecycleOwner(), this::mostrar);
    }

    private String sim(boolean v) {
        return getString(v ? R.string.diag_sim : R.string.diag_nao);
    }

    private void mostrar(DiagnosticoViewModel.Leitura l) {
        b.textoAcelerometro.setText(getString(R.string.diag_acel, Formatos.decimal(l.acel[0]),
                Formatos.decimal(l.acel[1]), Formatos.decimal(l.acel[2]), String.format(Formatos.PT_BR, "%.2f", l.g)));
        b.textoPico.setText(getString(R.string.diag_pico, String.format(Formatos.PT_BR, "%.2f", l.picoG)));
        b.textoGiroscopio.setText(l.giro == null ? getString(R.string.diag_indisponivel)
                : getString(R.string.diag_giro, String.format(Formatos.PT_BR, "%.2f", l.giro[0]),
                String.format(Formatos.PT_BR, "%.2f", l.giro[1]), String.format(Formatos.PT_BR, "%.2f", l.giro[2])));
        if (l.proximidadeCm == null) {
            b.textoProximidade.setText(R.string.diag_indisponivel);
        } else {
            boolean coberto = l.proximidadeCm < Math.min(5f, l.proximidadeMax);
            b.textoProximidade.setText(getString(R.string.diag_proximidade,
                    getString(coberto ? R.string.diag_coberto : R.string.diag_livre), Formatos.decimal(l.proximidadeCm)));
        }
        b.textoBarometro.setText(l.pressaoHpa == null ? getString(R.string.diag_indisponivel)
                : getString(R.string.diag_barometro, Formatos.decimal(l.pressaoHpa), Formatos.decimal(l.altitudeM)));
        b.textoLuz.setText(l.luzLux == null ? getString(R.string.diag_indisponivel)
                : getString(R.string.diag_luz, Formatos.decimal(l.luzLux)));
        b.textoDetector.setText(textoDetector(l));
        b.textoGps.setText(getString(R.string.diag_gps,
                getString(l.gpsPermitido ? R.string.diag_permitido : R.string.diag_nao_permitido),
                getString(l.gpsLigado ? R.string.diag_ligado : R.string.diag_desligado)));
    }

    private String textoDetector(DiagnosticoViewModel.Leitura l) {
        DetectorQueda.Estado e = l.detector;
        if (e == null) return "";
        String perfil = getString(InicioFragment.nomePerfil(PerfilAtividade.deTexto(l.perfil)));
        String fase;
        if (e.analisando) {
            fase = getString(R.string.diag_det_analisando, String.format(Formatos.PT_BR, "%.1f", e.picoG),
                    Math.round(e.rotacaoGraus));
        } else if (e.emSilencio) {
            fase = getString(R.string.diag_det_silencio);
        } else {
            fase = getString(R.string.diag_det_aguardando);
        }
        return getString(R.string.diag_det_resumo, perfil, String.format(Formatos.PT_BR, "%.1f", e.limiarImpactoG),
                fase, getString(textoMotivo(e.ultimoResultado)), l.quedasConfirmadas);
    }

    private static int textoMotivo(DetectorQueda.Motivo m) {
        switch (m) {
            case CONFIRMADA:
                return R.string.diag_motivo_confirmada;
            case ROTACAO_BAIXA:
                return R.string.diag_motivo_rotacao;
            case VELOCIDADE_ANTES_BAIXA:
                return R.string.diag_motivo_velocidade;
            case NENHUM:
            default:
                return R.string.diag_motivo_nenhum;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        vm.iniciar();
    }

    @Override
    public void onPause() {
        vm.parar();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
