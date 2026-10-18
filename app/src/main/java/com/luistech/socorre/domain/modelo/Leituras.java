package com.luistech.socorre.domain.modelo;

/**
 * Leituras de sensores que confirmaram um evento. Gravado em users/{uid}/eventos/{id}.leituras.
 * Campos públicos e construtor vazio para serialização do Firestore.
 */
public class Leituras {
    public double picoG;
    public double rotacaoGraus;
    /** Variação de altitude (m) pelo barômetro. Negativo = queda. Nulo se não houver barômetro. */
    public Double variacaoAltitudeM;
    public Double velocidadeAntesKmh;
    public boolean quedaLivre;
    /** Verdadeiro quando queda livre, altitude ou o próprio aparelho caindo reforçaram a detecção. */
    public boolean reforcoContexto;

    public Leituras() {
    }
}
