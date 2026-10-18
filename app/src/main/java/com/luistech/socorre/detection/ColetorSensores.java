package com.luistech.socorre.detection;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;

import com.luistech.socorre.domain.modelo.Leituras;

/**
 * Ponte entre o SensorManager e o DetectorQueda. Todos os eventos chegam na thread do
 * Handler informado, que é a mesma usada para a velocidade do GPS (o detector não é thread-safe).
 * Sensores ausentes (giroscópio, barômetro, proximidade) apenas reduzem as evidências.
 */
public class ColetorSensores implements SensorEventListener {

    public interface Ouvinte {
        /** brusco = disparo do modo de teste "qualquer movimento brusco". */
        void aoConfirmarQueda(Leituras leituras, boolean brusco);
    }

    /** Pico (g) que conta como movimento brusco no modo de teste. Calibre aqui se for sensível demais. */
    static final double LIMIAR_BRUSCO_G = 2.0;
    private static final long INTERVALO_BRUSCO_MS = 5_000;
    private java.util.function.BooleanSupplier modoBrusco = () -> false;
    private long ultimoBrusco = -INTERVALO_BRUSCO_MS;

    private final SensorManager sensores;
    private final DetectorQueda detector;
    private final Ouvinte ouvinte;
    private final Sensor acelerometro;
    private final Sensor giroscopio;
    private final Sensor proximidade;
    private final Sensor barometro;
    private double gx, gy, gz;
    private boolean ativo;

    public ColetorSensores(SensorManager sensores, DetectorQueda detector, Ouvinte ouvinte) {
        this.sensores = sensores;
        this.detector = detector;
        this.ouvinte = ouvinte;
        acelerometro = sensores.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        giroscopio = sensores.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        proximidade = sensores.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        barometro = sensores.getDefaultSensor(Sensor.TYPE_PRESSURE);
    }

    /** ~50 Hz com SENSOR_DELAY_GAME (não exige a permissão HIGH_SAMPLING_RATE_SENSORS). */
    public boolean iniciar(Handler handler) {
        if (ativo) return true;
        if (acelerometro == null) return false;
        sensores.registerListener(this, acelerometro, SensorManager.SENSOR_DELAY_GAME, handler);
        if (giroscopio != null) sensores.registerListener(this, giroscopio, SensorManager.SENSOR_DELAY_GAME, handler);
        if (proximidade != null) sensores.registerListener(this, proximidade, SensorManager.SENSOR_DELAY_NORMAL, handler);
        if (barometro != null) sensores.registerListener(this, barometro, SensorManager.SENSOR_DELAY_NORMAL, handler);
        ativo = true;
        return true;
    }

    public void parar() {
        sensores.unregisterListener(this);
        ativo = false;
    }

    public void usarModoBrusco(java.util.function.BooleanSupplier ativo) {
        modoBrusco = ativo;
    }

    public DetectorQueda detector() {
        return detector;
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        switch (e.sensor.getType()) {
            case Sensor.TYPE_GYROSCOPE:
                gx = e.values[0];
                gy = e.values[1];
                gz = e.values[2];
                break;
            case Sensor.TYPE_ACCELEROMETER:
                long t = e.timestamp / 1_000_000L;
                Leituras r = detector.processar(t, e.values[0], e.values[1], e.values[2], gx, gy, gz);
                if (r != null) {
                    ouvinte.aoConfirmarQueda(r, false);
                } else if (modoBrusco.getAsBoolean() && t - ultimoBrusco > INTERVALO_BRUSCO_MS) {
                    double g = Math.sqrt(e.values[0] * e.values[0] + e.values[1] * e.values[1]
                            + e.values[2] * e.values[2]) / SensorManager.GRAVITY_EARTH;
                    if (g >= LIMIAR_BRUSCO_G) {
                        ultimoBrusco = t;
                        Leituras b = new Leituras();
                        b.picoG = g;
                        ouvinte.aoConfirmarQueda(b, true);
                    }
                }
                break;
            case Sensor.TYPE_PROXIMITY:
                float alcance = Math.min(5f, e.sensor.getMaximumRange());
                detector.atualizarProximidade(e.values[0] < alcance);
                break;
            case Sensor.TYPE_PRESSURE:
                detector.atualizarAltitude(SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, e.values[0]));
                break;
            default:
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int precisao) {
        // não utilizado
    }
}
