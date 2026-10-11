package quiz.application.usecase.player;

import quiz.application.exception.BusinessException;
import quiz.application.port.output.TeamRepository;
import quiz.application.port.output.UnitOfWork;
import quiz.domain.entity.Team;

/**
 * Leave Game / desconexão — marca a equipa como OFFLINE.
 * O jogo CONTINUA com as restantes equipes (tolerância a desconexões, spec §17).
 */
public class LeaveGameUseCase {

    private final TeamRepository teams;
    private final UnitOfWork uow;

    public LeaveGameUseCase(TeamRepository teams, UnitOfWork uow) {
        this.teams = teams;
        this.uow = uow;
    }

    public void call(long teamId) {
        uow.call(() -> {
            Team team = teams.lockById(teamId)
                    .orElseThrow(() -> new BusinessException.NotFound("Equipa não encontrada"));
            team.markOffline();
            teams.update(team);
            return null;
        });
    }
}
