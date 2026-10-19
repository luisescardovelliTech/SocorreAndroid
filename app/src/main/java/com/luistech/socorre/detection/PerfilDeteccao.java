package com.luistech.socorre.detection;

import com.luistech.socorre.domain.modelo.PerfilAtividade;
import com.luistech.socorre.domain.modelo.Sensibilidade;

public final class PerfilDeteccao {
    public final double impactoMinG;
    public final double rotacaoMinGraus;
    public final double velocidadeMinAntesKmh;
    public final double quedaAltitudeReforcoM;
    public final boolean quedaLivreReforca;

    public PerfilDeteccao(double impactoMinG, double rotacaoMinGraus, double velocidadeMinAntesKmh,
                          double quedaAltitudeReforcoM, boolean quedaLivreReforca) {
        this.impactoMinG = impactoMinG;
        this.rotacaoMinGraus = rotacaoMinGraus;
        this.velocidadeMinAntesKmh = velocidadeMinAntesKmh;
        this.quedaAltitudeReforcoM = quedaAltitudeReforcoM;
        this.quedaLivreReforca = quedaLivreReforca;
    }

    public static PerfilDeteccao padrao(PerfilAtividade perfil) {
        switch (perfil) {
            case BICICLETA:
                return new PerfilDeteccao(3.0, 60, 0, 0, false);
            case MOTO:
                return new PerfilDeteccao(4.0, 60, 15, 0, false);
            case TRABALHO_CAMPO:
                return new PerfilDeteccao(3.0, 60, 0, 1.5, false);
            case CAMINHADA_CASA:
            default:
                return new PerfilDeteccao(2.5, 45, 0, 0, true);
        }
    }

    public PerfilDeteccao comSensibilidade(Sensibilidade s) {
        return new PerfilDeteccao(impactoMinG * s.fator, rotacaoMinGraus * s.fator,
                velocidadeMinAntesKmh, quedaAltitudeReforcoM, quedaLivreReforca);
    }

    public PerfilDeteccao comLimiares(double impactoG, double rotacaoGraus) {
        return new PerfilDeteccao(impactoG, rotacaoGraus, velocidadeMinAntesKmh,
                quedaAltitudeReforcoM, quedaLivreReforca);
    }
}
