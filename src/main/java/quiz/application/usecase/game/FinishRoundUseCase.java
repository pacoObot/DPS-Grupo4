package quiz.application.usecase.game;

import quiz.application.exception.BusinessException;
import quiz.application.port.output.AnswerRepository;
import quiz.application.port.output.GameRepository;
import quiz.application.port.output.TeamRepository;
import quiz.application.port.output.UnitOfWork;
import quiz.domain.entity.Game;
import quiz.domain.entity.Team;
import quiz.domain.enums.GameStatus;
import quiz.domain.enums.TeamStatus;
import quiz.shared.dto.GameInfoDTO;

/**
 * Finish Round — conclui a ronda atual:
 *   - equipas online sem resposta -> TIMEOUT (wrong++ e persistido)
 *   - avança currentRound se ainda houver rondas
 *
 * Chamado pelo GameScheduler quando o RoundCoordinator sinaliza fim
 * (todas responderam OU tempo expirou).
 */
public class FinishRoundUseCase {

    private final GameRepository games;
    private final TeamRepository teams;
    private final AnswerRepository answers;
    private final UnitOfWork uow;

    public FinishRoundUseCase(GameRepository games, TeamRepository teams,
                              AnswerRepository answers, UnitOfWork uow) {
        this.games = games;
        this.teams = teams;
        this.answers = answers;
        this.uow = uow;
    }

    public GameInfoDTO call(long gameId) {
        return uow.call(() -> {
            Game g = games.lockById(gameId)
                    .orElseThrow(() -> new BusinessException.NotFound("Partida não encontrada"));

            if (g.getStatus() == GameStatus.FINISHED)
                return quiz.application.usecase.admin.ManageGameUseCase.toInfo(g);

            long questionId = games.findQuestionIdForRound(gameId, g.getCurrentRound())
                    .map(q -> q.value())
                    .orElse(-1L);

            for (Team t : teams.findByGame(gameId)) {
                if (t.getStatus() == TeamStatus.OFFLINE) continue;      // já saiu
                if (t.getStatus() == TeamStatus.PLAYING || t.getStatus() == TeamStatus.WAITING) {
                    if (questionId > 0 && !answers.hasAnsweredRound(t.getId().value(), questionId)) {
                        t.markTimeout();                                 // wrong++, status TIMEOUT
                        teams.update(t);
                    }
                }
            }

            if (g.getCurrentRound() >= g.getTotalRounds()) {
                // última ronda concluída — o FinishGameUseCase encerra a partida
                return quiz.application.usecase.admin.ManageGameUseCase.toInfo(g);
            }
            g.nextRound();
            games.update(g);
            return quiz.application.usecase.admin.ManageGameUseCase.toInfo(g);
        });
    }
}
