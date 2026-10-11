package quiz.application.usecase.player;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import quiz.application.port.output.UnitOfWork;
import quiz.application.usecase.admin.CreateQuestionUseCase;
import quiz.application.usecase.admin.ManageGameUseCase;
import quiz.application.usecase.game.StartGameUseCase;
import quiz.application.usecase.game.StartRoundUseCase;
import quiz.application.service.GameService;
import quiz.domain.entity.Team;
import quiz.infrastructure.persistence.postgres.AnswerRepositoryImpl;
import quiz.infrastructure.persistence.postgres.GameRepositoryImpl;
import quiz.infrastructure.persistence.postgres.QuestionRepositoryImpl;
import quiz.infrastructure.persistence.postgres.TeamRepositoryImpl;
import quiz.shared.dto.AnswerResultDTO;
import quiz.support.PostgresIT;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integração do fluxo transacional de resposta (SubmitAnswerUseCase) contra
 * PostgreSQL real: correto, errado, duplicado, tardio e timeout.
 */
class SubmitAnswerIT extends PostgresIT {

    private QuestionRepositoryImpl questions;
    private TeamRepositoryImpl teams;
    private GameRepositoryImpl games;
    private AnswerRepositoryImpl answers;
    private UnitOfWork uow;

    private CreateQuestionUseCase createQuestion;
    private ManageGameUseCase manageGame;
    private JoinGameUseCase joinGame;
    private StartGameUseCase startGame;
    private StartRoundUseCase startRound;
    private SubmitAnswerUseCase submitAnswer;

    @BeforeEach
    void wireUseCases() {
        questions = new QuestionRepositoryImpl(jdbc);
        teams = new TeamRepositoryImpl(jdbc);
        games = new GameRepositoryImpl(jdbc);
        answers = new AnswerRepositoryImpl(jdbc);
        uow = newUnitOfWork();

        createQuestion = new CreateQuestionUseCase(questions, uow);
        manageGame = new ManageGameUseCase(games, questions, uow);
        joinGame = new JoinGameUseCase(games, teams, uow);
        startGame = new StartGameUseCase(games, teams, uow);
        startRound = new StartRoundUseCase(games, teams, new GameService(games, teams, questions), uow);
        submitAnswer = new SubmitAnswerUseCase(teams, games, questions, answers, uow);
    }

    private long question(char correct, int points, int timeLimit) {
        return createQuestion.call("Pergunta teste", "Geral", "EASY",
                "Alternativa A", "Alternativa B", "Alternativa C", "Alternativa D",
                correct, points, timeLimit, true).id();
    }

    private long runningGame(int timePerQuestion, long... questionIds) {
        List<Long> ids = new java.util.ArrayList<>();
        for (long id : questionIds) ids.add(id);

        long gameId = manageGame.create("Prova", ids, timePerQuestion).gameId();
        String code = games.findById(gameId).orElseThrow().getJoinCode();

        // equipas de teste
        joinGame.call(code, "Alpha");
        joinGame.call(code, "Beta");
        joinGame.call(code, "Gama");

        startGame.call(gameId);
        startRound.call(gameId);
        return gameId;
    }

    private long teamId(long gameId, String name) {
        return teams.findByName(gameId, name).orElseThrow().getId().value();
    }

    @Test
    void respostaCorretaPontuaEPersisteProva() {
        long qid = question('A', 10, 30);
        long gameId = runningGame(30, qid);
        long alpha = teamId(gameId, "Alpha");

        AnswerResultDTO r = submitAnswer.call(alpha, qid, 'A');

        assertEquals("CORRECT", r.status());
        assertTrue(r.points() >= 10 && r.points() <= 20, "pontos esperados 10..20, veio " + r.points());
        assertEquals(r.points(), r.totalScore());
        assertTrue(answers.existsByTeamAndQuestion(alpha, qid));

        Team reloaded = teams.findById(alpha).orElseThrow();
        assertEquals(r.points(), reloaded.getScore().getPoints());
        assertEquals(1, reloaded.getScore().getCorrectCount());
    }

    @Test
    void respostaErradaNaoPontuaMasContaErro() {
        long qid = question('A', 10, 30);
        long gameId = runningGame(30, qid);
        long beta = teamId(gameId, "Beta");

        AnswerResultDTO r = submitAnswer.call(beta, qid, 'C');

        assertEquals("WRONG", r.status());
        assertEquals(0, r.points());
        Team reloaded = teams.findById(beta).orElseThrow();
        assertEquals(0, reloaded.getScore().getPoints());
        assertEquals(1, reloaded.getScore().getWrongCount());
    }

    @Test
    void respostaDuplicadaEhDetetadaSemSomarDuasVezes() {
        long qid = question('A', 10, 30);
        long gameId = runningGame(30, qid);
        long alpha = teamId(gameId, "Alpha");

        AnswerResultDTO first = submitAnswer.call(alpha, qid, 'A');
        AnswerResultDTO second = submitAnswer.call(alpha, qid, 'A');

        assertEquals("CORRECT", first.status());
        assertEquals("DUPLICATE", second.status());
        assertEquals(0, second.points());

        Team reloaded = teams.findById(alpha).orElseThrow();
        assertEquals(first.points(), reloaded.getScore().getPoints()); // somado uma só vez
        assertEquals(1, reloaded.getScore().getCorrectCount());
    }

    @Test
    void respostaParaOutraRondaEhTardia() {
        long qid = question('A', 10, 30);
        long gameId = runningGame(30, qid);
        long alpha = teamId(gameId, "Alpha");

        AnswerResultDTO r = submitAnswer.call(alpha, qid + 999, 'A');

        assertEquals("LATE", r.status());
        assertEquals(0, r.points());
        assertFalse(answers.existsByTeamAndQuestion(alpha, qid));
    }

    @Test
    void respostaAposDeadlineEhTimeout() throws InterruptedException {
        long qid = question('A', 10, 1);       // 1 segundo por pergunta
        long gameId = runningGame(1, qid);
        long gama = teamId(gameId, "Gama");

        Thread.sleep(1300);                     // passa o deadline da ronda

        AnswerResultDTO r = submitAnswer.call(gama, qid, 'A');

        assertEquals("TIMEOUT", r.status());
        assertEquals(0, r.points());
        Team reloaded = teams.findById(gama).orElseThrow();
        assertEquals(1, reloaded.getScore().getWrongCount());
    }
}