package com.luistech.socorre;

import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;

final class ProvedorAppCheck {

    private ProvedorAppCheck() {
    }

    static void instalar() {
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance());
    }
}
