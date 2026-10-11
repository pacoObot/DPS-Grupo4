package quiz.application.usecase.game;

import quiz.application.exception.BusinessException;
import quiz.application.port.output.GameRepository;
import quiz.application.port.output.TeamRepository;
import quiz.application.port.output.UnitOfWork;
import quiz.application.service.GameService;
import quiz.domain.entity.Game;
import quiz.domain.entity.Team;
import quiz.domain.enums.GameStatus;
import quiz.domain.enums.TeamStatus;
import quiz.shared.dto.QuestionDTO;

import java.time.Instant;

/**
 * Start Round — marca o início da ronda ATUAL:
 *   - round_started_at = agora  (o servidor passa a ser a autoridade do tempo)
 *   - equipes online -> PLAYING
 *   - devolve a pergunta da ronda (DTO seguro, sem resposta)
 */
public class StartRoundUseCase {

    private final GameRepository games;
    private final TeamRepository teams;
    private final GameService gameService;
    private final UnitOfWork uow;

    public StartRoundUseCase(GameRepository games, TeamRepository teams,
                             GameService gameService, UnitOfWork uow) {
        this.games = games;
        this.teams = teams;
        this.gameService = gameService;
        this.uow = uow;
    }

    public QuestionDTO call(long gameId) {
        return uow.call(() -> {
            Game g = games.lockById(gameId)
                    .orElseThrow(() -> new BusinessException.NotFound("Partida não encontrada"));

            if (g.getStatus() != GameStatus.RUNNING)
                throw new BusinessException.Conflict("A partida não está a correr (estado: " + g.getStatus() + ")");
            if (g.getCurrentRound() <= 0 || g.getCurrentRound() > g.getTotalRounds())
                throw new BusinessException.Conflict("Ronda inválida: " + g.getCurrentRound());

            g.setRoundStartedAt(Instant.now());
            games.update(g);

            // todas as equipas online entram em PLAYING nesta ronda
            for (Team t : teams.findByGame(gameId)) {
                if (t.getStatus() != TeamStatus.OFFLINE) {
                    t.markPlaying();
                    teams.update(t);
                }
            }
            return gameService.currentQuestionFor(g);
        });
    }
}
