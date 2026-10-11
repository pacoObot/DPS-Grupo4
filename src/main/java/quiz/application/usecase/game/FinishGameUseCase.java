package quiz.application.usecase.game;

import quiz.application.exception.BusinessException;
import quiz.application.port.output.GameRepository;
import quiz.application.port.output.TeamRepository;
import quiz.application.port.output.UnitOfWork;
import quiz.application.service.ScoreService;
import quiz.domain.entity.Game;
import quiz.domain.entity.Team;
import quiz.shared.dto.GameInfoDTO;
import quiz.shared.dto.ScoreboardEntry;

import java.util.List;

/**
 * Finish Game — encerra a partida, congela o estado das equipas
 * e devolve a classificação final.
 */
public class FinishGameUseCase {

    private final GameRepository games;
    private final TeamRepository teams;
    private final UnitOfWork uow;
    private final ScoreService scoreService;

    public FinishGameUseCase(GameRepository games, TeamRepository teams,
                             UnitOfWork uow, ScoreService scoreService) {
        this.games = games;
        this.teams = teams;
        this.uow = uow;
        this.scoreService = scoreService;
    }

    public record Result(GameInfoDTO game, List<ScoreboardEntry> finalScoreboard) {}

    public Result call(long gameId) {
        GameInfoDTO info = uow.call(() -> {
            Game g = games.lockById(gameId)
                    .orElseThrow(() -> new BusinessException.NotFound("Partida " + gameId + " não encontrada"));

            g.finish();
            games.update(g);

            for (Team t : teams.findByGame(gameId)) {
                t.markFinished();
                teams.update(t);
            }
            return quiz.application.usecase.admin.ManageGameUseCase.toInfo(g);
        });

        // ranking depois do commit (leitura consistente)
        return new Result(info, scoreService.ranking(gameId));
    }
}
