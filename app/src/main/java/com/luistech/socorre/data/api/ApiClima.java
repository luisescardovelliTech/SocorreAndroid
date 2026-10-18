package com.luistech.socorre.data.api;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/** Open-Meteo - tempo atual (https://open-meteo.com/en/docs). Gratuito e sem chave. */
public interface ApiClima {

    @GET("v1/forecast")
    Call<JsonObject> tempoAtual(@Query("latitude") double lat,
                                @Query("longitude") double lon,
                                @Query("current") String campos,
                                @Query("wind_speed_unit") String unidadeVento);
}
