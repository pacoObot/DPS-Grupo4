package quiz.domain.entity;

import org.junit.jupiter.api.Test;
import quiz.domain.enums.GameStatus;
import quiz.domain.valueobject.QuestionId;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Máquina de estados da partida (domínio puro). */
class GameTest {

    private Game game() {
        return Game.create("Prova", 15, List.of(new QuestionId(1), new QuestionId(2), new QuestionId(3)));
    }

    @Test
    void criadaFicaAberta() {
        Game g = game();
        assertEquals(3, g.getTotalRounds());
        assertEquals(0, g.getCurrentRound());
        // Game.create coloca a partida pronta a receber inscrições
        assertEquals(GameStatus.OPEN, g.getStatus());
    }

    @Test
    void naoPermiteSemsPerguntas() {
        assertThrows(IllegalArgumentException.class,
                () -> Game.create("X", 15, List.of()));
    }

    @Test
    void startColocaARodarNaPrimeiraRonda() {
        Game g = game();
        g.start();
        assertEquals(GameStatus.RUNNING, g.getStatus());
        assertEquals(1, g.getCurrentRound());
        assertNotNull(g.getStartedAt());
    }

    @Test
    void naoPodeIniciarDuasVezes() {
        Game g = game();
        g.start();
        assertThrows(IllegalStateException.class, g::start);
    }

    @Test
    void pauseResumeSoEmRodagem() {
        Game g = game();
        assertThrows(IllegalStateException.class, g::pause);

        g.start();
        g.pause();
        assertEquals(GameStatus.PAUSED, g.getStatus());

        g.resume();
        assertEquals(GameStatus.RUNNING, g.getStatus());
    }

    @Test
    void nextRoundAvancaAteAoFim() {
        Game g = game();
        g.start();
        assertTrue(g.nextRound());
        assertEquals(2, g.getCurrentRound());
        assertTrue(g.nextRound());
        assertEquals(3, g.getCurrentRound());
        assertFalse(g.nextRound());
        assertEquals(3, g.getCurrentRound());
    }

    @Test
    void deadlineEhInicioMaisTempo() {
        Game g = game();
        g.start();
        assertNull(g.currentDeadline()); // ainda sem ronda iniciada

        g.setRoundStartedAt(java.time.Instant.parse("2026-01-01T00:00:00Z"));
        assertEquals(java.time.Instant.parse("2026-01-01T00:00:15Z"), g.currentDeadline());
    }

    @Test
    void finishMarcaTerminada() {
        Game g = game();
        g.start();
        g.finish();
        assertEquals(GameStatus.FINISHED, g.getStatus());
        assertNotNull(g.getFinishedAt());
    }

    @Test
    void tempoForaDoIntervaloEhRejeitado() {
        Game g = Game.create("X", 0, List.of(new QuestionId(1)));
        assertThrows(IllegalArgumentException.class, g::validate);
    }
}