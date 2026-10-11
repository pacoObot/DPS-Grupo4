package quiz.application.port.output;

import quiz.domain.entity.Game;
import quiz.domain.valueobject.QuestionId;

import java.util.List;
import java.util.Optional;

/** Porta de saída: persistência de partidas e do seu plano de rondas. */
public interface GameRepository {

    Game save(Game game);

    Optional<Game> findById(long id);

    Optional<Game> findByCode(String joinCode);

    List<Game> findAll();

    /** SELECT ... FOR UPDATE na partida (transições de estado concorrentes). */
    Optional<Game> lockById(long id);

    void update(Game game);

    /** Liga a partida às perguntas selecionadas (tabela game_questions). */
    void saveGameQuestions(long gameId, List<QuestionId> questionIds);

    /** Pergunta atribuída a uma ronda específica. */
    Optional<QuestionId> findQuestionIdForRound(long gameId, int roundNumber);

    /** Partida mais recente (para o dashboard). */
    Optional<Game> findLatest();
}
