package quiz.application.usecase.player;

import quiz.application.exception.BusinessException;
import quiz.application.port.output.GameRepository;
import quiz.application.port.output.TeamRepository;
import quiz.application.service.GameService;
import quiz.domain.entity.Game;
import quiz.domain.entity.Team;
import quiz.domain.enums.GameStatus;
import quiz.shared.dto.QuestionDTO;

/** Devolve a pergunta atual ao cliente (sem a resposta correta). */
public class GetQuestionUseCase {

    private final TeamRepository teams;
    private final GameService gameService;

    public GetQuestionUseCase(TeamRepository teams, GameService gameService) {
        this.teams = teams;
        this.gameService = gameService;
    }

    public QuestionDTO call(long teamId) {
        Team team = teams.findById(teamId)
                .orElseThrow(() -> new BusinessException.NotFound("Equipa não encontrada"));
        Game game = gameService.requireGame(team.getGameId().value());

        if (game.getStatus() != GameStatus.RUNNING && game.getStatus() != GameStatus.PAUSED)
            throw new BusinessException.Conflict("Ainda não há pergunta ativa (estado: " + game.getStatus() + ")");
        if (game.getCurrentRound() <= 0)
            throw new BusinessException.Conflict("A ronda ainda não começou");

        return gameService.currentQuestionFor(game);
    }
}
