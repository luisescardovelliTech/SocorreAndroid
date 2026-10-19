package com.luistech.socorre.detection;

import android.util.Log;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.luistech.socorre.domain.modelo.PerfilAtividade;
import com.luistech.socorre.domain.modelo.Sensibilidade;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ProvedorLimiares {

    private FirebaseRemoteConfig config;

    @Inject
    public ProvedorLimiares() {
    }

    public void buscar() {
        try {
            config = FirebaseRemoteConfig.getInstance();
            config.setDefaultsAsync(padroes());
            config.fetchAndActivate();
        } catch (Exception e) {
            Log.w("Socorre.Limiares", "remote config indisponivel, usando padroes");
        }
    }

    public PerfilDeteccao perfil(PerfilAtividade p, Sensibilidade s) {
        PerfilDeteccao base = PerfilDeteccao.padrao(p);
        if (config != null) {
            base = base.comLimiares(
                    valor(chave(p, "impacto_g"), base.impactoMinG),
                    valor(chave(p, "rotacao_graus"), base.rotacaoMinGraus));
        }
        return base.comSensibilidade(s);
    }

    private double valor(String chave, double padrao) {
        double v = config.getDouble(chave);
        return v > 0 ? v : padrao;
    }

    static String chave(PerfilAtividade p, String sufixo) {
        return "deteccao_" + p.name().toLowerCase(Locale.ROOT) + "_" + sufixo;
    }

    private static Map<String, Object> padroes() {
        Map<String, Object> m = new HashMap<>();
        for (PerfilAtividade p : PerfilAtividade.values()) {
            PerfilDeteccao d = PerfilDeteccao.padrao(p);
            m.put(chave(p, "impacto_g"), d.impactoMinG);
            m.put(chave(p, "rotacao_graus"), d.rotacaoMinGraus);
        }
        return m;
    }
}
