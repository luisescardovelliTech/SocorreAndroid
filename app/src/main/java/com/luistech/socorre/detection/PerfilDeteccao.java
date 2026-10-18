package com.luistech.socorre.detection;

import com.luistech.socorre.domain.modelo.PerfilAtividade;
import com.luistech.socorre.domain.modelo.Sensibilidade;

/**
 * Limiares da detecção, isolados do motor (DetectorQueda). Imutável.
 * Os valores padrão podem ser sobrescritos pelo Remote Config (ver ProvedorLimiares).
 */
public final class PerfilDeteccao {
    public final double impactoMinG;
    public final double rotacaoMinGraus;
    /** Velocidade mínima antes do impacto (km/h). 0 = não exige. Só avaliada quando há GPS. */
    public final double velocidadeMinAntesKmh;
    /** Queda de altitude (m) que reforça a detecção. 0 = desativado. */
    public final double quedaAltitudeReforcoM;
    /** Se a fase de queda livre (opcional) reforça a detecção. */
    public final boolean quedaLivreReforca;

    public PerfilDeteccao(double impactoMinG, double rotacaoMinGraus, double velocidadeMinAntesKmh,
                          double quedaAltitudeReforcoM, boolean quedaLivreReforca) {
        this.impactoMinG = impactoMinG;
        this.rotacaoMinGraus = rotacaoMinGraus;
        this.velocidadeMinAntesKmh = velocidadeMinAntesKmh;
        this.quedaAltitudeReforcoM = quedaAltitudeReforcoM;
        this.quedaLivreReforca = quedaLivreReforca;
    }

    /** Limiares iniciais definidos na especificação. */
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

    /** Aplica a sensibilidade escolhida aos limiares de impacto e rotação. */
    public PerfilDeteccao comSensibilidade(Sensibilidade s) {
        return new PerfilDeteccao(impactoMinG * s.fator, rotacaoMinGraus * s.fator,
                velocidadeMinAntesKmh, quedaAltitudeReforcoM, quedaLivreReforca);
    }

    /** Substitui os limiares de impacto e rotação (usado pelo Remote Config). */
    public PerfilDeteccao comLimiares(double impactoG, double rotacaoGraus) {
        return new PerfilDeteccao(impactoG, rotacaoGraus, velocidadeMinAntesKmh,
                quedaAltitudeReforcoM, quedaLivreReforca);
    }
}
