package com.luistech.socorre;

import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;

/** Debug: o token de depuração aparece no Logcat e deve ser cadastrado no console do App Check. */
final class ProvedorAppCheck {

    private ProvedorAppCheck() {
    }

    static void instalar() {
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance());
    }
}
