package quiz.application.port.output;

import quiz.domain.entity.Team;
import quiz.domain.valueobject.TeamId;

import java.util.List;
import java.util.Optional;

/** Porta de saída: persistência de equipas. */
public interface TeamRepository {

    Team save(Team team);

    Optional<Team> findById(long id);

    /**
     * SELECT ... FOR UPDATE — trava a linha da equipa durante a transação.
     * Usado pelo SubmitAnswerUseCase para evitar lost updates quando duas
     * respostas da mesma equipa (ou de equipas concorrentes) escrevem ao mesmo tempo.
     */
    Optional<Team> lockById(long id);

    Optional<Team> findByName(long gameId, String name);

    List<Team> findByGame(long gameId);

    /** Ranking por pontuação decrescente. */
    List<Team> findRanking(long gameId);

    void update(Team team);

    void deleteByGame(long gameId);
}
