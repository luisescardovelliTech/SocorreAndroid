package com.luistech.socorre.detection;

import com.luistech.socorre.domain.modelo.Leituras;

/**
 * Motor de detecção de quedas. Java puro (sem classes Android) para ser testável com JUnit.
 *
 * Abre a análise com qualquer um destes sinais:
 *   1. caiu ou foi jogado: queda livre (>= 200 ms quase sem peso) seguida de um pouso de 1,3 g ou mais;
 *   2. girou: o aparelho deu uma volta completa (360°) em até 1 s;
 *   3. impacto forte: |a| >= limiar do perfil (+0,5 g se estiver no bolso).
 * A decisão sai 1 s depois (tempo de medir pico e giro), sem esperar o aparelho ficar parado:
 * quem filtra o alarme falso é a contagem regressiva com o botão "Estou bem".
 * No impacto forte sem queda livre nem giro ainda se exige rotação e, quando há GPS, a velocidade
 * mínima do perfil antes do impacto (moto), para buraco e pancada no bolso não dispararem.
 *
 * Tempos em milissegundos, todos na mesma base de relógio (timestamp dos sensores).
 * Não é thread-safe: chamar sempre da mesma thread.
 */
public class DetectorQueda {

    public static final double GRAVIDADE = 9.81;
    static final int CAPACIDADE_BUFFER = 200;          // ~4 s a 50 Hz
    static final double ACRESCIMO_BOLSO_G = 0.5;
    /** Pausa após cada detecção, alerta ou "Estou bem", para o mesmo tombo não contar duas vezes. */
    public static final long SILENCIO_MS = 3_000;
    /** Janela antes (contexto) e depois (pico e giro) do impacto. */
    static final long JANELA_MS = 1_000;
    static final double QUEDA_LIVRE_G = 0.5;
    // ponytail: um pulo com o celular no bolso também tem 200 ms de queda livre e abre a contagem;
    // aumentar para ~350 ms se os alarmes falsos incomodarem.
    static final long QUEDA_LIVRE_MIN_MS = 200;
    /** Depois da queda livre, um pouso macio (cama, sofá, grama) já conta como impacto. */
    static final double IMPACTO_POS_QUEDA_LIVRE_G = 1.3;
    /** Volta completa em até 1 s = aparelho girando (balançar para lá e para cá não soma). */
    static final double GIRO_DISPARO_GRAUS = 360;
    static final long VALIDADE_VELOCIDADE_MS = 10_000;
    static final double FATOR_REFORCO_ROTACAO = 0.75;

    /** Resultado da última análise (para diagnóstico e calibração no aparelho). */
    public enum Motivo { NENHUM, CONFIRMADA, ROTACAO_BAIXA, VELOCIDADE_ANTES_BAIXA }

    /** Fotografia do detector para a tela de diagnóstico. */
    public static final class Estado {
        public final boolean analisando;
        public final boolean emSilencio;
        public final double limiarImpactoG;
        public final double picoG;
        public final double rotacaoGraus;
        public final Motivo ultimoResultado;

        Estado(boolean analisando, boolean emSilencio, double limiarImpactoG, double picoG, double rotacaoGraus,
               Motivo ultimoResultado) {
            this.analisando = analisando;
            this.emSilencio = emSilencio;
            this.limiarImpactoG = limiarImpactoG;
            this.picoG = picoG;
            this.rotacaoGraus = rotacaoGraus;
            this.ultimoResultado = ultimoResultado;
        }
    }

    /** Amostra combinada de acelerômetro + giroscópio. */
    static final class Amostra {
        final long t;
        final double ax, ay, az, g, giro;
        /** Rotação feita nesta amostra (rad) em cada eixo, para somar a rotação líquida. */
        final double rx, ry, rz;
        final Double altitude;

        Amostra(long t, double ax, double ay, double az, double g, double giro,
                double rx, double ry, double rz, Double altitude) {
            this.t = t;
            this.ax = ax;
            this.ay = ay;
            this.az = az;
            this.g = g;
            this.giro = giro;
            this.rx = rx;
            this.ry = ry;
            this.rz = rz;
            this.altitude = altitude;
        }
    }

    private final BufferCircular<Amostra> buffer = new BufferCircular<>(CAPACIDADE_BUFFER);
    private PerfilDeteccao perfil;
    private boolean bolsoCoberto;
    private long silencioAte = Long.MIN_VALUE;
    private long tUltimaAmostra = -1;
    private long inicioQuedaLivre = -1;
    private long fimQuedaLivre = Long.MIN_VALUE / 2;

