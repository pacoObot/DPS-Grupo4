package quiz.infrastructure.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quiz.application.exception.BusinessException;
import quiz.application.service.ConcurrencyService;
import quiz.application.usecase.player.GetQuestionUseCase;
import quiz.application.usecase.player.GetScoreboardUseCase;
import quiz.application.usecase.player.JoinGameUseCase;
import quiz.application.usecase.player.LeaveGameUseCase;
import quiz.application.usecase.player.SubmitAnswerUseCase;
import quiz.domain.entity.Game;
import quiz.domain.entity.Team;
import quiz.application.port.output.GameRepository;
import quiz.application.port.output.TeamRepository;
import quiz.infrastructure.concurrency.RoundCoordinatorRegistry;
import quiz.shared.dto.AnswerResultDTO;
import quiz.shared.dto.QuestionDTO;
import quiz.shared.dto.ScoreboardEntry;
import quiz.shared.protocol.Protocol;

import java.util.List;

/**
 * =====================================================================
 *  PlayerClientHandler — comandos de JOGADOR
 * =====================================================================
 *
 *  Corre dentro da thread do ClientHandler desta ligação — ou seja,
 *  cada equipa é processada pela sua thread, em paralelo com as outras.
 *
 *  O fluxo ANSWER é o mais sensível:
 *    thread do cliente -> SubmitAnswerUseCase (transação PG)
 *                      -> RoundCoordinator.signal (contagem da ronda)
 */
public final class PlayerClientHandler {

    private static final Logger log = LoggerFactory.getLogger(PlayerClientHandler.class);

    private final ClientRegistry registry;
    private final RoundCoordinatorRegistry coordinators;
    private final ConcurrencyService concurrency;
    private final JoinGameUseCase joinGame;
    private final SubmitAnswerUseCase submitAnswer;
    private final GetQuestionUseCase getQuestion;
    private final GetScoreboardUseCase getScoreboard;
    private final LeaveGameUseCase leaveGame;
    private final TeamRepository teams;
    private final GameRepository games;

    public PlayerClientHandler(ClientRegistry registry, RoundCoordinatorRegistry coordinators,
                               ConcurrencyService concurrency, JoinGameUseCase joinGame,
                               SubmitAnswerUseCase submitAnswer, GetQuestionUseCase getQuestion,
                               GetScoreboardUseCase getScoreboard, LeaveGameUseCase leaveGame,
                               TeamRepository teams, GameRepository games) {
        this.registry = registry;
        this.coordinators = coordinators;
        this.concurrency = concurrency;
        this.joinGame = joinGame;
        this.submitAnswer = submitAnswer;
        this.getQuestion = getQuestion;
        this.getScoreboard = getScoreboard;
        this.leaveGame = leaveGame;
        this.teams = teams;
        this.games = games;
    }

    /** JOIN_GAME|codigo|NomeEquipa — primeira mensagem do jogador. */
    public boolean join(Protocol.Message msg, TcpConnection conn) {
        if (msg.count() < 2) {
            conn.send("JOIN_ERROR|formato: JOIN_GAME|codigo|NomeEquipa");
            return false;
        }
        try {
            JoinGameUseCase.Result r = joinGame.call(msg.field(0), msg.field(1));
            conn.bindTeam(r.teamId(), r.game().gameId());

            concurrency.attachTeam(conn.id(), r.teamName());
            long online = registry.onlineTeams(r.game().gameId());

            conn.send("JOIN_OK|" + r.teamId() + "|" + r.game().gameId() + "|" + r.game().name()
                    + "|" + r.game().joinCode());
            conn.send(r.game().toProtocol());
            broadcastWaiting(r.game().gameId());

            // Reconexão a uma partida já a decorrer: repõe o estado no cliente
            // (ecrã de jogo + pergunta atual), para não ficar preso no lobby.
            String status = r.game().status();
            if ("RUNNING".equals(status) || "PAUSED".equals(status)) {
                conn.send("PAUSED".equals(status)
                        ? "GAME_PAUSED|" + r.game().gameId()
                        : "GAME_STARTED|" + r.game().gameId());
                try {
                    conn.send(getQuestion.call(r.teamId()).toProtocol());
                } catch (BusinessException ignored) {
                    // ainda sem ronda ativa — o cliente continua no ecrã de jogo
                }
            }

            log.info("Equipe {} entrou na partida {} ({} online) — thread={}",
                    r.teamName(), r.game().name(), online, conn.threadName());
            return true;
        } catch (BusinessException e) {
            conn.send("JOIN_ERROR|" + e.getMessage());
            return false;
        }
    }

