package quiz.application.usecase.player;

import quiz.application.exception.BusinessException;
import quiz.application.port.output.GameRepository;
import quiz.application.port.output.TeamRepository;
import quiz.application.port.output.UnitOfWork;
import quiz.domain.entity.Game;
import quiz.domain.entity.Team;
import quiz.domain.enums.GameStatus;
import quiz.domain.enums.TeamStatus;
import quiz.shared.dto.GameInfoDTO;

import java.util.Optional;

/**
 * Join Game — inscreve uma equipa usando o código da partida.
 * UNIQUE(game_id, name) no PostgreSQL + pré-verificação => sem duplicados.
 *
 * Se a partida já estiver a decorrer (RUNNING/PAUSED), permite RECONECTAR
 * uma equipa existente que estava OFFLINE (ex.: o jogador recarregou a
 * página ou perdeu rede). Uma equipa já online noutro dispositivo é rejeitada.
 */
public class JoinGameUseCase {

    private final GameRepository games;
    private final TeamRepository teams;
    private final UnitOfWork uow;

    public JoinGameUseCase(GameRepository games, TeamRepository teams, UnitOfWork uow) {
        this.games = games;
        this.teams = teams;
        this.uow = uow;
    }

    public record Result(long teamId, String teamName, GameInfoDTO game) {}

    public Result call(String joinCode, String teamName) {
        if (joinCode == null || joinCode.isBlank())
            throw new BusinessException.Validation("Código da partida obrigatório");

        return uow.call(() -> {
            Game game = games.findByCode(joinCode.strip().toUpperCase())
                    .orElseThrow(() -> new BusinessException.NotFound("Código de partida inválido"));

            long gameId = game.getId().value();
            Optional<Team> existing = teams.findByName(gameId, teamName);

            if (game.getStatus() != GameStatus.OPEN) {
                // -------- reconexão a uma partida já a decorrer --------
                if (game.getStatus() == GameStatus.RUNNING || game.getStatus() == GameStatus.PAUSED) {
                    if (existing.isPresent()) {
                        Team t = existing.get();
                        if (t.getStatus() == TeamStatus.OFFLINE) {
                            t.setStatus(TeamStatus.WAITING);
                            teams.update(t);
                            return new Result(t.getId().value(), t.getName(), info(game));
                        }
                        throw new BusinessException.Conflict(
                                "A equipa '" + teamName + "' já está ligada noutro dispositivo");
                    }
                }
                throw new BusinessException.Conflict("Inscrições fechadas (estado: " + game.getStatus() + ")");
            }

            if (existing.isPresent())
                throw new BusinessException.Conflict("Já existe uma equipa chamada '" + teamName + "' nesta partida");

            Team team = Team.create(game.getId(), teamName);
            team.validate();
            Team saved = teams.save(team);
            return new Result(saved.getId().value(), saved.getName(), info(game));
        });
    }

    private GameInfoDTO info(Game game) {
        long gameId = game.getId().value();
        int online = (int) teams.findByGame(gameId).stream().filter(Team::isOnline).count();
        return new GameInfoDTO(gameId, game.getName(), game.getJoinCode(),
                game.getStatus().name(), game.getCurrentRound(), game.getTotalRounds(),
                game.getTimePerQuestion(), online, teams.findByGame(gameId).size());
    }
}
