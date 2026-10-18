package com.luistech.socorre.util;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;

import androidx.appcompat.app.AppCompatDelegate;

import com.luistech.socorre.data.local.Preferencias;

/**
 * Tema: sistema, claro, escuro ou automático pelo sensor de luz ambiente.
 * No modo automático, histerese (10/40 lux) e intervalo mínimo evitam piscar.
 */
public class GerenciadorTema implements SensorEventListener {

    private static final float ESCURO_LUX = 10f;
    private static final float CLARO_LUX = 40f;
    private static final long INTERVALO_MIN_MS = 15_000;

    private final SensorManager sensores;
    private long ultimaTroca;

    public GerenciadorTema(SensorManager sensores) {
        this.sensores = sensores;
    }

    public static void aplicar(String modo) {
        int noturno;
        switch (modo) {
            case Preferencias.TEMA_CLARO:
                noturno = AppCompatDelegate.MODE_NIGHT_NO;
                break;
            case Preferencias.TEMA_ESCURO:
                noturno = AppCompatDelegate.MODE_NIGHT_YES;
                break;
            case Preferencias.TEMA_SENSOR:
                // Começa seguindo o sistema até a primeira leitura do sensor.
                if (AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_UNSPECIFIED) return;
                noturno = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                break;
            case Preferencias.TEMA_SISTEMA:
            default:
                noturno = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                break;
        }
        AppCompatDelegate.setDefaultNightMode(noturno);
    }

    /** Liga o sensor de luz (chamar em onResume quando o modo for SENSOR). */
    public boolean iniciar() {
        Sensor luz = sensores.getDefaultSensor(Sensor.TYPE_LIGHT);
        if (luz == null) return false;
        sensores.registerListener(this, luz, SensorManager.SENSOR_DELAY_NORMAL);
        return true;
    }

    public void parar() {
        sensores.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        float lux = e.values[0];
        int atual = AppCompatDelegate.getDefaultNightMode();
        int desejado = lux < ESCURO_LUX ? AppCompatDelegate.MODE_NIGHT_YES
                : lux > CLARO_LUX ? AppCompatDelegate.MODE_NIGHT_NO : atual;
        long agora = SystemClock.elapsedRealtime();
        if (desejado != atual && agora - ultimaTroca > INTERVALO_MIN_MS) {
            ultimaTroca = agora;
            AppCompatDelegate.setDefaultNightMode(desejado);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int precisao) {
        // não utilizado
    }
}
