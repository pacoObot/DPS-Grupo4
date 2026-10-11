package quiz.application.usecase.player;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import quiz.application.exception.BusinessException;
import quiz.application.port.output.GameRepository;
import quiz.application.port.output.TeamRepository;
import quiz.application.port.output.UnitOfWork;
import quiz.domain.entity.Game;
import quiz.domain.entity.Score;
import quiz.domain.entity.Team;
import quiz.domain.enums.GameStatus;
import quiz.domain.enums.TeamStatus;
import quiz.domain.valueobject.GameId;
import quiz.domain.valueobject.QuestionId;
import quiz.domain.valueobject.TeamId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Testes do caso de uso de entrada na partida, com portas simuladas (Mockito).
 * Não toca em PostgreSQL nem em rede.
 */
class JoinGameUseCaseTest {

    private GameRepository games;
    private TeamRepository teams;
    private UnitOfWork uow;
    private JoinGameUseCase useCase;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        games = mock(GameRepository.class);
        teams = mock(TeamRepository.class);
        uow = mock(UnitOfWork.class);
        useCase = new JoinGameUseCase(games, teams, uow);

        // a unidade de trabalho apenas executa o trabalho fornecido
        when(uow.call(any())).thenAnswer(inv -> ((Supplier<?>) inv.getArgument(0)).get());
    }

    private Game openGame(long id) {
        return new Game(new GameId(id), "Prova", "ABC123", GameStatus.OPEN,
                1, 0, 15, null, Instant.now(), null, null, List.of(new QuestionId(1)));
    }

    @Test
    void codigoVazioEhValidacao() {
        assertThrows(BusinessException.Validation.class, () -> useCase.call("  ", "Alpha"));
        verifyNoInteractions(games);
    }

    @Test
    void codigoInexistenteEhNaoEncontrado() {
        when(games.findByCode("ZZZZZZ")).thenReturn(Optional.empty());
        assertThrows(BusinessException.NotFound.class, () -> useCase.call("zzzzzz", "Alpha"));
    }

    @Test
    void inscricoesFechadasSaoConflito() {
        Game g = openGame(1);
        g.setStatus(GameStatus.RUNNING);
        when(games.findByCode("ABC123")).thenReturn(Optional.of(g));

        assertThrows(BusinessException.Conflict.class, () -> useCase.call("abc123", "Alpha"));
    }

    @Test
    void nomeDuplicadoEhConflito() {
        Game g = openGame(1);
        when(games.findByCode("ABC123")).thenReturn(Optional.of(g));
        when(teams.findByName(1L, "Alpha"))
                .thenReturn(Optional.of(Team.create(new GameId(1), "Alpha")));

        assertThrows(BusinessException.Conflict.class, () -> useCase.call("ABC123", "Alpha"));
        verify(teams, never()).save(any());
    }

    @Test
    void entradaComSucessoDevolveIdsEDadosDaPartida() {
        Game g = openGame(1);
        when(games.findByCode("ABC123")).thenReturn(Optional.of(g)); // aceita código normalizado
        when(teams.findByName(1L, "Alpha")).thenReturn(Optional.empty());

        Team saved = new Team(new TeamId(7), new GameId(1), "Alpha",
                new Score(), TeamStatus.WAITING, Instant.now(), null);
        when(teams.save(any(Team.class))).thenReturn(saved);
        when(teams.findByGame(1L)).thenReturn(List.of(saved));

        JoinGameUseCase.Result r = useCase.call("abc123", "Alpha");

        assertEquals(7, r.teamId());
        assertEquals("Alpha", r.teamName());
        assertEquals("Prova", r.game().name());
        assertEquals(1, r.game().teamsTotal());
        verify(teams).save(any(Team.class));
        verify(teams).findByName(eq(1L), eq("Alpha"));
    }

    @Test
    void nomeInvalidoEhRejeitadoPelaValidacaoDoDominio() {
        Game g = openGame(1);
        when(games.findByCode("ABC123")).thenReturn(Optional.of(g));
        when(teams.findByName(1L, "Bad|Name")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> useCase.call("ABC123", "Bad|Name"));
    }
}