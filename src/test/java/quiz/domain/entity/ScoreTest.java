package quiz.domain.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Testes da fórmula de pontuação (domínio puro, sem Spring, sem DB).
 *   pontos = base + round(10 * tempo_restante / tempo_total)
 */
class ScoreTest {

    @Test
    void semTempoSuficienteDevolveBase() {
        assertEquals(10, Score.calculate(10, 0, 0));
    }

    @Test
    void respostaImediataRecebeBonusMaximo() {
        // restante == total -> +10 de bónus
        assertEquals(20, Score.calculate(10, 10_000, 10_000));
    }

    @Test
    void metadeDoTempoRecebeMetadeDoBonus() {
        assertEquals(15, Score.calculate(10, 5_000, 10_000));
    }

    @Test
    void tempoEsgotadoDevolveBase() {
        assertEquals(10, Score.calculate(10, 0, 10_000));
    }

    @Test
    void ratioAcimaDeUmEhLimitado() {
        assertEquals(20, Score.calculate(10, 25_000, 10_000));
    }

    @Test
    void baseInvalidaAssumeDez() {
        assertEquals(20, Score.calculate(0, 10_000, 10_000));
    }

    @Test
    void acumulaCertasEErradas() {
        Score s = new Score();
        s.addCorrect(15);
        s.addCorrect(20);
        s.addWrong();

        assertEquals(35, s.getPoints());
        assertEquals(2, s.getCorrectCount());
        assertEquals(1, s.getWrongCount());
    }
}