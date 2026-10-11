package quiz.infrastructure.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quiz.application.exception.BusinessException;
import quiz.application.service.ConcurrencyService;
import quiz.application.service.ScoreService;
import quiz.application.service.StatsService;
import quiz.application.usecase.admin.AdminLoginUseCase;
import quiz.application.usecase.admin.CreateQuestionUseCase;
import quiz.application.usecase.admin.DeleteQuestionUseCase;
import quiz.application.usecase.admin.ListQuestionsUseCase;
import quiz.application.usecase.admin.ManageGameUseCase;
import quiz.application.usecase.admin.UpdateQuestionUseCase;
import quiz.application.usecase.game.StartGameUseCase;
import quiz.application.usecase.player.GetScoreboardUseCase;
import quiz.domain.entity.Team;
import quiz.application.port.output.GameRepository;
import quiz.application.port.output.TeamRepository;
import quiz.infrastructure.concurrency.GameScheduler;
import quiz.shared.dto.GameInfoDTO;
import quiz.shared.dto.QuestionRowDTO;
import quiz.shared.dto.ScoreboardEntry;
import quiz.shared.dto.ServerStats;
import quiz.shared.protocol.Protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * =====================================================================
 *  AdminClientHandler — comandos do ADMINISTRADOR
 * =====================================================================
 *
 *  Fluxo obrigatório da spec §10:
 *    Admin (JavaFX) -> TCP -> AdminClientHandler -> UseCase -> Repository -> PostgreSQL
 *
 *  O controlador (esta classe) NUNCA toca em JDBC nem prepara SQL —
 *  apenas traduz mensagens do protocolo em chamadas de casos de uso.
 */
public final class AdminClientHandler {

    private static final Logger log = LoggerFactory.getLogger(AdminClientHandler.class);

    private final AdminLoginUseCase login;
    private final CreateQuestionUseCase createQuestion;
    private final UpdateQuestionUseCase updateQuestion;
    private final DeleteQuestionUseCase deleteQuestion;
    private final ListQuestionsUseCase listQuestions;
    private final ManageGameUseCase manageGame;
    private final StartGameUseCase startGame;
    private final GetScoreboardUseCase getScoreboard;
    private final ScoreService scoreService;
    private final ConcurrencyService concurrency;
    private final TeamRepository teams;
    private final GameRepository games;
    private final ClientRegistry registry;
    private final GameScheduler scheduler;
    private final StatsService statsService;
    private final int serverPort;
    private final int poolSize;

    public AdminClientHandler(AdminLoginUseCase login,
                              CreateQuestionUseCase createQuestion,
                              UpdateQuestionUseCase updateQuestion,
                              DeleteQuestionUseCase deleteQuestion,
                              ListQuestionsUseCase listQuestions,
                              ManageGameUseCase manageGame,
                              StartGameUseCase startGame,
                              GetScoreboardUseCase getScoreboard,
                              ScoreService scoreService,
                              ConcurrencyService concurrency,
                              TeamRepository teams,
                              GameRepository games,
                              ClientRegistry registry,
                              GameScheduler scheduler,
                              StatsService statsService,
                              int serverPort,
                              int poolSize) {
        this.login = login;
        this.createQuestion = createQuestion;
        this.updateQuestion = updateQuestion;
        this.deleteQuestion = deleteQuestion;
        this.listQuestions = listQuestions;
        this.manageGame = manageGame;
        this.startGame = startGame;
        this.getScoreboard = getScoreboard;
        this.scoreService = scoreService;
        this.concurrency = concurrency;
        this.teams = teams;
        this.games = games;
        this.registry = registry;
        this.scheduler = scheduler;
        this.statsService = statsService;
        this.serverPort = serverPort;
        this.poolSize = poolSize;
    }

    /** ADMIN_LOGIN|user|pass — primeira mensagem do admin. */
    public boolean authenticate(Protocol.Message msg, TcpConnection conn) {
        if (msg.count() < 2) {
            conn.send("LOGIN_ERROR|formato: ADMIN_LOGIN|user|pass");
            return false;
        }
        try {
            login.call(msg.field(0), msg.field(1));
            conn.markAdmin();
            conn.send("LOGIN_OK|" + msg.field(0));
            log.info("Admin '{}' autenticado ({}) thread={}",
                    msg.field(0), conn.remote(), conn.threadName());
            return true;
        } catch (BusinessException e) {
            conn.send("LOGIN_ERROR|" + e.getMessage());
            return false;
        }
    }

