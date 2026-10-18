package com.luistech.socorre;

import android.app.Application;
import android.content.Context;

import androidx.test.runner.AndroidJUnitRunner;

import dagger.hilt.android.testing.HiltTestApplication;

/** Runner de testes instrumentados com Hilt (configurado em testInstrumentationRunner). */
public class ExecutorTestesHilt extends AndroidJUnitRunner {

    @Override
    public Application newApplication(ClassLoader cl, String nome, Context contexto)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return super.newApplication(cl, HiltTestApplication.class.getName(), contexto);
    }
}
