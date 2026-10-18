package com.luistech.socorre.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.luistech.socorre.alert.ControladorEmergencia;
import com.luistech.socorre.data.local.Preferencias;
import com.luistech.socorre.data.repositorio.RepositorioTrajetos;
import com.luistech.socorre.domain.caso_de_uso.IniciarTrajeto;
import com.luistech.socorre.domain.modelo.TipoEvento;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class TrajetoViewModel extends ViewModel {

    private final RepositorioTrajetos trajetos;
    private final IniciarTrajeto trajeto;
    private final Preferencias prefs;
    private final ControladorEmergencia controlador;

    @Inject
    public TrajetoViewModel(RepositorioTrajetos trajetos, IniciarTrajeto trajeto, Preferencias prefs,
                            ControladorEmergencia controlador) {
        this.trajetos = trajetos;
        this.trajeto = trajeto;
        this.prefs = prefs;
        this.controlador = controlador;
    }

    public LiveData<RepositorioTrajetos.Resumo> resumo() {
        return trajetos.resumoAoVivo();
    }

    public boolean ativo() {
        return prefs.trajetoAtivoId() != null;
    }

    public int intervaloCheckin() {
        return prefs.intervaloCheckinMin();
    }

    public void alterarIntervalo(int minutos) {
        trajeto.alterarIntervaloCheckin(minutos);
    }

    public void encerrar() {
        trajeto.encerrar();
    }

    public void panico() {
        controlador.dispararImediato(TipoEvento.PANICO, false);
    }
}