    private Double altitudeAtual;
    private Double velocidadeKmh;
    private long velocidadeEm;

    // Estado da análise em andamento
    private boolean emAnalise;
    private long tImpacto;
    private double picoG;
    private double rotacaoRad;
    private Double altitudeAntes;
    private Double velocidadeAntes;
    private boolean quedaLivre;
    /** Caiu/foi jogado de verdade: >= 200 ms em queda livre logo antes do impacto. */
    private boolean aparelhoCaiu;
    private boolean girou;
    private double[] vetorAntes;
    private Motivo ultimoResultado = Motivo.NENHUM;

    public DetectorQueda(PerfilDeteccao perfil) {
        this.perfil = perfil;
    }

    public void atualizarPerfil(PerfilDeteccao novo) {
        this.perfil = novo;
    }

    public void atualizarProximidade(boolean coberto) {
        this.bolsoCoberto = coberto;
    }

    public void atualizarAltitude(double altitudeM) {
        this.altitudeAtual = altitudeM;
    }

    public void atualizarVelocidade(long t, double kmh) {
        this.velocidadeKmh = kmh;
        this.velocidadeEm = t;
    }

    /** Pausa curta após alerta ou "Estou bem". Usa o relógio dos próprios sensores. */
    public void iniciarSilencio() {
        if (tUltimaAmostra >= 0) silencioAte = tUltimaAmostra + SILENCIO_MS;
        emAnalise = false;
    }

    public boolean emAnalise() {
        return emAnalise;
    }

    public Estado estado() {
        return new Estado(emAnalise, tUltimaAmostra >= 0 && tUltimaAmostra < silencioAte, limiarImpacto(),
                picoG, Math.toDegrees(rotacaoRad), ultimoResultado);
    }

    double limiarImpacto() {
        return perfil.impactoMinG + (bolsoCoberto ? ACRESCIMO_BOLSO_G : 0);
    }

    /**
     * Processa uma amostra (aceleração em m/s², giroscópio em rad/s).
     * @return as leituras quando uma queda é confirmada; nulo caso contrário.
     */
    public Leituras processar(long t, double ax, double ay, double az, double gx, double gy, double gz) {
        double g = Math.sqrt(ax * ax + ay * ay + az * az) / GRAVIDADE;
        double giro = Math.sqrt(gx * gx + gy * gy + gz * gz);
        double dt = tUltimaAmostra < 0 ? 0 : Math.min((t - tUltimaAmostra) / 1000.0, 0.1);
        tUltimaAmostra = t;
        buffer.adicionar(new Amostra(t, ax, ay, az, g, giro, gx * dt, gy * dt, gz * dt, altitudeAtual));

        if (g < QUEDA_LIVRE_G) {
            if (inicioQuedaLivre < 0) inicioQuedaLivre = t;
        } else if (inicioQuedaLivre >= 0) {
            if (t - inicioQuedaLivre >= QUEDA_LIVRE_MIN_MS) fimQuedaLivre = t;
            inicioQuedaLivre = -1;
        }

        if (t < silencioAte) {
            emAnalise = false;
            return null;
        }
        if (!emAnalise) {
            boolean pousou = g >= IMPACTO_POS_QUEDA_LIVRE_G && t - fimQuedaLivre <= JANELA_MS;
            boolean girando = giroLiquidoGraus(t) >= GIRO_DISPARO_GRAUS;
            if (g >= limiarImpacto() || pousou || girando) iniciarAnalise(t, g, girando);
            return null;
        }
        if (t - tImpacto <= JANELA_MS) {
            picoG = Math.max(picoG, g);
            rotacaoRad += giro * dt;
            return null;
        }
        emAnalise = false;
        return avaliar(t);
    }

