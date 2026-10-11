package quiz.application.port.output;

import quiz.domain.entity.Question;

import java.util.List;
import java.util.Optional;

/** Porta de saída: persistência de perguntas. Implementação em infra (PostgreSQL). */
public interface QuestionRepository {

    /** Persiste e devolve a instância com o id preenchido. */
    Question save(Question question);

    Optional<Question> findById(long id);

    /** Lista com filtros opcionais (pesquisa por texto, dificuldade, ativas). */
    List<Question> findAll(String search, String difficulty, Boolean active);

    /** Perguntas ativas selecionadas para criar uma partida. */
    List<Question> findActive(int limit);

    void update(Question question);

    void delete(long id);

    /** Contagem agregada para o dashboard (1 query, sem carregar linhas). */
    record QuestionStats(long total, long active, long categories) {}

    QuestionStats stats();

    /** true se a pergunta já foi usada numa partida (game_questions/answers). */
    boolean isReferenced(long id);
}
