package quiz.application.usecase.game;

import quiz.application.exception.BusinessException;
import quiz.application.port.output.GameRepository;
import quiz.application.port.output.TeamRepository;
import quiz.application.port.output.UnitOfWork;
import quiz.domain.entity.Game;
import quiz.domain.enums.GameStatus;
import quiz.shared.dto.GameInfoDTO;

/**
 * Start Game — transição OPEN -> RUNNING (revalidada com SELECT ... FOR UPDATE).
 * Exige pelo menos 1 equipa inscrita.
 */
public class StartGameUseCase {

    private final GameRepository games;
    private final TeamRepository teams;
    private final UnitOfWork uow;

    public StartGameUseCase(GameRepository games, TeamRepository teams, UnitOfWork uow) {
        this.games = games;
        this.teams = teams;
        this.uow = uow;
    }

    public GameInfoDTO call(long gameId) {
        return uow.call(() -> {
            Game g = games.lockById(gameId)
                    .orElseThrow(() -> new BusinessException.NotFound("Partida não encontrada"));

            if (g.getStatus() == GameStatus.RUNNING) {
                return quiz.application.usecase.admin.ManageGameUseCase.toInfo(g); // idempotente
            }
            if (g.getStatus() != GameStatus.OPEN)
                throw new BusinessException.Conflict(
                        "Só se pode iniciar uma partida com inscrições abertas (estado: " + g.getStatus() + ")");

            int totalTeams = teams.findByGame(gameId).size();
            if (totalTeams == 0)
                throw new BusinessException.Conflict("É preciso pelo menos 1 equipa inscrita");

            g.start(); // OPEN -> RUNNING, currentRound = 1, started_at = agora
            games.update(g);
            return quiz.application.usecase.admin.ManageGameUseCase.toInfo(g);
        });
    }
}
