package quiz.infrastructure.concurrency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quiz.application.port.output.GameRepository;
import quiz.application.port.output.TeamRepository;
import quiz.application.service.ConcurrencyService;
import quiz.application.service.ScoreService;
import quiz.application.usecase.game.FinishGameUseCase;
import quiz.application.usecase.game.FinishRoundUseCase;
import quiz.application.usecase.game.StartRoundUseCase;
import quiz.domain.entity.Game;
import quiz.domain.entity.Team;
import quiz.domain.enums.GameStatus;
import quiz.domain.enums.TeamStatus;
import quiz.infrastructure.network.ClientRegistry;
import quiz.shared.dto.QuestionDTO;
import quiz.shared.dto.ScoreboardEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * =====================================================================
 *  GameScheduler — thread do ciclo de jogo (rondas)
 * =====================================================================
 *
 *  Corre NUMA THREAD PRÓPRIA, nunca bloqueia o accept() nem os ClientHandlers.
 *
 *  Por ronda:
 *    1. StartRoundUseCase  -> round_started_at = agora (autoridade do tempo)
 *    2. broadcast QUESTION -> TODAS as equipas recebem a mesma pergunta
 *    3. loop de espera (200ms):
 *         - todas responderam?   -> CountDownLatch a 0
 *         - tempo expirou?       -> deadline do servidor
 *         - pausa?               -> RoundCoordinator.freeze()
 *    4. TIMEOUT para quem ficou pendente + FinishRoundUseCase (persiste)
 *    5. broadcast SCOREBOARD + NEXT_ROUND
 *
 *  Esta sequência é o diagrama de atividades da documentação.
 */
