package quiz.application.usecase.player;

import quiz.application.service.ScoreService;
import quiz.shared.dto.ScoreboardEntry;

import java.util.List;

/** Classificação atual da partida. */
public class GetScoreboardUseCase {

    private final ScoreService scoreService;

    public GetScoreboardUseCase(ScoreService scoreService) {
        this.scoreService = scoreService;
    }

    public List<ScoreboardEntry> call(long gameId) {
        return scoreService.ranking(gameId);
    }
}
