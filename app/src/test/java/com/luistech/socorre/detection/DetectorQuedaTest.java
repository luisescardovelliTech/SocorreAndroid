package com.luistech.socorre.detection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.luistech.socorre.domain.modelo.Leituras;
import com.luistech.socorre.domain.modelo.PerfilAtividade;
import com.luistech.socorre.domain.modelo.Sensibilidade;

import org.junit.Test;

/** Testes do motor de detecção com sinais sintéticos a 50 Hz. */
public class DetectorQuedaTest {

    private static final double G = DetectorQueda.GRAVIDADE;
    private static final long PASSO = 20; // 50 Hz

    /** Gera amostras e guarda a primeira detecção (ou nulo) e quando ela aconteceu. */
    private static final class Sim {
        final DetectorQueda d;
        long t = 1_000_000;
        Leituras deteccao;
        long tDeteccao = -1;
        int deteccoes;

        Sim(PerfilAtividade p) {
            d = new DetectorQueda(PerfilDeteccao.padrao(p));
        }

        Sim(PerfilDeteccao p) {
            d = new DetectorQueda(p);
        }

        private void registrar(Leituras r) {
            if (r == null) return;
            deteccoes++;
            if (deteccao == null) {
                deteccao = r;
                tDeteccao = t;
            }
        }

        /** Mantém um vetor de aceleração (em g) e giro (rad/s, eixo x) constantes por ms milissegundos. */
        Sim manter(double xg, double yg, double zg, double giro, long ms) {
            for (long fim = t + ms; t < fim; t += PASSO) {
                registrar(d.processar(t, xg * G, yg * G, zg * G, giro, 0, 0));
            }
            return this;
        }

        /** Em pé, parado. */
        Sim emPe(long ms) {
            return manter(0, 1, 0, 0, ms);
        }

        /** Deitado, parado (orientação 90 graus diferente de "em pé"). */
        Sim deitado(long ms) {
            return manter(0, 0, 1, 0, ms);
        }

        /** Impacto: pico de g com giro durante a janela. */
        Sim impacto(double picoG, double giro, long ms) {
            return manter(0, picoG, 0, giro, ms);
        }

        Sim caminhar(long ms) {
            for (long fim = t + ms; t < fim; t += PASSO) {
                double fase = (t % 1000) / 1000.0 * 2 * Math.PI;
                double gy = 1 + 0.6 * Math.sin(fase);
                registrar(d.processar(t, 0.1 * G, gy * G, 0.2 * G, 1.2, 0.3, 0));
            }
            return this;
        }

        /** Queda típica: em pé, queda livre, impacto com rotação e deitado. */
        Sim quedaCompleta(double picoG, long depoisMs) {
            emPe(2000);
            manter(0, 0.2, 0, 2.0, 300);   // queda livre
            impacto(picoG, 3.0, 200);
            return deitado(depoisMs);
        }
    }

    @Test
    public void caminhadaNormalNaoDisparaAlerta() {
        Sim s = new Sim(PerfilAtividade.CAMINHADA_CASA).caminhar(120_000);
        assertNull(s.deteccao);
    }

    @Test
    public void quedaCompletaEhDetectadaComLeituras() {
        Sim s = new Sim(PerfilAtividade.CAMINHADA_CASA).quedaCompleta(3.5, 3_000);
        Leituras r = s.deteccao;
        assertNotNull(r);
        assertEquals(3.5, r.picoG, 0.05);
        assertTrue(r.rotacaoGraus >= 45);
        assertTrue(r.quedaLivre);
        assertNull(r.variacaoAltitudeM); // sem barômetro
    }

    @Test
    public void disparaNaHoraSemEsperarFicarParado() {
        // A pessoa (ou o celular) continua se mexendo logo depois: o alerta sai mesmo assim, em ~1 s.
        Sim s = new Sim(PerfilAtividade.CAMINHADA_CASA);
        s.emPe(2000).manter(0, 0.2, 0, 2.0, 300);
        long impacto = s.t;
        s.impacto(3.5, 3.0, 200).caminhar(3_000);
        assertNotNull(s.deteccao);
        assertTrue("disparou em " + (s.tDeteccao - impacto) + " ms", s.tDeteccao - impacto <= 1_100);
    }

    @Test
    public void quedaNaCamaComPousoMacioEhDetectada() {
        // Queda livre seguida de pouso macio (1,6 g), abaixo do limiar normal, caindo com a tela para baixo.
        Sim s = new Sim(PerfilAtividade.CAMINHADA_CASA);
        s.d.atualizarProximidade(true);
        s.emPe(2000).manter(0, 0.1, 0, 2.0, 350).impacto(1.6, 3.0, 200).deitado(2_000);
        assertNotNull(s.deteccao);
    }

