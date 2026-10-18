package com.luistech.socorre.alert;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/** Som de alarme com volume crescente + vibração durante a contagem regressiva. */
@Singleton
public class AlarmeSonoro {

    private static final long RAMPA_MS = 15_000;
    private static final long PASSO_MS = 500;

    private final Context contexto;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private MediaPlayer player;
    private Vibrator vibrador;
    private long inicio;

    private final Runnable rampa = new Runnable() {
        @Override
        public void run() {
            if (player == null) return;
            float v = Math.min(1f, 0.15f + 0.85f * (System.currentTimeMillis() - inicio) / (float) RAMPA_MS);
            player.setVolume(v, v);
            if (v < 1f) handler.postDelayed(this, PASSO_MS);
        }
    };

    @Inject
    public AlarmeSonoro(@ApplicationContext Context contexto) {
        this.contexto = contexto;
    }

    public void iniciar() {
        parar();
        inicio = System.currentTimeMillis();
        try {
            Uri som = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (som == null) som = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            player.setDataSource(contexto, som);
            player.setLooping(true);
            player.setWakeMode(contexto, PowerManager.PARTIAL_WAKE_LOCK);
            player.setVolume(0.15f, 0.15f);
            player.prepare();
            player.start();
            handler.post(rampa);
        } catch (Exception e) {
            // Sem som não pode impedir a contagem: segue só com vibração e notificação.
            Log.w("Socorre.Alarme", "nao foi possivel tocar o alarme", e);
            liberarPlayer();
        }
        vibrar();
    }

    @SuppressWarnings("deprecation")
    private void vibrar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = contexto.getSystemService(VibratorManager.class);
            vibrador = vm != null ? vm.getDefaultVibrator() : null;
        } else {
            vibrador = (Vibrator) contexto.getSystemService(Context.VIBRATOR_SERVICE);
        }
        if (vibrador != null && vibrador.hasVibrator()) {
            vibrador.vibrate(VibrationEffect.createWaveform(new long[]{0, 800, 400}, 0));
        }
    }

    public void parar() {
        handler.removeCallbacks(rampa);
        liberarPlayer();
        if (vibrador != null) {
            vibrador.cancel();
            vibrador = null;
        }
    }

    private void liberarPlayer() {
        if (player != null) {
            try {
                player.stop();
            } catch (IllegalStateException ignorada) {
                // já parado
            }
            player.release();
            player = null;
        }
    }
}
