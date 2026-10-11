package quiz.application.service;

import quiz.application.port.output.AnswerRepository;
import quiz.application.port.output.GameRepository;
import quiz.application.port.output.QuestionRepository;
import quiz.application.port.output.TeamRepository;
import quiz.domain.entity.Game;
import quiz.domain.entity.Team;
import quiz.domain.enums.TeamStatus;
import quiz.shared.dto.ScoreboardEntry;
import quiz.shared.dto.ServerStats;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * =====================================================================
 *  StatsService — agrega dados REAIS para o dashboard administrativo
 * =====================================================================
 *
 *  Consulta repositórios (contagens baratas) e devolve uma fotografia
 *  imutável ({@link ServerStats}). Não guarda estado — é chamado a cada
 *  pedido GET_STATS. Os números de ligações/threads vêm do registry e do
 *  pool (infraestrutura) e são passados pelo handler, mantendo esta
 *  classe livre de dependências de rede.
 */
public final class StatsService {

    private final QuestionRepository questions;
    private final GameRepository games;
    private final TeamRepository teams;
    private final AnswerRepository answers;
    private final ScoreService scoreService;

    public StatsService(QuestionRepository questions, GameRepository games,
                        TeamRepository teams, AnswerRepository answers,
                        ScoreService scoreService) {
        this.questions = questions;
        this.games = games;
        this.teams = teams;
        this.answers = answers;
        this.scoreService = scoreService;
    }

    public ServerStats snapshot(int port, int poolSize, int activeThreads, int connections) {
        QuestionRepository.QuestionStats q = questions.stats();
        Optional<Game> latest = games.findLatest();

        if (latest.isEmpty()) {
            return new ServerStats(port, poolSize, activeThreads, connections,
                    q.total(), q.active(), q.categories(),
                    -1, "-", "-", 0, 0, 0, 0, 0, 0, List.of());
        }

        Game g = latest.get();
        long gameId = g.getId().value();
        List<Team> all = teams.findByGame(gameId);
        int joined = all.size();
        int online = (int) all.stream().filter(Team::isOnline).count();
        long timeouts = all.stream()
                .filter(t -> t.getStatus() == TeamStatus.TIMEOUT)
                .count();
        long answered = answers.countByGame(gameId);

        List<ServerStats.TopEntry> top = new ArrayList<>();
        for (ScoreboardEntry e : scoreService.ranking(gameId)) {
            if (top.size() >= 3) break;
            top.add(new ServerStats.TopEntry(e.position(), e.teamName(), e.points()));
        }

        return new ServerStats(port, poolSize, activeThreads, connections,
                q.total(), q.active(), q.categories(),
                gameId, g.getStatus().name(), g.getName(),
                g.getCurrentRound(), g.getTotalRounds(),
                joined, online, answered, timeouts, top);
    }
}