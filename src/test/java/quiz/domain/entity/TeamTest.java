package quiz.domain.entity;

import org.junit.jupiter.api.Test;
import quiz.domain.enums.TeamStatus;
import quiz.domain.valueobject.TeamId;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TeamTest {

    private Team team() {
        return Team.create(new quiz.domain.valueobject.GameId(1), "Alpha");
    }

    @Test
    void criadaFicaEmEspera() {
        Team t = team();
        assertEquals(TeamStatus.WAITING, t.getStatus());
        assertTrue(t.isOnline());
        assertEquals(0, t.getScore().getPoints());
    }

    @Test
    void respostaCertaSomaPontosEMarcaRespondida() {
        Team t = team();
        t.markPlaying();
        t.registerCorrectAnswer(18);
        assertEquals(18, t.getScore().getPoints());
        assertEquals(1, t.getScore().getCorrectCount());
        assertEquals(TeamStatus.ANSWERED, t.getStatus());
    }

    @Test
    void respostaErradaContaErro() {
        Team t = team();
        t.registerWrongAnswer();
        assertEquals(0, t.getScore().getPoints());
        assertEquals(1, t.getScore().getWrongCount());
        assertEquals(TeamStatus.ANSWERED, t.getStatus());
    }

    @Test
    void timeoutContaComoErrada() {
        Team t = team();
        t.markPlaying();
        t.markTimeout();
        assertEquals(TeamStatus.TIMEOUT, t.getStatus());
        assertEquals(1, t.getScore().getWrongCount());
    }

    @Test
    void offlineNaoVoltaAJogar() {
        Team t = team();
        t.markOffline();
        assertFalse(t.isOnline());
        t.markPlaying(); // não deve reverter OFFLINE
        assertEquals(TeamStatus.OFFLINE, t.getStatus());
    }

    @Test
    void nomeInvalidoEhRejeitado() {
        Team t = new Team(null, new quiz.domain.valueobject.GameId(1), "Bad|Name",
                new Score(), TeamStatus.WAITING, Instant.now(), null);
        assertThrows(IllegalArgumentException.class, t::validate);
    }

    @Test
    void nomeVazioEhRejeitado() {
        Team t = new Team(null, new quiz.domain.valueobject.GameId(1), "",
                new Score(), TeamStatus.WAITING, Instant.now(), null);
        assertThrows(IllegalArgumentException.class, t::validate);
    }

    @Test
    void nomeValidoAceitaAcentosENumeros() {
        Team t = new Team(new TeamId(1), new quiz.domain.valueobject.GameId(1), "Equipa Águia 42",
                new Score(), TeamStatus.WAITING, Instant.now(), null);
        assertDoesNotThrow(t::validate);
    }
}