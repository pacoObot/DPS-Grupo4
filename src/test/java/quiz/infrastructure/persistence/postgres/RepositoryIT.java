package quiz.infrastructure.persistence.postgres;

import org.junit.jupiter.api.Test;
import quiz.domain.entity.Game;
import quiz.domain.entity.Question;
import quiz.domain.entity.Team;
import quiz.domain.enums.Difficulty;
import quiz.domain.enums.GameStatus;
import quiz.domain.valueobject.GameId;
import quiz.domain.valueobject.QuestionId;
import quiz.support.PostgresIT;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integração dos repositórios contra o PostgreSQL real:
 * pergunta, partida (+ plano de rondas) e equipas (+ ranking).
 */
class RepositoryIT extends PostgresIT {

    private final QuestionRepositoryImpl questions = new QuestionRepositoryImpl(jdbc);
    private final GameRepositoryImpl games = new GameRepositoryImpl(jdbc);
    private final TeamRepositoryImpl teams = new TeamRepositoryImpl(jdbc);

    private Question newQuestion(String text, char correct) {
        Question q = Question.create(text, "Geral", Difficulty.EASY,
                "A1", "B2", "C3", "D4", correct, 10, 20, true);
        q.validate();
        return questions.save(q);
    }

    @Test
    void perguntaFazRoundTripComRespostaCorreta() {
        Question saved = newQuestion("Capital de Portugal?", 'A');
        assertTrue(saved.getId().value() > 0);

        Question loaded = questions.findById(saved.getId().value()).orElseThrow();
        assertEquals("Capital de Portugal?", loaded.getText());
        assertEquals('A', loaded.getCorrectOption());
        assertTrue(loaded.isCorrect('a'));
    }

    @Test
    void filtroPorAtivasFunciona() {
        Question ativa = newQuestion("Ativa?", 'A');
        Question inativa = questions.save(Question.create("Inativa?", "Geral", Difficulty.EASY,
                "a", "b", "c", "d", 'B', 10, 20, false));

        List<Question> ativas = questions.findAll(null, null, true);
        assertTrue(ativas.stream().anyMatch(q -> q.getId().equals(ativa.getId())));
        assertTrue(ativas.stream().noneMatch(q -> q.getId().equals(inativa.getId())));
    }

    @Test
    void partidaGuardaPlanoDeRondasELePerguntaPorRonda() {
        Question q1 = newQuestion("Pergunta 1", 'A');
        Question q2 = newQuestion("Pergunta 2", 'B');

        Game g = Game.create("Prova", 15, List.of(q1.getId(), q2.getId()));
        g.setJoinCode("CODIGO1");
        Game saved = games.save(g);
        games.saveGameQuestions(saved.getId().value(), List.of(q1.getId(), q2.getId()));

        assertEquals(q1.getId().value(),
                games.findQuestionIdForRound(saved.getId().value(), 1).orElseThrow().value());
        assertEquals(q2.getId().value(),
                games.findQuestionIdForRound(saved.getId().value(), 2).orElseThrow().value());

        Game loaded = games.findById(saved.getId().value()).orElseThrow();
        assertEquals(GameStatus.OPEN, loaded.getStatus());
        assertEquals(2, loaded.getQuestionIds().size());
        assertEquals(2, loaded.getTotalRounds());
    }

    @Test
    void encontradaPeloCodigoDeEntrada() {
        Game g = Game.create("Prova", 15, List.of(new QuestionId(newQuestion("P", 'A').getId().value())));
        g.setJoinCode("XYZ789");
        Game saved = games.save(g);

        assertEquals(saved.getId().value(), games.findByCode("XYZ789").orElseThrow().getId().value());
        assertTrue(games.findByCode("NAOEXISTE").isEmpty());
    }

    @Test
    void rankingOrdenaPorPontuacaoDescendente() {
        Game g = Game.create("Prova", 15, List.of(newQuestion("P", 'A').getId()));
        g.setJoinCode("RANK01");
        Game saved = games.save(g);
        long gameId = saved.getId().value();

        Team a = Team.create(new GameId(gameId), "Alpha");
        a.registerCorrectAnswer(20);
        Team sa = teams.save(a);

        Team b = Team.create(new GameId(gameId), "Beta");
        b.registerCorrectAnswer(5);
        teams.save(b);

        List<Team> ranking = teams.findRanking(gameId);
        assertEquals(2, ranking.size());
        assertEquals("Alpha", ranking.get(0).getName());
        assertEquals(20, ranking.get(0).getScore().getPoints());

        assertTrue(teams.lockById(sa.getId().value()).isPresent());
        assertTrue(teams.findByName(gameId, "Alpha").isPresent());
    }
}