    private void iniciarAnalise(long t, double g, boolean girando) {
        emAnalise = true;
        tImpacto = t;
        picoG = g;
        rotacaoRad = 0;
        quedaLivre = false;
        aparelhoCaiu = t - fimQuedaLivre <= JANELA_MS;
        girou = girando;

        // Olha o buffer anterior ao impacto: queda livre, rotação, altitude e orientação.
        double somaAlt = 0;
        int nAlt = 0;
        double sx = 0, sy = 0, sz = 0;
        int nVetor = 0;
        long anterior = -1;
        for (int i = 0; i < buffer.tamanho(); i++) {
            Amostra a = buffer.obter(i);
            long antes = t - a.t;
            if (antes <= JANELA_MS) {
                if (a.g < QUEDA_LIVRE_G) quedaLivre = true;
                if (anterior >= 0) rotacaoRad += a.giro * Math.min((a.t - anterior) / 1000.0, 0.1);
            }
            if (antes >= 300 && antes <= 2000) {
                sx += a.ax;
                sy += a.ay;
                sz += a.az;
                nVetor++;
            }
            if (antes >= 1000 && a.altitude != null) {
                somaAlt += a.altitude;
                nAlt++;
            }
            anterior = a.t;
        }
        vetorAntes = nVetor > 0 ? new double[]{sx / nVetor, sy / nVetor, sz / nVetor} : null;
        // Atenção: Double.valueOf evita o unboxing de null no operador ternário (sem barômetro).
        altitudeAntes = nAlt > 0 ? Double.valueOf(somaAlt / nAlt) : altitudeAtual;
        velocidadeAntes = (velocidadeKmh != null && t - velocidadeEm <= VALIDADE_VELOCIDADE_MS
                && velocidadeEm <= t) ? velocidadeKmh : null;
    }

    private Leituras avaliar(long t) {
        Leituras r = new Leituras();
        r.picoG = arredondar(picoG);
        r.rotacaoGraus = arredondar(Math.max(Math.toDegrees(rotacaoRad), mudancaOrientacao(t)));
        r.quedaLivre = quedaLivre;
        if (altitudeAntes != null && altitudeAtual != null) {
            r.variacaoAltitudeM = arredondar(altitudeAtual - altitudeAntes);
        }
        r.velocidadeAntesKmh = velocidadeAntes;

        boolean reforco = aparelhoCaiu || (perfil.quedaLivreReforca && quedaLivre)
                || (perfil.quedaAltitudeReforcoM > 0 && r.variacaoAltitudeM != null
                && r.variacaoAltitudeM <= -perfil.quedaAltitudeReforcoM);
        r.reforcoContexto = reforco;

        // Caiu, foi jogado ou girou: o próprio movimento já é a evidência (vale em todos os perfis).
        if (!aparelhoCaiu && !girou) {
            // Impacto forte: sem GPS não bloqueia; com GPS, o perfil pode exigir velocidade antes.
            if (r.velocidadeAntesKmh != null && perfil.velocidadeMinAntesKmh > 0
                    && r.velocidadeAntesKmh < perfil.velocidadeMinAntesKmh) {
                ultimoResultado = Motivo.VELOCIDADE_ANTES_BAIXA;
                return null;
            }
            if (r.rotacaoGraus < perfil.rotacaoMinGraus * (reforco ? FATOR_REFORCO_ROTACAO : 1.0)) {
                ultimoResultado = Motivo.ROTACAO_BAIXA;
                return null;
            }
        }

        ultimoResultado = Motivo.CONFIRMADA;
        silencioAte = t + SILENCIO_MS;
        return r;
    }

    /** Rotação líquida (graus) no último segundo: girar soma, balançar para lá e para cá se cancela. */
    private double giroLiquidoGraus(long t) {
        double sx = 0, sy = 0, sz = 0;
        for (int i = buffer.tamanho() - 1; i >= 0; i--) {
            Amostra a = buffer.obter(i);
            if (t - a.t > JANELA_MS) break;
            sx += a.rx;
            sy += a.ry;
            sz += a.rz;
        }
        return Math.toDegrees(Math.sqrt(sx * sx + sy * sy + sz * sz));
    }

    /** Ângulo (graus) entre a gravidade antes do impacto e a média dos últimos 300 ms. */
    private double mudancaOrientacao(long t) {
        if (vetorAntes == null) return 0;
        double sx = 0, sy = 0, sz = 0;
        for (int i = buffer.tamanho() - 1; i >= 0; i--) {
            Amostra a = buffer.obter(i);
            if (t - a.t > 300) break;
            sx += a.ax;
            sy += a.ay;
            sz += a.az;
        }
        double n1 = Math.sqrt(vetorAntes[0] * vetorAntes[0] + vetorAntes[1] * vetorAntes[1] + vetorAntes[2] * vetorAntes[2]);
        double n2 = Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (n1 < 1e-6 || n2 < 1e-6) return 0;
        double cos = (vetorAntes[0] * sx + vetorAntes[1] * sy + vetorAntes[2] * sz) / (n1 * n2);
        return Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, cos))));
    }

    private static double arredondar(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