    @Test
    public void pousoMacioSemQuedaLivreNaoDispara() {
        Sim s = new Sim(PerfilAtividade.CAMINHADA_CASA);
        s.emPe(2000).impacto(1.6, 3.0, 200).deitado(3_000);
        assertNull(s.deteccao);
    }

    @Test
    public void aparelhoJogadoDisparaEmTodosOsPerfisMesmoParadoNoGps() {
        for (PerfilAtividade p : PerfilAtividade.values()) {
            Sim s = new Sim(p);
            s.d.atualizarVelocidade(s.t + 1000, 0); // em casa, GPS parado
            s.emPe(2000).manter(0, 0.1, 0, 4.0, 400).impacto(1.6, 3.0, 200).deitado(3_000);
            assertNotNull(p.name(), s.deteccao);
        }
    }

    @Test
    public void giroCompletoDisparaSemImpactoEmTodosOsPerfis() {
        for (PerfilAtividade p : PerfilAtividade.values()) {
            Sim s = new Sim(p);
            s.emPe(2000).manter(0, 1, 0, 7.0, 1_000).deitado(1_500); // ~400 graus em 1 s
            assertNotNull(p.name(), s.deteccao);
            assertTrue(s.deteccao.rotacaoGraus >= 360);
        }
    }

    @Test
    public void virarParaPaisagemNaoDispara() {
        Sim s = new Sim(PerfilAtividade.CAMINHADA_CASA);
        s.emPe(2000).manter(0, 1, 0, Math.PI, 500).manter(1, 0, 0, 0, 3_000); // 90 graus
        assertNull(s.deteccao);
    }

    @Test
    public void balancarParaLaEParaCaNaoContaComoGiro() {
        Sim s = new Sim(PerfilAtividade.CAMINHADA_CASA);
        for (int i = 0; i < 30; i++) s.manter(0, 1, 0, i % 2 == 0 ? 8 : -8, 100);
        assertNull(s.deteccao);
    }

    @Test
    public void impactoSemRotacaoNaoDispara() {
        // Aparelho bate "de pé" e fica na mesma orientação, sem giro.
        Sim s = new Sim(PerfilAtividade.CAMINHADA_CASA);
        s.emPe(2000).impacto(4.0, 0, 200).emPe(3_000);
        assertNull(s.deteccao);
    }

    @Test
    public void bolsoCobertoAumentaLimiarEmMeioG() {
        Sim livre = new Sim(PerfilAtividade.CAMINHADA_CASA);
        livre.emPe(2000).impacto(2.7, 3.0, 200).deitado(3_000);
        assertNotNull(livre.deteccao);

        Sim bolso = new Sim(PerfilAtividade.CAMINHADA_CASA);
        bolso.d.atualizarProximidade(true);
        bolso.emPe(2000).impacto(2.7, 3.0, 200).deitado(3_000); // sem queda livre: vale o limiar
        assertNull(bolso.deteccao);
        assertEquals(3.0, bolso.d.limiarImpacto(), 1e-9);
    }

    @Test
    public void detectaVariasQuedasSeguidas() {
        Sim s = new Sim(PerfilAtividade.CAMINHADA_CASA);
        for (int i = 0; i < 5; i++) s.quedaCompleta(3.5, 3_000);
        assertEquals(5, s.deteccoes);
    }

    @Test
    public void mesmoTomboNaoContaDuasVezes() {
        Sim s = new Sim(PerfilAtividade.CAMINHADA_CASA);
        s.emPe(2000).manter(0, 0.2, 0, 2.0, 300).impacto(3.5, 3.0, 200).deitado(1_100);
        assertEquals(1, s.deteccoes);
        s.manter(0, 0.2, 0, 2.0, 300).impacto(3.5, 3.0, 200).deitado(3_000); // quicou logo em seguida
        assertEquals("dentro da pausa de 3 s não conta de novo", 1, s.deteccoes);
        s.quedaCompleta(3.5, 3_000);
        assertEquals("depois da pausa volta a detectar", 2, s.deteccoes);
    }

    @Test
    public void estouBemPausaSoTresSegundos() {
        Sim s = new Sim(PerfilAtividade.CAMINHADA_CASA).emPe(1_000);
        s.d.iniciarSilencio(); // "Estou bem"
        s.manter(0, 0.2, 0, 2.0, 300).impacto(3.5, 3.0, 200).deitado(3_000);
        assertNull("logo após o Estou bem", s.deteccao);
        s.quedaCompleta(3.5, 3_000);
        assertNotNull("nova queda depois da pausa", s.deteccao);
    }

