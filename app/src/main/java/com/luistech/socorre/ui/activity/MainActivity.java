package com.luistech.socorre.ui.activity;

import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.luistech.socorre.R;
import com.luistech.socorre.SocorreApp;
import com.luistech.socorre.data.local.Preferencias;
import com.luistech.socorre.data.repositorio.RepositorioUsuario;
import com.luistech.socorre.databinding.ActivityMainBinding;
import com.luistech.socorre.util.GerenciadorTema;

import javax.inject.Inject;
import javax.inject.Provider;

import dagger.hilt.android.AndroidEntryPoint;

/** Activity única que hospeda a navegação. Sem regra de negócio. */
@AndroidEntryPoint(AppCompatActivity.class)
public class MainActivity extends Hilt_MainActivity {

    @Inject
    Preferencias prefs;
    /** Provider: só cria o Firebase depois de confirmar que ele está configurado. */
    @Inject
    Provider<RepositorioUsuario> usuarios;

    private GerenciadorTema temaSensor;

    @Override
    protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        if (!SocorreApp.firebaseConfigurado(this)) {
            setContentView(R.layout.activity_sem_firebase);
            return;
        }
        ActivityMainBinding b = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        NavHostFragment host = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host);
        if (host == null) return;
        NavController nav = host.getNavController();
        NavGraph grafo = nav.getNavInflater().inflate(R.navigation.nav_principal);
        grafo.setStartDestination(destinoInicial());
        nav.setGraph(grafo);
        NavigationUI.setupWithNavController(b.navegacaoInferior, nav);
        nav.addOnDestinationChangedListener((c, destino, args) -> {
            int id = destino.getId();
            boolean principal = id == R.id.inicioFragment || id == R.id.historicoFragment
                    || id == R.id.contatosFragment || id == R.id.configuracoesFragment;
            b.navegacaoInferior.setVisibility(principal ? View.VISIBLE : View.GONE);
        });

        if (estado == null && usuarios.get().logado()) sincronizar();
        temaSensor = new GerenciadorTema(getSystemService(SensorManager.class));
    }

    private int destinoInicial() {
        if (!prefs.onboardingConcluido()) return R.id.boasVindasFragment;
        if (!usuarios.get().logado()) return R.id.loginFragment;
        return R.id.inicioFragment;
    }

    /** Atualiza o cache local (contatos, nome, ficha) usado pelo SMS offline. */
    private void sincronizar() {
        RepositorioUsuario repo = usuarios.get();
        repo.carregarUsuario();
        repo.carregarContatos();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (temaSensor != null && Preferencias.TEMA_SENSOR.equals(prefs.modoTema())) temaSensor.iniciar();
    }

    @Override
    protected void onPause() {
        if (temaSensor != null) temaSensor.parar();
        super.onPause();
    }
}