    public void handle(Protocol.Message msg, TcpConnection conn) {
        try {
            switch (msg.type()) {
                case "PING"                 -> conn.send("PONG");
                case "CREATE_QUESTION"      -> createQuestion(msg, conn);
                case "UPDATE_QUESTION"      -> updateQuestion(msg, conn);
                case "DELETE_QUESTION"      -> deleteQuestion(msg, conn);
                case "LIST_QUESTIONS"       -> listQuestions(msg, conn);
                case "SET_QUESTION_ACTIVE"  -> setQuestionActive(msg, conn);
                case "CREATE_GAME"          -> createGame(msg, conn);
                case "OPEN_GAME"            -> openGame(msg, conn);
                case "START_GAME"           -> startGame(msg, conn);
                case "PAUSE_GAME"           -> pauseGame(msg, conn);
                case "RESUME_GAME"          -> resumeGame(msg, conn);
                case "STOP_GAME"            -> stopGame(msg, conn);
                case "GET_TEAMS"            -> getTeams(msg, conn);
                case "GET_SCOREBOARD"       -> getScoreboard(msg, conn);
                case "GET_MONITOR"          -> getMonitor(conn);
                case "GET_STATS"            -> getStats(conn);
                default                     -> conn.send("ERROR|comando desconhecido: " + msg.type());
            }
        } catch (BusinessException e) {
            conn.send("ERROR|" + e.getMessage());
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            conn.send("ERROR|mensagem malformada — veja docs/protocolo.md");
        } catch (RuntimeException e) {
            log.error("Erro admin em {}: {}", msg.type(), e.getMessage());
            conn.send("ERROR|falha interna: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    //  Perguntas (CRUD)
    // ------------------------------------------------------------------

    private void createQuestion(Protocol.Message m, TcpConnection conn) {
        if (m.count() < 11) {
            conn.send("ERROR|CREATE_QUESTION|text|cat|diff|A|B|C|D|correct|pts|tempo|active");
            return;
        }
        QuestionRowDTO q = createQuestion.call(
                m.field(0), m.field(1), m.field(2),
                m.field(3), m.field(4), m.field(5), m.field(6),
                m.field(7).charAt(0), m.intField(8, 10), m.intField(9, 15),
                m.boolField(10, true));
        conn.send("QUESTION_CREATED|" + q.id());
        log.info("Pergunta #{} criada por admin", q.id());
    }

    private void updateQuestion(Protocol.Message m, TcpConnection conn) {
        if (m.count() < 12) {
            conn.send("ERROR|UPDATE_QUESTION|id|text|cat|diff|A|B|C|D|correct|pts|tempo|active");
            return;
        }
        QuestionRowDTO q = updateQuestion.call(
                m.longField(0, -1),
                m.field(1), m.field(2), m.field(3),
                m.field(4), m.field(5), m.field(6), m.field(7),
                m.field(8).charAt(0), m.intField(9, 10), m.intField(10, 15),
                m.boolField(11, true));
        conn.send("QUESTION_UPDATED|" + q.id());
    }

    private void deleteQuestion(Protocol.Message m, TcpConnection conn) {
        long id = m.longField(0, -1);
        deleteQuestion.call(id);
        conn.send("QUESTION_DELETED|" + id);
        log.info("Pergunta #{} eliminada", id);
    }

    private void listQuestions(Protocol.Message m, TcpConnection conn) {
        String search = m.field(0);
        String difficulty = m.field(1);
        String activeRaw = m.field(2);
        Boolean active = (activeRaw == null || activeRaw.isBlank()) ? null
                : activeRaw.equalsIgnoreCase("true") || activeRaw.equals("1");

        List<QuestionRowDTO> rows = listQuestions.call(search, difficulty, active);
        for (QuestionRowDTO row : rows) conn.send(row.toProtocol());
        conn.send("LIST_END|" + rows.size());
    }

    private void setQuestionActive(Protocol.Message m, TcpConnection conn) {
        long id = m.longField(0, -1);
        boolean active = m.boolField(1, true);
        updateQuestion.setActive(id, active);
        conn.send("QUESTION_UPDATED|" + id);
        log.info("Pergunta #{} {}", id, active ? "ATIVADA" : "DESATIVADA");
    }

    // ------------------------------------------------------------------
    //  Partida
    // ------------------------------------------------------------------

    private void createGame(Protocol.Message m, TcpConnection conn) {
        if (m.count() < 3) {
            conn.send("ERROR|CREATE_GAME|nome|q1,q2,q3|tempoSegundos");
            return;
        }
        List<Long> ids = new ArrayList<>();
        for (String part : m.field(1).split(",")) {
            if (!part.isBlank()) ids.add(Long.parseLong(part.trim()));
        }
        GameInfoDTO g = manageGame.create(m.field(0), ids, m.intField(2, 15));
        conn.send("GAME_CREATED|" + g.gameId() + "|" + g.joinCode());
        log.info("Partida '{}' criada com código {} ({} rondas)", g.name(), g.joinCode(), g.totalRounds());
    }

    private void openGame(Protocol.Message m, TcpConnection conn) {
        GameInfoDTO g = manageGame.open(m.longField(0, -1));
        conn.send(g.toProtocol());
        registry.broadcastToAdmins("GAME_STATUS|" + g.gameId() + "|OPEN");
        log.info("Inscrições abertas para a partida {} ({})", g.gameId(), g.joinCode());
    }

    private void startGame(Protocol.Message m, TcpConnection conn) {
        long gameId = m.longField(0, -1);
        GameInfoDTO g = startGame.call(gameId);
        conn.send(g.toProtocol());
        registry.broadcastToGame(gameId, "GAME_STARTED|" + gameId);
        registry.broadcastToAdmins("GAME_STATUS|" + gameId + "|RUNNING");
        // acorda o GameScheduler para executar as rondas desta partida
        scheduler.activate(gameId);
        log.info("Partida {} INICIADA — {} rondas de {}s", gameId, g.totalRounds(), g.timePerQuestion());
    }

    private void pauseGame(Protocol.Message m, TcpConnection conn) {
        long gameId = m.longField(0, -1);
        GameInfoDTO g = manageGame.pause(gameId);
        conn.send(g.toProtocol());
        registry.broadcastToGame(gameId, "GAME_PAUSED|" + gameId);
        log.info("Partida {} PAUSADA", gameId);
    }

    private void resumeGame(Protocol.Message m, TcpConnection conn) {
        long gameId = m.longField(0, -1);
        GameInfoDTO g = manageGame.resume(gameId);
        conn.send(g.toProtocol());
        registry.broadcastToGame(gameId, "GAME_RESUMED|" + gameId);
        log.info("Partida {} RETOMADA", gameId);
    }

    private void stopGame(Protocol.Message m, TcpConnection conn) {
        long gameId = m.longField(0, -1);
        GameInfoDTO g = manageGame.stop(gameId);
        conn.send(g.toProtocol());
        registry.broadcastToGame(gameId, "GAME_STOPPED|" + gameId);
        log.info("Partida {} ENCERRADA pelo admin", gameId);
    }

    // ------------------------------------------------------------------
    //  Monitorização
    // ------------------------------------------------------------------

    private void getTeams(Protocol.Message m, TcpConnection conn) {
        long gameId = m.longField(0, -1);
        Map<Long, String> threadByTeam = registry.gameConnections(gameId).stream()
                .collect(Collectors.toMap(c -> c.teamId(), TcpConnection::threadName, (a, b) -> a));

        List<Team> all = teams.findByGame(gameId);
        StringBuilder sb = new StringBuilder("TEAMS");
        for (Team t : all) {
            sb.append('|').append(t.getName())
              .append(':').append(t.getScore().getPoints())
              .append(':').append(t.getStatus())
              .append(':').append(threadByTeam.getOrDefault(t.getId().value(), "-"));
        }
        conn.send(sb.toString());
    }

    private void getScoreboard(Protocol.Message m, TcpConnection conn) {
        long gameId = m.longField(0, -1);
        List<ScoreboardEntry> board = getScoreboard.call(gameId);
        conn.send(ScoreboardEntry.toProtocol(board));
    }

    /** MONITORAMENTO CONCORRENTE (spec §26) — prova do paralelismo. */
    private void getMonitor(TcpConnection conn) {
        conn.send(concurrency.toProtocol());
        conn.send("POOL|size=" + registry.count() + "|activeThreads=" + concurrency.activeThreads());
        conn.send("THREAD_NAMES|"
                + registry.all().stream().map(TcpConnection::threadName)
                        .distinct().sorted().reduce((a, b) -> a + "," + b).orElse("-"));
    }

    /** Fotografia agregada (dados reais) para o dashboard administrativo. */
    private void getStats(TcpConnection conn) {
        ServerStats stats = statsService.snapshot(
                serverPort, poolSize, concurrency.activeThreads(), registry.count());
        conn.send(stats.toProtocol());
    }
}
