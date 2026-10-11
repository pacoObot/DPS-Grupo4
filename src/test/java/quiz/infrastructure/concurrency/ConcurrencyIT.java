package quiz.infrastructure.concurrency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import quiz.application.port.output.UnitOfWork;
import quiz.application.service.GameService;
import quiz.application.usecase.admin.CreateQuestionUseCase;
import quiz.application.usecase.admin.ManageGameUseCase;
import quiz.application.usecase.game.StartGameUseCase;
import quiz.application.usecase.game.StartRoundUseCase;
import quiz.application.usecase.player.JoinGameUseCase;
import quiz.application.usecase.player.SubmitAnswerUseCase;
import quiz.domain.entity.Team;
import quiz.infrastructure.persistence.postgres.AnswerRepositoryImpl;
import quiz.infrastructure.persistence.postgres.GameRepositoryImpl;
import quiz.infrastructure.persistence.postgres.QuestionRepositoryImpl;
import quiz.infrastructure.persistence.postgres.TeamRepositoryImpl;
import quiz.shared.dto.AnswerResultDTO;
import quiz.support.PostgresIT;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PROVA DE CONCORRÊNCIA contra PostgreSQL real.
 *
 * Vários clientes (threads) respondem SIMULTANEAMENTE à mesma pergunta.
 * O par SELECT ... FOR UPDATE (partida -> equipa) + transação por thread
 * garante que:
 *   - nenhuma pontuação é perdida (sem "lost update");
 *   - uma equipa só soma uma vez por pergunta (UNIQUE + trava da linha).
 */
class ConcurrencyIT extends PostgresIT {

    private static final int TEAMS = 24;

    private QuestionRepositoryImpl questions;
    private TeamRepositoryImpl teams;
    private GameRepositoryImpl games;
    private AnswerRepositoryImpl answers;
    private SubmitAnswerUseCase submitAnswer;

    private long qid;
    private long gameId;
    private List<Long> teamIds = new ArrayList<>();

    @BeforeEach
    void setUpGame() {
        questions = new QuestionRepositoryImpl(jdbc);
        teams = new TeamRepositoryImpl(jdbc);
        games = new GameRepositoryImpl(jdbc);
        answers = new AnswerRepositoryImpl(jdbc);
        UnitOfWork uow = newUnitOfWork();

        CreateQuestionUseCase createQuestion = new CreateQuestionUseCase(questions, uow);
        ManageGameUseCase manageGame = new ManageGameUseCase(games, questions, uow);
        JoinGameUseCase joinGame = new JoinGameUseCase(games, teams, uow);
        StartGameUseCase startGame = new StartGameUseCase(games, teams, uow);
        StartRoundUseCase startRound = new StartRoundUseCase(games, teams,
                new GameService(games, teams, questions), uow);
        submitAnswer = new SubmitAnswerUseCase(teams, games, questions, answers, uow);

        qid = createQuestion.call("Escolha A", "Geral", "EASY",
                "A", "B", "C", "D", 'A', 10, 60, true).id();

        gameId = manageGame.create("Corrida", List.of(qid), 60).gameId();
        String code = games.findById(gameId).orElseThrow().getJoinCode();

        for (int i = 1; i <= TEAMS; i++) {
            teamIds.add(joinGame.call(code, "Equipa-" + i).teamId());
        }

        startGame.call(gameId);
        startRound.call(gameId);
    }

    @Test
    void respostasSimultaneasNaoPerdemPontos() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(TEAMS);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<AnswerResultDTO>> futures = new ArrayList<>();

        for (long id : teamIds) {
            futures.add(pool.submit((Callable<AnswerResultDTO>) () -> {
                startGate.await();
                return submitAnswer.call(id, qid, 'A');
            }));
        }
        startGate.countDown(); // dispara todas as threads ao mesmo tempo

        int totalPoints = 0;
        int correct = 0;
        for (Future<AnswerResultDTO> f : futures) {
            AnswerResultDTO r = f.get(30, TimeUnit.SECONDS);
            if ("CORRECT".equals(r.status())) {
                correct++;
                totalPoints += r.points();
                assertTrue(r.points() >= 10 && r.points() <= 20);
            }
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(TEAMS, correct, "todas as equipas deviam acertar");

        // nenhuma resposta perdida: soma do banco == soma dos resultados
        int dbTotal = 0;
        int dbCorrect = 0;
        for (Team t : teams.findByGame(gameId)) {
            dbTotal += t.getScore().getPoints();
            dbCorrect += t.getScore().getCorrectCount();
            assertEquals(1, t.getScore().getCorrectCount(),
                    "cada equipa deve ter exatamente 1 acerto");
        }
        assertEquals(totalPoints, dbTotal, "pontuação total divergente (lost update?)");
        assertEquals(TEAMS, dbCorrect);
        assertEquals(TEAMS, answers.countByGame(gameId));
    }

    @Test
    void duasSubmissoesSimultaneasDaMesmaEquipaContamSoUma() throws Exception {
        long alpha = teamIds.get(0);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch startGate = new CountDownLatch(1);

        Callable<AnswerResultDTO> task = () -> {
            startGate.await();
            return submitAnswer.call(alpha, qid, 'A');
        };
        Future<AnswerResultDTO> f1 = pool.submit(task);
        Future<AnswerResultDTO> f2 = pool.submit(task);
        startGate.countDown();

        AnswerResultDTO r1 = f1.get(30, TimeUnit.SECONDS);
        AnswerResultDTO r2 = f2.get(30, TimeUnit.SECONDS);
        pool.shutdown();

        List<String> statuses = List.of(r1.status(), r2.status());
        long processed = statuses.stream().filter(s -> s.equals("CORRECT") || s.equals("WRONG")).count();
        long duplicates = statuses.stream().filter(s -> s.equals("DUPLICATE")).count();

        assertEquals(1, processed, "exatamente uma submissão deve ser processada: " + statuses);
        assertEquals(1, duplicates, "a outra deve ser DUPLICATE: " + statuses);

        Team reloaded = teams.findById(alpha).orElseThrow();
        assertEquals(1, reloaded.getScore().getCorrectCount());
        assertEquals(1, answers.countByGame(gameId));
    }

    @Test
    void acertoNuncaExcedeUmPorEquipaPorPergunta() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        AtomicInteger processed = new AtomicInteger();

        List<Callable<Void>> tasks = new ArrayList<>();
        for (long id : teamIds) {
            for (int k = 0; k < 5; k++) { // 5 respostas por equipa, em paralelo
                tasks.add(() -> {
                    AnswerResultDTO r = submitAnswer.call(id, qid, 'A');
                    if (!"DUPLICATE".equals(r.status())) processed.incrementAndGet();
                    return null;
                });
            }
        }
        pool.invokeAll(tasks);
        pool.shutdown();

        // só uma resposta por equipa é processada; as restantes são DUPLICATE
        assertEquals(TEAMS, processed.get());
        assertEquals(TEAMS, answers.countByGame(gameId));
    }
}