    @Test
    public void motoExigeVelocidadeAntesNoImpactoQuandoHaGps() {
        Sim lento = new Sim(PerfilAtividade.MOTO);
        lento.d.atualizarVelocidade(lento.t + 1000, 5);
        lento.emPe(2000).impacto(5.0, 3.0, 200).deitado(3_000); // pancada sem queda livre
        assertNull(lento.deteccao);
        assertEquals(DetectorQueda.Motivo.VELOCIDADE_ANTES_BAIXA, lento.d.estado().ultimoResultado);

        Sim rapido = new Sim(PerfilAtividade.MOTO);
        rapido.d.atualizarVelocidade(rapido.t + 1000, 45);
        rapido.emPe(2000).impacto(5.0, 3.0, 200).deitado(3_000);
        assertNotNull(rapido.deteccao);
        assertEquals(45, rapido.deteccao.velocidadeAntesKmh, 1e-9);
    }

    @Test
    public void motoSemGpsNaoBloqueiaAlerta() {
        Sim s = new Sim(PerfilAtividade.MOTO).quedaCompleta(5.0, 3_000);
        assertNotNull(s.deteccao);
        assertNull(s.deteccao.velocidadeAntesKmh);
    }

    @Test
    public void motoIgnoraImpactoAbaixoDe4gSemQuedaLivre() {
        Sim s = new Sim(PerfilAtividade.MOTO);
        s.emPe(2000).impacto(3.6, 3.0, 200).deitado(3_000);
        assertNull(s.deteccao);
    }

    @Test
    public void quedaDeAltitudeReforcaRotacaoBaixaNoCampo() {
        // Rotação ~50 graus: abaixo de 60, mas acima de 60 * 0,75 = 45.
        double giro = Math.toRadians(50) / 0.5;

        Sim semAltitude = new Sim(PerfilAtividade.TRABALHO_CAMPO);
        semAltitude.emPe(2000).manter(0, 3.5, 0, giro, 500).emPe(3_000);
        assertNull(semAltitude.deteccao);

        Sim comAltitude = new Sim(PerfilAtividade.TRABALHO_CAMPO);
        comAltitude.d.atualizarAltitude(100.0);
        comAltitude.emPe(2000).manter(0, 3.5, 0, giro, 500);
        comAltitude.d.atualizarAltitude(98.0);
        comAltitude.emPe(3_000);
        assertNotNull(comAltitude.deteccao);
        assertTrue(comAltitude.deteccao.reforcoContexto);
        assertEquals(-2.0, comAltitude.deteccao.variacaoAltitudeM, 0.05);
    }

    @Test
    public void sensibilidadeAltaReduzLimiar() {
        PerfilDeteccao alta = PerfilDeteccao.padrao(PerfilAtividade.MOTO).comSensibilidade(Sensibilidade.ALTA);
        assertEquals(3.4, alta.impactoMinG, 1e-9);
        Sim s = new Sim(alta);
        s.emPe(2000).impacto(3.6, 3.0, 200).deitado(3_000);
        assertNotNull(s.deteccao);
    }

    @Test
    public void rotacaoPorOrientacaoFuncionaSemGiroscopio() {
        Sim s = new Sim(PerfilAtividade.CAMINHADA_CASA);
        s.emPe(2000).impacto(3.5, 0, 200).deitado(2_000);
        assertNotNull(s.deteccao);
        assertEquals(90, s.deteccao.rotacaoGraus, 1.0);
        assertFalse(s.deteccao.quedaLivre);
    }

    @Test
    public void estadoInformaMotivoDeCadaResultado() {
        Sim semRotacao = new Sim(PerfilAtividade.CAMINHADA_CASA);
        semRotacao.emPe(2000).impacto(4.0, 0, 200).emPe(3_000);
        assertEquals(DetectorQueda.Motivo.ROTACAO_BAIXA, semRotacao.d.estado().ultimoResultado);

        Sim queda = new Sim(PerfilAtividade.CAMINHADA_CASA).quedaCompleta(3.5, 3_000);
        assertEquals(DetectorQueda.Motivo.CONFIRMADA, queda.d.estado().ultimoResultado);
        assertTrue(queda.d.estado().emSilencio);
    }

    @Test
    public void estadoMostraAAnaliseEmAndamento() {
        Sim s = new Sim(PerfilAtividade.CAMINHADA_CASA);
        s.emPe(2000).impacto(4.0, 3.0, 200).deitado(300);
        DetectorQueda.Estado e = s.d.estado();
        assertTrue(e.analisando);
        assertEquals(4.0, e.picoG, 0.05);
        assertEquals(2.5, e.limiarImpactoG, 1e-9);
    }
}
