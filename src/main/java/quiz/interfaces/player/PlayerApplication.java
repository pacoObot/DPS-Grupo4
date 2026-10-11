package quiz.interfaces.player;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quiz.interfaces.common.HeaderBar;
import quiz.interfaces.common.TcpClient;
import quiz.interfaces.common.Theme;

/**
 * PlayerApplication — interface do jogador (spec §9).
 *
 *   PLAYER LOGIN (JOIN_GAME) -> Lobby -> Quiz -> Result
 *
 * Fluxo de mensagens recebidas do servidor:
 *   JOIN_OK / JOIN_ERROR / WAITING / QUESTION / ANSWER_RESULT /
 *   SCOREBOARD / NEXT_ROUND / GAME_OVER / ERROR
 */
public final class PlayerApplication extends Application {

    private static final Logger log = LoggerFactory.getLogger(PlayerApplication.class);

    private final TcpClient tcp = new TcpClient();
    private Stage stage;
    private BorderPane root;
    private HeaderBar header;
    private LoginView loginView;
    private LobbyView lobbyView;
    private QuizView quizView;
    private ResultView resultView;

    private String teamName = "";
    private String gameName = "";
    private boolean joined = false;
    private boolean gameOver = false;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        Theme.apply(null);

        loginView = new LoginView(this::join);
        lobbyView = new LobbyView();
        quizView = new QuizView(tcp);
        resultView = new ResultView();

        root = new BorderPane();
        header = new HeaderBar("Quiz Multiplayer Paralelo", "Cliente do Jogador");
        header.setStatus("Desligado", "#dc2626");
        root.setTop(header);
        root.setCenter(loginView);

        tcp.setListener(new TcpClient.Listener() {
            @Override
            public void onMessage(String line) {
                Platform.runLater(() -> dispatch(line));
            }

            @Override
            public void onClosed(String reason) {
                Platform.runLater(() -> showDisconnected(reason));
            }
        });

        Scene scene = new Scene(root, 1100, 760);
        scene.getStylesheets().addAll(Theme.stylesheets());
        stage.setTitle("Quiz Multiplayer Paralelo — Jogador");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> tcp.close());
        stage.show();
    }

    private void join(String host, int port, String code, String team) {
        if (code.isEmpty() || team.isEmpty()) {
            loginView.setStatus("Preencha o código da partida e o nome da equipa.", true);
            return;
        }
        this.teamName = team;
        loginView.busy(true);
        loginView.setStatus("A ligar a " + host + ":" + port + "...", false);
        header.setStatus("A ligar...", "#f59e0b");
        new Thread(() -> {
            try {
                tcp.connect(host, port);
                tcp.send("JOIN_GAME|" + code + "|" + team);
            } catch (Exception e) {
                Platform.runLater(() -> {
                    loginView.busy(false);
                    loginView.setStatus("Falha na ligação: " + e.getMessage(), true);
                    header.setStatus("Desligado", "#dc2626");
                });
            }
        }, "player-connect").start();
    }

    private void dispatch(String line) {
        int sep = line.indexOf('|');
        String type = sep < 0 ? line : line.substring(0, sep);
        String[] f = sep < 0 ? new String[0] : line.substring(sep + 1).split("\\|", -1);

        switch (type) {
            case "JOIN_OK" -> {
                joined = true;
                gameName = f.length > 2 ? f[2] : "";
                quizView.setOwnTeam(teamName);
                lobbyView.setJoined(f.length > 2 ? f[1] : "-", teamName, gameName);
                root.setCenter(lobbyView);
                stage.setTitle("Quiz Paralelo — " + teamName);
                header.setStatus("Ligado · " + teamName, "#16a34a");
                header.setSubtitle(gameName.isEmpty() ? "Sala de espera" : gameName);
                log.info("Entrou na partida: {}", gameName);
            }
            case "JOIN_ERROR" -> {
                loginView.busy(false);
                loginView.setStatus(f.length > 0 ? f[0] : "Não foi possível entrar.", true);
                header.setStatus("Desligado", "#dc2626");
            }
            case "WAITING" -> {
                if (f.length >= 2) lobbyView.setWaiting(parseInt(f[0], 0), parseInt(f[1], 0));
            }
            case "GAME_STATUS" -> {
                if (f.length >= 2) {
                    switch (f[1]) {
                        case "OPEN" -> { if (joined) root.setCenter(lobbyView); }
                        case "RUNNING" -> {
                            if (joined && !gameOver) {
                                quizView.setPaused(false);
                                root.setCenter(quizView);
                            }
                        }
                        case "PAUSED" -> {
                            if (joined && !gameOver) {
                                quizView.setPaused(true);
                                root.setCenter(quizView);
                            }
                        }
                        case "FINISHED" -> { /* GAME_OVER trata o fim */ }
                        default -> lobbyView.setStatus("Estado da partida: " + f[1]);
                    }
                }
            }
            case "QUESTION" -> {
                if (!gameOver) {
                    root.setCenter(quizView);
                    quizView.showQuestion(f);
                }
            }
            case "GAME_STARTED" -> {
                if (joined && !gameOver) root.setCenter(quizView);
            }
            case "GAME_PAUSED" -> {
                if (joined && !gameOver) {
                    quizView.setPaused(true);
                    root.setCenter(quizView);
                }
            }
            case "GAME_RESUMED" -> quizView.setPaused(false);
            case "GAME_STOPPED" -> {
                if (joined && !gameOver) quizView.setPaused(true);
            }
            case "ANSWER_RESULT" -> quizView.showResult(f);
            case "NEXT_ROUND" -> quizView.nextRound(f.length > 0 ? f[0] : "?");
            case "SCOREBOARD" -> {
                if (gameOver) resultView.showScoreboard(f);
                else quizView.showScoreboard(f);
            }
            case "GAME_OVER" -> {
                gameOver = true;
                resultView.setGameName(gameName.isEmpty() ? "partida encerrada" : gameName);
                root.setCenter(resultView);
            }
            case "ERROR" -> {
                String msg = f.length > 0 ? f[0] : "erro";
                if (!joined) loginView.setStatus(msg, true);
                else if (!gameOver) lobbyView.setStatus(msg);
            }
            case "PONG", "LEFT" -> { }
            default -> log.debug("mensagem ignorada: {}", type);
        }
    }

    private void showDisconnected(String reason) {
        joined = false;
        loginView.busy(false);
        loginView.setStatus("Ligação perdida: " + reason
                + (gameOver ? "" : " — clique em ENTRAR para voltar."), true);
        root.setCenter(loginView);
        header.setStatus("Desligado", "#dc2626");
        if (!gameOver) {
            Alert a = new Alert(Alert.AlertType.WARNING,
                    "A ligação ao servidor terminou:\n" + reason, ButtonType.OK);
            a.setHeaderText("Desligado");
            a.showAndWait();
        }
    }

    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
