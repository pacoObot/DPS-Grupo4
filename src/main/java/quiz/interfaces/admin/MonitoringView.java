package quiz.interfaces.admin;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import quiz.interfaces.common.TcpClient;
import quiz.interfaces.common.Theme;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * MonitoringView — MONITORAMENTO CONCORRENTE (spec §26).
 *
 *   Thread                Equipe    Estado
 *   quiz-pool-1-1         Alpha     ANSWERED
 *   quiz-pool-1-2         Beta      ANSWERING
 *
 * Faz polling de GET_MONITOR a cada 1,5s — a prova visual do paralelismo
 * durante a apresentação.
 */
public final class MonitoringView extends VBox {

    private record Row(String thread, String team, String state) {}

    private final TcpClient tcp;
    private final ObservableList<Row> rows = FXCollections.observableArrayList();
    private final TableView<Row> table = new TableView<>(rows);
    private final Label connLabel = label("Conexões: 0");
    private final Label threadLabel = label("Threads ativas: 0");
    private final Label poolLabel = label("Pool: -");
    private final TextArea eventLog = new TextArea();
    private final Timeline poller;

    public MonitoringView(TcpClient tcp) {
        this.tcp = tcp;
        setSpacing(10);
        setPadding(new Insets(14));

        Label title = new Label("MONITORAMENTO CONCORRENTE");
        title.getStyleClass().add("title");

        HBox stats = new HBox(18, connLabel, threadLabel, poolLabel);
        stats.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        buildTable();

        eventLog.setEditable(false);
        eventLog.setWrapText(false);
        eventLog.getStyleClass().add("log");
        eventLog.setPrefHeight(260);
        eventLog.setPromptText("eventos de respostas/rondas aparecem aqui...");

        Label logTitle = label("EVENTOS (respostas simultâneas com thread e tempo)");
        getChildren().addAll(title, stats, table, logTitle, eventLog);
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(eventLog, Priority.ALWAYS);

        // polling automático — demonstração ao vivo
        poller = new Timeline(new KeyFrame(Duration.millis(1500), e -> tcp.send("GET_MONITOR")));
        poller.setCycleCount(Timeline.INDEFINITE);
        poller.play();
    }

    @SuppressWarnings("unchecked")
    private void buildTable() {
        table.setPrefHeight(240);
        table.getStyleClass().add("table-view");

        TableColumn<Row, String> thCol = new TableColumn<>("Thread (pool)");
        thCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().thread()));
        thCol.setPrefWidth(240);
        TableColumn<Row, String> teamCol = new TableColumn<>("Equipe");
        teamCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().team()));
        teamCol.setPrefWidth(220);
        TableColumn<Row, String> stCol = new TableColumn<>("Estado");
        stCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().state()));
        stCol.setPrefWidth(160);
        table.getColumns().setAll(List.of(thCol, teamCol, stCol));
    }

    // ----------------------------------------------------------- dispatch
    public void dispatch(String type, String[] f) {
        switch (type) {
            case "MONITOR" -> {
                List<Row> parsed = new ArrayList<>();
                for (int i = 1; i < f.length; i++) {
                    String[] p = f[i].split(":");
                    if (p.length >= 3) parsed.add(new Row(p[0], p[1], p[2]));
                }
                parsed.sort(Comparator.comparing(Row::team));
                rows.setAll(parsed);
            }
            case "POOL" -> poolLabel.setText("Pool: " + String.join(" ", java.util.Arrays.copyOfRange(f, 1, Math.max(1, f.length))));
            case "THREAD_NAMES" -> threadLabel.setText("Threads: " + (f.length > 1 ? f[1] : "-"));
            case "TEAM_EVENT" -> {
                if (f.length >= 3) {
                    append("[" + f[2] + "] " + f[0] + " -> " + f[1]);
                    connLabel.setText("Conexões: " + Math.max(0, rows.size()));
                }
            }
            case "ROUND_EVENT" -> {
                if (f.length >= 3) {
                    append(">>> Ronda " + f[2] + " " + (f[1].equals("START") ? "INICIADA" : "CONCLUÍDA")
                            + " | " + (f[1].equals("START") ? f[3] + " equipas" : "scoreboard enviado"));
                }
            }
            case "GAME_STATUS" -> append(">>> Partida " + (f.length > 1 ? f[1] : ""));
            default -> { }
        }
    }

    private void append(String line) {
        String next = eventLog.getText() + line + "\n";
        // mantém as últimas ~200 linhas
        if (next.length() > 20_000) next = next.substring(next.length() - 15_000);
        eventLog.setText(next);
        eventLog.positionCaret(next.length());
    }

    public void stop() {
        poller.stop();
    }

    private static Label label(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("subtitle");
        l.setStyle("-fx-text-fill: " + Theme.ACCENT + "; -fx-font-weight: bold;");
        return l;
    }
}
