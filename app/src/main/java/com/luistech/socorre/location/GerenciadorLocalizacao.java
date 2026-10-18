package com.luistech.socorre.location;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.Tasks;
import com.luistech.socorre.domain.modelo.Localizacao;
import com.luistech.socorre.security.GerenciadorPermissoes;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/** Fused Location Provider: posição para alertas (melhor esforço) e atualizações de trajeto. */
@Singleton
public class GerenciadorLocalizacao {

    private static final String TAG = "Socorre.Localizacao";
    public static final String ORIGEM_ATUAL = "ATUAL";
    public static final String ORIGEM_ULTIMA = "ULTIMA_CONHECIDA";
    private static final long FRESCA_MS = 60_000;
    private static final float PRECISAO_BOA_M = 30;

    private final Context contexto;
    private final FusedLocationProviderClient cliente;

    @Inject
    public GerenciadorLocalizacao(@ApplicationContext Context contexto, FusedLocationProviderClient cliente) {
        this.contexto = contexto;
        this.cliente = cliente;
    }

    /**
     * Melhor localização disponível dentro do prazo. Bloqueante: nunca chamar na thread principal.
     * @return nulo se não houver permissão ou nenhuma posição conhecida.
     */
    @WorkerThread
    @SuppressLint("MissingPermission")
    @Nullable
    public Localizacao obterMelhorLocalizacao(long prazoMs) {
        if (!GerenciadorPermissoes.temLocalizacao(contexto)) return null;
        Location ultima = null;
        try {
            ultima = Tasks.await(cliente.getLastLocation(), 1, TimeUnit.SECONDS);
            if (ultima != null && idadeMs(ultima) < FRESCA_MS && ultima.getAccuracy() <= PRECISAO_BOA_M) {
                return converter(ultima, ORIGEM_ATUAL);
            }
        } catch (Exception e) {
            Log.w(TAG, "ultima localizacao indisponivel: " + e.getClass().getSimpleName());
        }
        CancellationTokenSource cancelamento = new CancellationTokenSource();
        try {
            CurrentLocationRequest req = new CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .setDurationMillis(prazoMs)
                    .setMaxUpdateAgeMillis(FRESCA_MS)
                    .build();
            Location atual = Tasks.await(cliente.getCurrentLocation(req, cancelamento.getToken()),
                    prazoMs, TimeUnit.MILLISECONDS);
            if (atual != null) return converter(atual, ORIGEM_ATUAL);
        } catch (Exception e) {
            cancelamento.cancel();
            Log.w(TAG, "localizacao atual falhou/expirou: " + e.getClass().getSimpleName());
        }
        return ultima != null ? converter(ultima, ORIGEM_ULTIMA) : null;
    }

    @SuppressLint("MissingPermission")
    public boolean iniciarAtualizacoes(LocationCallback callback, Looper looper) {
        if (!GerenciadorPermissoes.temLocalizacao(contexto)) return false;
        try {
            LocationRequest req = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000)
                    .setMinUpdateIntervalMillis(2_000)
                    .setMinUpdateDistanceMeters(5)
                    .build();
            cliente.requestLocationUpdates(req, callback, looper);
            return true;
        } catch (SecurityException e) {
            Log.w(TAG, "sem permissao para atualizacoes de localizacao");
            return false;
        }
    }

    public void pararAtualizacoes(LocationCallback callback) {
        cliente.removeLocationUpdates(callback);
    }

    public boolean gpsLigado() {
        LocationManager lm = (LocationManager) contexto.getSystemService(Context.LOCATION_SERVICE);
        return lm != null && lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private static long idadeMs(Location l) {
        return (android.os.SystemClock.elapsedRealtimeNanos() - l.getElapsedRealtimeNanos()) / 1_000_000;
    }

    private static Localizacao converter(Location l, String origem) {
        return new Localizacao(l.getLatitude(), l.getLongitude(), l.getAccuracy(), l.getTime(), origem);
    }
}
