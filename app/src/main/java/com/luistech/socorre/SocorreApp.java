package com.luistech.socorre;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.work.Configuration;

import com.google.firebase.FirebaseApp;
import com.luistech.socorre.data.local.Preferencias;
import com.luistech.socorre.detection.ProvedorLimiares;
import com.luistech.socorre.util.GerenciadorTema;
import com.luistech.socorre.util.Notificacoes;

import javax.inject.Inject;

import dagger.hilt.android.HiltAndroidApp;

/**
 * Hilt sem o plugin Gradle: a classe estende a gerada Hilt_SocorreApp (o Android Studio
 * mostra erro até o primeiro build, que gera a classe).
 */
@HiltAndroidApp(Application.class)
public class SocorreApp extends Hilt_SocorreApp implements Configuration.Provider {

    @Inject
    HiltWorkerFactory fabricaWorkers;
    @Inject
    Preferencias prefs;
    @Inject
    ProvedorLimiares limiares;

    @Override
    public void onCreate() {
        super.onCreate();
        Notificacoes.criarCanais(this);
        GerenciadorTema.aplicar(prefs.modoTema());
        configurarMapa();
        if (firebaseConfigurado(this)) {
            ProvedorAppCheck.instalar();
            limiares.buscar();
        }
    }

    /** OpenStreetMap: identificação exigida pela política de uso dos tiles e cache no armazenamento do app. */
    private void configurarMapa() {
        org.osmdroid.config.IConfigurationProvider osm = org.osmdroid.config.Configuration.getInstance();
        osm.load(this, getSharedPreferences("osmdroid", MODE_PRIVATE));
        osm.setUserAgentValue(getPackageName());
        java.io.File base = new java.io.File(getCacheDir(), "osmdroid");
        osm.setOsmdroidBasePath(base);
        osm.setOsmdroidTileCache(new java.io.File(base, "tiles"));
    }

    /** Falso enquanto o google-services.json não for adicionado (ver README). */
    public static boolean firebaseConfigurado(Context c) {
        return !FirebaseApp.getApps(c).isEmpty();
    }

    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder().setWorkerFactory(fabricaWorkers).build();
    }
}