public final class GameScheduler implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(GameScheduler.class);

    private static final long POLL_MS = 200;
    private static final long BETWEEN_ROUNDS_MS = 3000;

    private final GameRepository games;
    private final TeamRepository teams;
    private final StartRoundUseCase startRound;
    private final FinishRoundUseCase finishRound;
    private final FinishGameUseCase finishGame;
    private final ScoreService scoreService;
    private final RoundCoordinatorRegistry coordinators;
    private final ClientRegistry registry;
    private final ConcurrencyService concurrency;

    private final java.util.Set<Long> activeGames = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final java.util.Map<Long, Thread> workers = new java.util.concurrent.ConcurrentHashMap<>();
    private final Object lock = new Object();

    public GameScheduler(GameRepository games, TeamRepository teams,
                         StartRoundUseCase startRound, FinishRoundUseCase finishRound,
                         FinishGameUseCase finishGame, ScoreService scoreService,
                         RoundCoordinatorRegistry coordinators, ClientRegistry registry,
                         ConcurrencyService concurrency) {
        this.games = games;
        this.teams = teams;
        this.startRound = startRound;
        this.finishRound = finishRound;
        this.finishGame = finishGame;
        this.scoreService = scoreService;
        this.coordinators = coordinators;
        this.registry = registry;
        this.concurrency = concurrency;
    }

    /**
     * Chamado pelo AdminClientHandler após START_GAME.
     * Cada partida corre na sua PRÓPRIA virtual thread, logo podem
     * coexistir várias partidas em paralelo.
     */
    public void activate(long gameId) {
        if (!running.get()) return;
        if (!activeGames.add(gameId)) {
            log.warn("Partida {} já está em execução — ativação ignorada", gameId);
            return;
        }
        Thread worker = Thread.ofVirtual().name("game-" + gameId).unstarted(() -> {
            try {
                runGame(gameId);
            } catch (RuntimeException e) {
                log.error("Erro no ciclo da partida {}: {}", gameId, e.getMessage(), e);
            } finally {
                activeGames.remove(gameId);
                workers.remove(gameId);
                coordinators.remove(gameId);
            }
        });
        workers.put(gameId, worker);
        worker.start();
        log.info("GameScheduler ativado para a partida {}", gameId);
    }

    public void shutdown() {
        running.set(false);
        for (Thread t : workers.values()) {
            t.interrupt();
        }
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    /**
     * A thread principal do scheduler (não-daemon) fica apenas à espera,
     * mantendo o processo vivo; o trabalho de cada partida corre nas
     * virtual threads lançadas em activate().
     */
    @Override
    public void run() {
        log.info("GameScheduler (gestor multi-partida) iniciado — thread={}",
                Thread.currentThread().getName());
        while (running.get()) {
            synchronized (lock) {
                try {
                    lock.wait(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        log.info("GameScheduler terminado");
    }

    private void runGame(long gameId) {
        int round = 0;
        while (running.get()) {
            Game game = games.findById(gameId).orElse(null);
            if (game == null) return;

            GameStatus status = game.getStatus();
            if (status == GameStatus.PAUSED) {
                RoundCoordinator coordinator = coordinators.forGame(gameId);
                coordinator.freeze();
                while (running.get() && games.findById(gameId)
                        .map(g -> g.getStatus() == GameStatus.PAUSED).orElse(false)) {
                    sleep(300);
                }
                coordinator.unfreeze();
                continue;
            }
            if (status != GameStatus.RUNNING) {
                // STOP_GAME pelo admin ou fim natural
                if (status == GameStatus.FINISHED) {
                    broadcastFinal(gameId, games.findById(gameId).orElse(null));
                }
                return;
            }

            round = game.getCurrentRound();
            playRound(gameId, round);

            Game after = games.findById(gameId).orElse(null);
            if (after == null || after.getStatus() != GameStatus.RUNNING) continue;
            if (round >= after.getTotalRounds()) {
                break; // última ronda concluída
            }
            sleep(BETWEEN_ROUNDS_MS);
        }

        Game finalState = games.findById(gameId).orElse(null);
        if (finalState != null && finalState.getStatus() == GameStatus.RUNNING) {
            FinishGameUseCase.Result result = finishGame.call(gameId);
            log.info("PARTIDA {} TERMINADA", gameId);
            broadcastFinal(gameId, finalState);
        }
    }

    /** Executa uma ronda completa na thread do scheduler. */
    private void playRound(long gameId, int round) {
        RoundCoordinator coordinator = coordinators.forGame(gameId);
        QuestionDTO question;
        try {
            question = startRound.call(gameId);   // round_started_at = agora
        } catch (RuntimeException e) {
            log.warn("Não foi possível iniciar a ronda {}: {}", round, e.getMessage());
            return;
        }

        Game game = games.findById(gameId).orElseThrow();

        // deadline SEMPRE no domínio de System.nanoTime() (o relógio de
        // round_started_at é epoch — misturar os dois faria a ronda nunca
        // expirar). Descontamos o tempo já decorrido desde o início oficial.
        long roundMs = game.getTimePerQuestion() * 1000L;
        long startedMs = game.getRoundStartedAt() == null
                ? System.currentTimeMillis()
                : game.getRoundStartedAt().toEpochMilli();
        long elapsedMs = Math.max(0, System.currentTimeMillis() - startedMs);
        long remainingMs = Math.max(0, roundMs - elapsedMs);
        long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(remainingMs);

        // equipas online esperadas nesta ronda
        List<Long> expected = new ArrayList<>();
        for (Team t : teams.findByGame(gameId)) {
            if (t.getStatus() != TeamStatus.OFFLINE) expected.add(t.getId().value());
        }

        coordinator.beginRound(expected, deadlineNanos);

        // ====== TODAS AS EQUIPAS RECEBEM A PERGUNTA (broadcast) ======
        registry.broadcastToGame(gameId, question.toProtocol());
        for (var c : registry.gameConnections(gameId)) {
            concurrency.updateState(c.id(), "ANSWERING");
        }
        registry.broadcastToAdmins("ROUND_EVENT|" + gameId + "|START|" + round + "|" + expected.size());
        log.info("Ronda {}/{} iniciada — {} equipas, {}s por pergunta",
                round, game.getTotalRounds(), expected.size(), game.getTimePerQuestion());

        // ====== ESPERA CONCORRENTE: latch OU deadline OU pausa ======
        boolean paused = false;
        while (running.get()) {
            if (coordinator.allAnswered()) break;
            Game st = games.findById(gameId).orElse(null);
            if (st == null) return;

            if (st.getStatus() == GameStatus.PAUSED) {
                if (!paused) {                 // congela o deadline na pausa
                    coordinator.freeze();
                    paused = true;
                }
                sleep(POLL_MS);
                continue;
            }
            if (paused) {                      // retomou: devolve o tempo
                coordinator.unfreeze();
                paused = false;
            }
            if (st.getStatus() != GameStatus.RUNNING) return;
            if (coordinator.isExpired()) break;
            sleep(POLL_MS);
        }
        if (paused) coordinator.unfreeze();

        long remaining = coordinator.remainingMillis();
        log.info("Ronda {} concluída em {} ({} ms restantes)", round,
                coordinator.allAnswered() ? "todas responderam" : "tempo esgotado", remaining);

        // ====== TIMEOUT para quem ficou pendente ======
        for (Long teamId : coordinator.pendingTeams()) {
            Team t = teams.findById(teamId).orElse(null);
            if (t == null || t.getStatus() == TeamStatus.OFFLINE) continue;
            long total = t.getScore().getPoints();
            registry.sendToTeam(teamId, "ANSWER_RESULT|" + question.questionId()
                    + "|TIMEOUT|0|0|" + total);
            registry.broadcastToAdmins("TEAM_EVENT|" + t.getName() + "|TIMEOUT|"
                    + Thread.currentThread().getName());
            log.info("{} -> TIMEOUT", t.getName());
        }
        coordinator.endRound();

        // ====== persiste timeouts e avança a ronda ======
        try {
            finishRound.call(gameId);
        } catch (RuntimeException e) {
            log.error("Erro ao terminar ronda: {}", e.getMessage());
        }

        // ====== classificação para todos ======
        List<ScoreboardEntry> board = scoreService.ranking(gameId);
        registry.broadcastToGame(gameId, ScoreboardEntry.toProtocol(board));
        registry.broadcastToGame(gameId, "NEXT_ROUND|" + (round + 1));
        registry.broadcastToAdmins("ROUND_EVENT|" + gameId + "|END|" + round);

        printPrettyBoard(board, round);
        for (var c : registry.gameConnections(gameId)) {
            concurrency.updateState(c.id(), "WAITING");
        }
    }

    private void broadcastFinal(long gameId, Game game) {
        List<ScoreboardEntry> board = scoreService.ranking(gameId);
        registry.broadcastToGame(gameId, ScoreboardEntry.toProtocol(board));
        registry.broadcastToGame(gameId, "GAME_OVER");
        registry.broadcastToAdmins("GAME_STATUS|" + gameId + "|FINISHED");

        StringBuilder sb = new StringBuilder("CLASSIFICAÇÃO FINAL:\n");
        for (ScoreboardEntry e : board) {
            sb.append(String.format("  %dº %-16s %3d pts  ✓%d ✗%d%n",
                    e.position(), e.teamName(), e.points(), e.correct(), e.wrong()));
        }
        log.info("\n{}", sb);
    }

    private void printPrettyBoard(List<ScoreboardEntry> board, int round) {
        StringBuilder sb = new StringBuilder("CLASSIFICAÇÃO após ronda " + round + ":");
        for (ScoreboardEntry e : board) {
            sb.append(String.format("  %dº %s %dpts", e.position(), e.teamName(), e.points()));
        }
        log.info("{}", sb);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
