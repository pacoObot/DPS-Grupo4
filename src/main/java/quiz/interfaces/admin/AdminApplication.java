package quiz.interfaces.admin;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quiz.interfaces.common.HeaderBar;
import quiz.interfaces.common.TcpClient;
import quiz.interfaces.common.Theme;

/**
 * AdminApplication — ponto de entrada da interface de administrador (spec §9).
 *   ADMIN LOGIN -> Dashboard (Perguntas | Partida | Monitoramento)
 * Toda a comunicação é TCP (spec §10): a UI nunca acede à base de dados.
 */
public final class AdminApplication extends Application {

    private static final Logger log = LoggerFactory.getLogger(AdminApplication.class);

    private final TcpClient tcp = new TcpClient();
    private Stage stage;
    private LoginView loginView;
    private AdminDashboard dashboard;
    private BorderPane root;
    private HeaderBar header;
    private boolean loggedIn = false;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        Theme.apply(null); // regista estilos globais

        root = new BorderPane();
        loginView = new LoginView(this::connectAndLogin);
        header = new HeaderBar("Quiz Multiplayer Paralelo", "Consola do Administrador");
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
                Platform.runLater(() -> {
                    if (loggedIn) showDisconnected(reason);
                    else loginView.setStatus("Ligação perdida: " + reason, true);
                    loggedIn = false;
                });
            }
        });

        Scene scene = new Scene(root, 1200, 780);
        scene.getStylesheets().addAll(Theme.stylesheets());
        stage.setTitle("Quiz Multiplayer Paralelo — Admin");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> {
            if (dashboard != null) dashboard.stop();
            tcp.close();
        });
        stage.show();
    }

    private void connectAndLogin(String host, int port, String user, String password) {
        loginView.busy(true);
        loginView.setStatus("A ligar a " + host + ":" + port + "...", false);
        header.setStatus("A ligar...", "#f59e0b");
        new Thread(() -> {
            try {
                tcp.connect(host, port);
                tcp.send("ADMIN_LOGIN|" + user + "|" + password);
            } catch (Exception e) {
                Platform.runLater(() -> {
                    loginView.busy(false);
                    loginView.setStatus("Falha na ligação: " + e.getMessage(), true);
                    header.setStatus("Desligado", "#dc2626");
                });
            }
        }, "admin-connect").start();
    }

    private void dispatch(String line) {
        int sep = line.indexOf('|');
        String type = sep < 0 ? line : line.substring(0, sep);
        String[] f = sep < 0 ? new String[0] : line.substring(sep + 1).split("\\|", -1);

        switch (type) {
            case "LOGIN_OK" -> {
                if (loggedIn) return;
                loggedIn = true;
                showDashboard();
                log.info("Admin autenticado");
            }
            case "LOGIN_ERROR" -> {
                loginView.busy(false);
                loginView.setStatus(f.length > 0 ? f[0] : "Credenciais inválidas.", true);
                header.setStatus("Falha de login", "#dc2626");
            }
            case "ERROR" -> {
                String msg = f.length > 0 ? f[0] : "erro";
                if (loggedIn && dashboard != null) dashboard.dispatch(line);
                else loginView.setStatus(msg, true);
                if (f.length > 1 && f[1].equals("AUTH")) {
                    // sessão inválida — volta ao login
                    loggedIn = false;
                    showLogin("Sessão expirada.");
                }
            }
            default -> {
                if (loggedIn && dashboard != null) dashboard.dispatch(line);
            }
        }
    }

    private void showDashboard() {
        dashboard = new AdminDashboard(tcp);
        root.setCenter(dashboard);
        loginView = null;
        stage.setTitle("Quiz Multiplayer Paralelo — Admin");
        header.setStatus("Autenticado", "#16a34a");
        header.setSubtitle("Consola do Administrador");
        // pede o estado inicial
        tcp.send("LIST_QUESTIONS|||true");
        tcp.send("GET_MONITOR");
        tcp.send("GET_STATS");
    }

    private void showLogin(String reason) {
        if (dashboard != null) dashboard.stop();
        dashboard = null;
        loginView = new LoginView(this::connectAndLogin);
        loginView.setStatus(reason, true);
        root.setCenter(loginView);
    }

    private void showDisconnected(String reason) {
        if (dashboard != null) dashboard.stop();
        dashboard = null;
        loggedIn = false;
        showLogin("Desligado do servidor: " + reason);
        Alert a = new Alert(Alert.AlertType.WARNING,
                "A ligação ao servidor terminou:\n" + reason, ButtonType.OK);
        a.setHeaderText("Ligação perdida");
        a.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
