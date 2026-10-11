package quiz.application.service;

import quiz.application.port.output.TeamRepository;
import quiz.domain.entity.Team;
import quiz.shared.dto.ScoreboardEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * ScoreService — cálculo e apresentação da classificação.
 * Ranking consultado do PostgreSQL:
 *   SELECT ... FROM teams WHERE game_id = ? ORDER BY score DESC
 */
public class ScoreService {

    private final TeamRepository teams;

    public ScoreService(TeamRepository teams) {
        this.teams = teams;
    }

    /** Classificação com posições (1º, 2º, ...). */
    public List<ScoreboardEntry> ranking(long gameId) {
        List<Team> sorted = teams.findRanking(gameId);
        List<ScoreboardEntry> out = new ArrayList<>(sorted.size());
        int pos = 1;
        for (Team t : sorted) {
            out.add(new ScoreboardEntry(pos++,
                    t.getName(),
                    t.getScore().getPoints(),
                    t.getScore().getCorrectCount(),
                    t.getScore().getWrongCount(),
                    t.getStatus().name()));
        }
        return out;
    }

    /** Texto pronto a mostrar no monitor do admin. */
    public String prettyRanking(long gameId) {
        StringBuilder sb = new StringBuilder("CLASSIFICAÇÃO\n");
        for (ScoreboardEntry e : ranking(gameId)) {
            sb.append(String.format("  %dº %-16s %3d pts  ✓%d ✗%d  [%s]%n",
                    e.position(), e.teamName(), e.points(), e.correct(), e.wrong(), e.status()));
        }
        return sb.toString();
    }
}
