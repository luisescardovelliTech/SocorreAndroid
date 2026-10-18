package com.luistech.socorre.weather;

import android.location.Location;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.luistech.socorre.data.api.ApiClima;
import com.luistech.socorre.data.local.Preferencias;
import com.luistech.socorre.domain.modelo.Clima;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Response;

/** Clima atual via Open-Meteo (sem chave) com cache de ~30 min. Falha devolve nulo e o trajeto segue. */
@Singleton
public class ServicoClima {

    static final long CACHE_MS = 30 * 60 * 1000;
    private static final float CACHE_RAIO_M = 10_000;
    private static final String CAMPOS = "temperature_2m,precipitation,weather_code,wind_speed_10m";

    /** Formato do cache local. */
    static class Cache {
        Clima clima;
        double lat;
        double lng;
    }

    private final ApiClima api;
    private final Preferencias prefs;
    private final Gson gson = new Gson();

    @Inject
    public ServicoClima(ApiClima api, Preferencias prefs) {
        this.api = api;
        this.prefs = prefs;
    }

    @WorkerThread
    @Nullable
    public Clima obter(double lat, double lng) {
        Cache cache = lerCache();
        if (cache != null && recente(cache) && distancia(cache.lat, cache.lng, lat, lng) < CACHE_RAIO_M) {
            return cache.clima;
        }
        try {
            Response<JsonObject> r = api.tempoAtual(lat, lng, CAMPOS, "kmh").execute();
            if (!r.isSuccessful() || r.body() == null) return null;
            Clima c = converter(r.body());
            Cache novo = new Cache();
            novo.clima = c;
            novo.lat = lat;
            novo.lng = lng;
            prefs.salvarClimaCache(gson.toJson(novo));
            return c;
        } catch (Exception e) {
            Log.w("Socorre.Clima", "clima indisponivel: " + e.getClass().getSimpleName());
            return null;
        }
    }

    /** Temperatura do cache recente, sem acessar a rede (usada no alerta). */
    @Nullable
    public Double temperaturaRecente() {
        Cache c = lerCache();
        return c != null && recente(c) ? c.clima.temperaturaC : null;
    }

    static Clima converter(JsonObject j) {
        Clima c = new Clima();
        c.obtidoEm = System.currentTimeMillis();
        JsonObject atual = j.getAsJsonObject("current");
        if (atual == null) return c;
        if (atual.has("temperature_2m")) c.temperaturaC = atual.get("temperature_2m").getAsDouble();
        if (atual.has("wind_speed_10m")) c.ventoKmh = atual.get("wind_speed_10m").getAsDouble();
        if (atual.has("weather_code")) c.codigoTempo = atual.get("weather_code").getAsInt();
        boolean precipitando = atual.has("precipitation") && atual.get("precipitation").getAsDouble() > 0;
        c.chuva = precipitando || AvaliadorClima.codigoIndicaChuva(c.codigoTempo);
        return c;
    }

    private static boolean recente(Cache c) {
        return System.currentTimeMillis() - c.clima.obtidoEm < CACHE_MS;
    }

    @Nullable
    private Cache lerCache() {
        String json = prefs.climaCache();
        if (json == null) return null;
        try {
            Cache c = gson.fromJson(json, Cache.class);
            return c != null && c.clima != null ? c : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static float distancia(double lat1, double lng1, double lat2, double lng2) {
        float[] r = new float[1];
        Location.distanceBetween(lat1, lng1, lat2, lng2, r);
        return r[0];
    }
}