    /** Despacha os comandos autenticados. */
    public void handle(Protocol.Message msg, TcpConnection conn) {
        switch (msg.type()) {
            case "ANSWER"         -> handleAnswer(msg, conn);
            case "GET_QUESTION"   -> handleGetQuestion(conn);
            case "GET_SCOREBOARD" -> handleScoreboard(conn);
            case "LEAVE_GAME"     -> handleLeave(conn);
            case "PING"           -> conn.send("PONG");
            default               -> conn.send("ERROR|comando desconhecido: " + msg.type());
        }
    }

    private void handleAnswer(Protocol.Message msg, TcpConnection conn) {
        long questionId = msg.longField(0, -1);
        String optField = msg.field(1);

        // validação estrita da opção: exatamente uma letra A-D
        if (questionId < 0 || optField == null || optField.strip().length() != 1) {
            conn.send("ERROR|formato: ANSWER|questionId|A-D");
            return;
        }
        char option = Character.toUpperCase(optField.strip().charAt(0));
        if (option < 'A' || option > 'D') {
            conn.send("ERROR|opção deve ser A, B, C ou D");
            return;
        }

        try {
            // ====== PONTO DE ENCONTRO DA CONCORRÊNCIA ======
            // Esta thread (a da equipa) executa a transação em paralelo
            // com as threads das outras equipas.
            AnswerResultDTO r = submitAnswer.call(conn.teamId(), questionId, option);

            conn.send(Protocol.build("ANSWER_RESULT", r.questionId(), r.status(),
                    r.points(), r.bonus(), r.totalScore()));

            if (r.status().equals("CORRECT") || r.status().equals("WRONG")) {
                coordinators.forGame(conn.gameId()).signal(conn.teamId());
                concurrency.updateState(conn.id(), "ANSWERED");
            } else {
                concurrency.updateState(conn.id(), r.status());
            }

            log.info("Equipe {} -> {} (q={}, pts={}) thread={}",
                    teamName(conn.teamId()), r.status(), r.questionId(), r.points(),
                    Thread.currentThread().getName());

            // evento ao vivo para o monitor do admin
            registry.broadcastToAdmins("TEAM_EVENT|" + teamName(conn.teamId()) + "|" + r.status()
                    + "|" + Thread.currentThread().getName());

        } catch (BusinessException e) {
            conn.send("ERROR|" + e.getMessage());
        } catch (RuntimeException e) {
            log.error("Erro ao processar resposta: {}", e.getMessage());
            conn.send("ERROR|falha interna ao processar a resposta");
        }
    }

    private void handleGetQuestion(TcpConnection conn) {
        try {
            QuestionDTO q = getQuestion.call(conn.teamId());
            conn.send(q.toProtocol());
            concurrency.updateState(conn.id(), "ANSWERING");
        } catch (BusinessException e) {
            conn.send("ERROR|" + e.getMessage());
        }
    }

    private void handleScoreboard(TcpConnection conn) {
        try {
            long gameId = conn.gameId();
            if (gameId < 0) {
                conn.send("ERROR|jogador não está inscrito");
                return;
            }
            List<ScoreboardEntry> board = getScoreboard.call(gameId);
            conn.send(ScoreboardEntry.toProtocol(board));
        } catch (BusinessException e) {
            conn.send("ERROR|" + e.getMessage());
        }
    }

    private void handleLeave(TcpConnection conn) {
        if (conn.teamId() > 0) {
            long gameId = conn.gameId();
            try {
                leaveGame.call(conn.teamId());
            } catch (BusinessException ignored) {
            }
            log.info("Equipe {} saiu da partida", teamName(conn.teamId()));
            registry.broadcastToGame(gameId, "INFO|equipe " + teamName(conn.teamId()) + " desconectada");
            broadcastWaiting(gameId);
        }
        conn.send("LEFT");
    }

    /** Aplica a desconexão real (chamada no finally do ClientHandler). */
    public void onDisconnect(TcpConnection conn) {
        if (conn.teamId() <= 0) return;
        try {
            leaveGame.call(conn.teamId());
            log.info("Equipe {} desconectada — o jogo continua com {} equipas online",
                    teamName(conn.teamId()), registry.onlineTeams(conn.gameId()));
            registry.broadcastToGame(conn.gameId(),
                    "INFO|equipe " + teamName(conn.teamId()) + " desconectada — restantes continuam");
            broadcastWaiting(conn.gameId());
        } catch (RuntimeException e) {
            log.warn("Erro na limpeza da desconexão: {}", e.getMessage());
        }
    }

    private void broadcastWaiting(long gameId) {
        Game g = games.findById(gameId).orElse(null);
        if (g == null) return;
        long online = registry.onlineTeams(gameId);
        registry.broadcastToGame(gameId, "WAITING|" + online + "|" + g.getTotalRounds());
    }

    private String teamName(long teamId) {
        return teams.findById(teamId).map(Team::getName).orElse("?");
    }
}
