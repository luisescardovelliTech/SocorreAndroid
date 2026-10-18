package com.luistech.socorre.di;

import android.content.Context;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.PersistentCacheSettings;
import com.luistech.socorre.data.api.ApiClima;
import com.luistech.socorre.data.api.ApiEventos;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Module
@InstallIn(SingletonComponent.class)
public final class ModuloApp {

    private ModuloApp() {
    }

    @Provides
    @Singleton
    static FirebaseAuth auth() {
        return FirebaseAuth.getInstance();
    }

    @Provides
    @Singleton
    static FirebaseFirestore firestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // Cache persistente explícito: histórico offline e escritas enfileiradas sem rede.
        db.setFirestoreSettings(new FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                .build());
        return db;
    }

    @Provides
    @Singleton
    static FirebaseAnalytics analytics(@ApplicationContext Context contexto) {
        return FirebaseAnalytics.getInstance(contexto);
    }

    @Provides
    @Singleton
    static FusedLocationProviderClient localizacao(@ApplicationContext Context contexto) {
        return LocationServices.getFusedLocationProviderClient(contexto);
    }

    /** Executor para disco/rede fora da thread principal. */
    @Provides
    @Singleton
    static ExecutorService executor() {
        return Executors.newFixedThreadPool(3);
    }

    @Provides
    @Singleton
    static OkHttpClient okHttp() {
        return new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .callTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    @Provides
    @Singleton
    static ApiEventos apiEventos(OkHttpClient cliente) {
        // A URL completa vem do BuildConfig via @Url (API sem autenticação).
        return new Retrofit.Builder()
                .baseUrl("https://ideaberta.com.br/")
                .client(cliente)
                .build()
                .create(ApiEventos.class);
    }

    @Provides
    @Singleton
    static ApiClima apiClima(OkHttpClient cliente) {
        return new Retrofit.Builder()
                .baseUrl("https://api.open-meteo.com/")
                .client(cliente)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiClima.class);
    }
}
