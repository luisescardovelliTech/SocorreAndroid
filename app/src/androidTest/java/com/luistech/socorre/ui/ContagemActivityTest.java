package com.luistech.socorre.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;

import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;

import com.luistech.socorre.R;
import com.luistech.socorre.alert.ControladorEmergencia;
import com.luistech.socorre.domain.modelo.TipoEvento;
import com.luistech.socorre.ui.activity.ContagemActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.inject.Inject;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

@HiltAndroidTest
public class ContagemActivityTest {

    @Rule
    public HiltAndroidRule hilt = new HiltAndroidRule(this);

    @Inject
    ControladorEmergencia controlador;

    @Before
    public void preparar() {
        hilt.inject();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                controlador.iniciarContagem(TipoEvento.QUEDA, null, true));
    }

    @After
    public void limpar() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> controlador.cancelar());
    }

    @Test
    public void mostraBotoesCriticosEFaixaDeTeste() {
        try (ActivityScenario<ContagemActivity> ignorado = ActivityScenario.launch(ContagemActivity.class)) {
            onView(withId(R.id.botaoEstouBem)).check(matches(isDisplayed()));
            onView(withId(R.id.botaoPedirAjuda)).check(matches(isDisplayed()));
            onView(withId(R.id.faixaTeste)).check(matches(withText(R.string.teste_faixa)));
        }
    }

    @Test
    public void estouBemCancelaAContagem() {
        try (ActivityScenario<ContagemActivity> ignorado = ActivityScenario.launch(ContagemActivity.class)) {
            onView(withId(R.id.botaoEstouBem)).perform(click());
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertEquals(ControladorEmergencia.Fase.OCIOSO, controlador.atual().fase);
        }
    }
}
