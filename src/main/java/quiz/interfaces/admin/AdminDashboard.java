package quiz.interfaces.admin;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import quiz.interfaces.common.TcpClient;
import quiz.interfaces.common.Theme;
import quiz.interfaces.common.Ui;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AdminDashboard — "shell" da consola administrativa.
 *
 *  Layout profissional: navegação lateral (sidebar) + área de conteúdo.
 *  Secções: Dashboard | Perguntas | Partida | Monitorização.
 *
 *  Reencaminha as mensagens do protocolo para a secção certa; o Dashboard
 *  recebe ainda os eventos (TEAM_EVENT/ROUND_EVENT/GAME_STATUS) para os
 *  apresentar em tempo real.
 */
public final class AdminDashboard extends BorderPane {

    private final Map<String, Button> navButtons = new LinkedHashMap<>();
    private final Map<String, Node> sections = new LinkedHashMap<>();
    private final StackPane content = new StackPane();
    private final Label status = new Label("Pronto.");

    private final DashboardView dashboardView;
    private final QuestionManagementView questionView;
    private final GameManagementView gameView;
    private final MonitoringView monitoringView;

    public AdminDashboard(TcpClient tcp) {
        this.dashboardView = new DashboardView(tcp);
        this.questionView = new QuestionManagementView(tcp);
        this.gameView = new GameManagementView(tcp);
        this.monitoringView = new MonitoringView(tcp);

        register("dashboard", "📊  Dashboard", dashboardView);
        register("questions", "📚  Perguntas", questionView);
        register("game", "🎮  Partida", gameView);
        register("monitor", "🧵  Monitorização", monitoringView);

        VBox sidebar = buildSidebar();
        content.getChildren().addAll(sections.values());
        content.setPadding(new Insets(0));

        HBox center = new HBox(sidebar, content);
        HBox.setHgrow(content, Priority.ALWAYS);
        setCenter(center);

        status.getStyleClass().add("subtitle");
        HBox bottom = new HBox(status);
        bottom.setPadding(new Insets(6, 14, 6, 14));
        bottom.setAlignment(Pos.CENTER_LEFT);
        bottom.setStyle("-fx-border-color: " + Theme.LINE + " transparent transparent transparent;");
        setBottom(bottom);

        show("dashboard");
    }

    private void register(String key, String label, Node view) {
        Button b = Ui.navItem(label, () -> show(key));
        navButtons.put(key, b);
        sections.put(key, view);
    }

    private VBox buildSidebar() {
        VBox sb = new VBox();
        sb.getStyleClass().add("sidebar");

        Label brand = new Label("QUIZ PARALELO");
        brand.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: " + Theme.ACCENT + ";");
        Label brandSub = new Label("Consola de administração");
        brandSub.getStyleClass().add("muted");
        brandSub.setStyle("-fx-font-size: 11px;");
        VBox brandBox = new VBox(2, brand, brandSub);
        brandBox.setPadding(new Insets(4, 10, 12, 10));

        Label caption = new Label("GESTÃO");
        caption.getStyleClass().add("nav-caption");

        sb.getChildren().add(brandBox);
        sb.getChildren().add(caption);
        sb.getChildren().addAll(navButtons.values());
        return sb;
    }

    /** Mostra uma secção e marca o item correspondente como selecionado. */
    public void show(String key) {
        Node view = sections.get(key);
        if (view == null) return;
        for (Node n : content.getChildren()) {
            boolean active = n == view;
            n.setVisible(active);
            n.setManaged(active);
        }
        navButtons.forEach((k, b) -> {
            boolean sel = k.equals(key);
            if (sel) {
                if (!b.getStyleClass().contains("selected")) b.getStyleClass().add("selected");
            } else {
                b.getStyleClass().remove("selected");
            }
        });
        if (key.equals("dashboard")) dashboardView.refresh();
    }

    /** Roteia uma mensagem do servidor para as secções interessadas. */
    public void dispatch(String line) {
        int sep = line.indexOf('|');
        String type = sep < 0 ? line : line.substring(0, sep);
        String[] f = sep < 0 ? new String[0] : line.substring(sep + 1).split("\\|", -1);

        status.setText("◀ " + type);

        switch (type) {
            case "QUESTION_ROW", "QUESTION_CREATED", "QUESTION_UPDATED", "QUESTION_DELETED", "LIST_END" ->
                    questionView.dispatch(type, f);
            case "GAME_CREATED", "GAME_STATUS", "TEAMS", "SCOREBOARD", "TEAM_EVENT", "ROUND_EVENT" -> {
                gameView.dispatch(type, f);
                dashboardView.dispatch(type, f);
            }
            case "MONITOR", "POOL", "THREAD_NAMES" -> monitoringView.dispatch(type, f);
            case "STATS" -> dashboardView.dispatch(type, f);
            default -> { }
        }
    }

    public void stop() {
        monitoringView.stop();
        dashboardView.stop();
    }
}