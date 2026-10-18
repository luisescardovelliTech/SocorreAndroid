package com.luistech.socorre.detection;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Uma leitura do barômetro (hPa) para o alerta. Nulo se o aparelho não tiver barômetro. */
public final class LeitorPressao {

    private LeitorPressao() {
    }

    @WorkerThread
    @Nullable
    public static Float lerHpa(Context c, long prazoMs) {
        SensorManager sm = c.getSystemService(SensorManager.class);
        Sensor s = sm != null ? sm.getDefaultSensor(Sensor.TYPE_PRESSURE) : null;
        if (s == null) return null;
        float[] valor = {Float.NaN};
        CountDownLatch pronto = new CountDownLatch(1);
        SensorEventListener ouvinte = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent e) {
                valor[0] = e.values[0];
                pronto.countDown();
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int precisao) {
                // não utilizado
            }
        };
        sm.registerListener(ouvinte, s, SensorManager.SENSOR_DELAY_FASTEST);
        try {
            pronto.await(prazoMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            sm.unregisterListener(ouvinte);
        }
        return Float.isNaN(valor[0]) ? null : valor[0];
    }
}
