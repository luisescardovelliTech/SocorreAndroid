package com.luistech.socorre.ui.viewmodel;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.luistech.socorre.data.local.Preferencias;
import com.luistech.socorre.detection.DetectorQueda;
import com.luistech.socorre.detection.ProvedorLimiares;
import com.luistech.socorre.domain.modelo.PerfilAtividade;
import com.luistech.socorre.location.GerenciadorLocalizacao;
import com.luistech.socorre.security.GerenciadorPermissoes;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;

/** Leituras ao vivo para testes em aparelho físico (atualiza a ~10 Hz). */
@HiltViewModel
public class DiagnosticoViewModel extends ViewModel implements SensorEventListener {

    /** Fotografia das leituras. Valores nulos = sensor ausente. */
    public static final class Leitura {
        public float[] acel = new float[3];
        public double g;
        public double picoG;
        public float[] giro;
        public Float proximidadeCm;
        public Float proximidadeMax;
        public Float pressaoHpa;
        public Float altitudeM;
        public Float luzLux;
        public boolean gpsLigado;
        public boolean gpsPermitido;
        /** Mesmo algoritmo do monitoramento, rodando aqui só para teste (não dispara alerta). */
        public DetectorQueda.Estado detector;
        public String perfil;
        public int quedasConfirmadas;
    }

    private static final long INTERVALO_UI_MS = 100;

    private final Context contexto;
    private final SensorManager sensores;
    private final GerenciadorLocalizacao localizacao;
    private final MutableLiveData<Leitura> leitura = new MutableLiveData<>();
    private Leitura atual = new Leitura();
    private long ultimaPublicacao;
    private final DetectorQueda detector;

    @Inject
    public DiagnosticoViewModel(@ApplicationContext Context contexto, GerenciadorLocalizacao localizacao,
                                Preferencias prefs, ProvedorLimiares limiares) {
        this.contexto = contexto;
        this.sensores = contexto.getSystemService(SensorManager.class);
        this.localizacao = localizacao;
        if (sensores.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) atual.giro = new float[3];
        PerfilAtividade perfil = prefs.perfil();
        detector = new DetectorQueda(limiares.perfil(perfil, prefs.sensibilidade(perfil)));
        atual.perfil = perfil.name();
        atual.detector = detector.estado();
    }

    public LiveData<Leitura> leitura() {
        return leitura;
    }

    /** Disponibilidade de cada sensor usado pela detecção. */
    public boolean tem(int tipo) {
        return sensores.getDefaultSensor(tipo) != null;
    }

    public void iniciar() {
        int[] tipos = {Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GYROSCOPE, Sensor.TYPE_PROXIMITY,
                Sensor.TYPE_PRESSURE, Sensor.TYPE_LIGHT};
        for (int t : tipos) {
            Sensor s = sensores.getDefaultSensor(t);
            if (s != null) sensores.registerListener(this, s, SensorManager.SENSOR_DELAY_GAME);
        }
        atual.gpsLigado = localizacao.gpsLigado();
        atual.gpsPermitido = GerenciadorPermissoes.temLocalizacao(contexto);
    }

    public void parar() {
        sensores.unregisterListener(this);
    }

    public void zerarPico() {
        atual.picoG = 0;
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        switch (e.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                System.arraycopy(e.values, 0, atual.acel, 0, 3);
                atual.g = Math.sqrt(e.values[0] * e.values[0] + e.values[1] * e.values[1]
                        + e.values[2] * e.values[2]) / DetectorQueda.GRAVIDADE;
                atual.picoG = Math.max(atual.picoG, atual.g);
                float[] gi = atual.giro != null ? atual.giro : new float[3];
                if (detector.processar(e.timestamp / 1_000_000L, e.values[0], e.values[1], e.values[2],
                        gi[0], gi[1], gi[2]) != null) {
                    atual.quedasConfirmadas++;
                }
                atual.detector = detector.estado();
                break;
            case Sensor.TYPE_GYROSCOPE:
                if (atual.giro != null) System.arraycopy(e.values, 0, atual.giro, 0, 3);
                break;
            case Sensor.TYPE_PROXIMITY:
                atual.proximidadeCm = e.values[0];
                atual.proximidadeMax = e.sensor.getMaximumRange();
                detector.atualizarProximidade(e.values[0] < Math.min(5f, e.sensor.getMaximumRange()));
                break;
            case Sensor.TYPE_PRESSURE:
                atual.pressaoHpa = e.values[0];
                atual.altitudeM = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, e.values[0]);
                detector.atualizarAltitude(atual.altitudeM);
                break;
            case Sensor.TYPE_LIGHT:
                atual.luzLux = e.values[0];
                break;
            default:
                break;
        }
        long agora = SystemClock.elapsedRealtime();
        if (agora - ultimaPublicacao >= INTERVALO_UI_MS) {
            ultimaPublicacao = agora;
            leitura.setValue(atual);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int precisao) {
        // não utilizado
    }

    @Override
    protected void onCleared() {
        parar();
    }
}
