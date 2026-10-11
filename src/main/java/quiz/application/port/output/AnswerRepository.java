package quiz.application.port.output;

import quiz.domain.entity.Answer;

import java.util.List;

/** Porta de saída: persistência de respostas. */
public interface AnswerRepository {

    /** Persiste a resposta. UNIQUE(team_id, question_id) impede duplicados. */
    Answer save(Answer answer);

    /** Verificação prévia de double submission (a UNIQUE constraint é o 2.º guarda). */
    boolean existsByTeamAndQuestion(long teamId, long questionId);

    /** true se a equipa já respondeu à pergunta ATUAL da ronda. */
    boolean hasAnsweredRound(long teamId, long questionId);

    List<Answer> findByGame(long gameId);

    long countByGame(long gameId);
}
