package com.luistech.socorre.alert;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.luistech.socorre.data.local.Preferencias;
import com.luistech.socorre.data.repositorio.RepositorioEventos;
import com.luistech.socorre.data.repositorio.RepositorioUsuario;
import com.luistech.socorre.domain.modelo.ContatoEmergencia;
import com.luistech.socorre.domain.modelo.Envios;
import com.luistech.socorre.domain.modelo.Evento;
import com.luistech.socorre.domain.modelo.PerfilAtividade;
import com.luistech.socorre.domain.modelo.StatusEnvio;
import com.luistech.socorre.domain.modelo.TipoEvento;
import com.luistech.socorre.location.GerenciadorLocalizacao;
import com.luistech.socorre.weather.ServicoClima;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

public class DespachanteAlertaTest {

    private Context contexto;
    private Preferencias prefs;
    private GerenciadorLocalizacao localizacao;
    private RepositorioUsuario usuarios;
    private RepositorioEventos eventos;
    private EnviadorSms sms;
    private EnviadorApi api;
    private DespachanteAlerta despachante;

    @Before
    public void preparar() {
        contexto = mock(Context.class);
        prefs = mock(Preferencias.class);
        localizacao = mock(GerenciadorLocalizacao.class);
        usuarios = mock(RepositorioUsuario.class);
        eventos = mock(RepositorioEventos.class);
        sms = mock(EnviadorSms.class);
        api = mock(EnviadorApi.class);
        when(contexto.getString(anyInt())).thenReturn("texto");
        when(prefs.perfil()).thenReturn(PerfilAtividade.MOTO);
        when(prefs.nomeUsuario()).thenReturn("Ana");
        when(prefs.telefoneUsuario()).thenReturn("18999990000");
        when(prefs.contatos()).thenReturn(Collections.singletonList(
                new ContatoEmergencia("Bia", "18988880000", "filha", 1)));
        when(usuarios.uid()).thenReturn("uid1");
        when(eventos.novoId("uid1")).thenReturn("ev1");
        when(localizacao.obterMelhorLocalizacao(anyLong())).thenReturn(null);
        Envios.Sms enviado = new Envios.Sms();
        enviado.status = StatusEnvio.ENVIADO;
        when(sms.enviar(anyList(), anyString(), anyString(), anyString())).thenReturn(enviado);
        despachante = new DespachanteAlerta(contexto, prefs, localizacao, usuarios, eventos, sms, api,
                mock(FirebaseAnalytics.class), mock(ServicoClima.class));
    }

    @Test
    public void falhaDaApiNaoImpedeSms() {
        doThrow(new RuntimeException("fila indisponivel")).when(api).enfileirar(anyString(), anyString(), anyString());
        Evento e = despachante.disparar(TipoEvento.QUEDA, null, false);
        verify(sms).enviar(anyList(), anyString(), eq("uid1"), eq("ev1"));
        assertEquals(StatusEnvio.ENVIADO, e.envios.sms.status);
        assertEquals(StatusEnvio.FALHOU, e.envios.api.status);
    }

    @Test
    public void semGpsAindaEnviaSmsSemLocalizacao() {
        Evento e = despachante.disparar(TipoEvento.PANICO, null, false);
        assertEquals(null, e.localizacao);
        verify(sms).enviar(anyList(), anyString(), anyString(), anyString());
    }

    @Test
    public void modoTesteNaoEnviaSmsMasEnviaParaApi() {
        Evento e = despachante.disparar(TipoEvento.QUEDA, null, true);
        verify(sms, never()).enviar(anyList(), anyString(), anyString(), anyString());
        assertEquals(StatusEnvio.SIMULADO, e.envios.sms.status);
        assertEquals(true, e.teste);
        verify(api).enfileirar(eq("uid1"), eq("ev1"), anyString());
    }

    @Test
    public void falhaNoFirestoreNaoImpedeSms() {
        doThrow(new RuntimeException("firestore")).when(eventos).salvar(anyString(), any(Evento.class));
        Evento e = despachante.disparar(TipoEvento.QUEDA, null, false);
        assertEquals(StatusEnvio.ENVIADO, e.envios.sms.status);
    }
}
