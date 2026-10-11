package quiz.interfaces.admin;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import quiz.interfaces.common.TcpClient;
import quiz.interfaces.common.Theme;
import quiz.interfaces.common.Ui;
import quiz.shared.dto.ServerStats;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * =====================================================================
 *  DashboardView — painel de controlo do administrador
 * =====================================================================
 *
 *  Todos os indicadores vêm de GET_STATS (dados REAIS do PostgreSQL e do
 *  registry de ligações). Faz polling a cada 3s — leve, sem bloquear a
 *  JavaFX Application Thread (o envio é assíncrono no TcpClient).
 *
 *  Também acumula EVENTOS recentes (respostas/rondas) num ListView
 *  limitado, para não crescer indefinidamente em memória.
 */
public final class DashboardView extends VBox {

    private static final DateTimeFormatter HHMMSS = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int MAX_EVENTS = 200;

    private final TcpClient tcp;
    private final Timeline poller;

    // KPIs
    private final Ui.StatCard kpiThreads = new Ui.StatCard("⚙", "Threads ativas", Theme.ACCENT);
    private final Ui.StatCard kpiConns = new Ui.StatCard("🔌", "Ligações TCP", Theme.ACCENT);
    private final Ui.StatCard kpiPool = new Ui.StatCard("🧵", "Tamanho do pool", Theme.VIOLET);
    private final Ui.StatCard kpiQTotal = new Ui.StatCard("📚", "Perguntas", Theme.ACCENT);
    private final Ui.StatCard kpiQActive = new Ui.StatCard("✅", "Perguntas ativas", Theme.GREEN);
    private final Ui.StatCard kpiCats = new Ui.StatCard("🏷", "Categorias", Theme.VIOLET);
    private final Ui.StatCard kpiRound = new Ui.StatCard("🎯", "Ronda", Theme.AMBER);
    private final Ui.StatCard kpiTeams = new Ui.StatCard("👥", "Equipas (online/total)", Theme.ACCENT);
    private final Ui.StatCard kpiAnswers = new Ui.StatCard("📝", "Respostas recebidas", Theme.GREEN);
    private final Ui.StatCard kpiTimeouts = new Ui.StatCard("⏱", "Timeouts", Theme.AMBER);

    private final Label gameName = new Label("—");
    private final Label gameBadgeHolder = new Label();
    private final Label gameInfo = new Label("Sem partida registada.");
    private final VBox topBox = new VBox(8);
    private final ObservableList<String> events = FXCollections.observableArrayList();
    private final ListView<String> eventList = new ListView<>(events);

    public DashboardView(TcpClient tcp) {
        this.tcp = tcp;
        setSpacing(16);
        setPadding(new Insets(18));

        Label title = Ui.title("Painel de controlo");
        Label sub = Ui.subtitle("Indicadores em tempo real do servidor, das perguntas e da partida mais recente.");

        GridPane kpis = new GridPane();
        kpis.getStyleClass().add("kpi-grid");
        kpis.addRow(0, kpiThreads, kpiConns, kpiPool);
        kpis.addRow(1, kpiQTotal, kpiQActive, kpiCats);
        kpis.addRow(2, kpiRound, kpiTeams, kpiAnswers);
        for (javafx.scene.Node n : kpis.getChildren()) {
            GridPane.setHgrow(n, Priority.ALWAYS);
            if (n instanceof Ui.StatCard sc) sc.setMaxWidth(Double.MAX_VALUE);
        }

        // ---- partida atual + top 3 ----
        gameName.getStyleClass().add("card-title");
        HBox gameHead = new HBox(10, gameName, gameBadgeHolder, Ui.hSpacer());
        gameHead.setAlignment(Pos.CENTER_LEFT);
        gameInfo.getStyleClass().add("subtitle");
        topBox.setSpacing(8);

        VBox gameCard = Ui.card(Ui.section("Partida atual"), gameHead, gameInfo,
                new Label(" "), Ui.cardTitle("Top 3"), topBox);

        // ---- eventos recentes ----
        eventList.setPrefHeight(360);
        eventList.getStyleClass().add("mono");
        eventList.setPlaceholder(new Label("Sem eventos ainda."));
        VBox eventsCard = Ui.card(Ui.section("Eventos recentes"), eventList);
        VBox.setVgrow(eventList, Priority.ALWAYS);

        HBox panels = new HBox(16, gameCard, eventsCard);
        HBox.setHgrow(gameCard, Priority.ALWAYS);
        HBox.setHgrow(eventsCard, Priority.ALWAYS);
        gameCard.setMaxWidth(Double.MAX_VALUE);
        eventsCard.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(panels, Priority.ALWAYS);

        getChildren().addAll(title, sub, kpis, panels);

        poller = new Timeline(new KeyFrame(Duration.seconds(3), e -> refresh()));
        poller.setCycleCount(Timeline.INDEFINITE);
        poller.play();
        refresh();
    }

    public void refresh() {
        tcp.send("GET_STATS");
    }

    public void stop() {
        poller.stop();
    }

    // ------------------------------------------------------------ dispatch
    public void dispatch(String type, String[] f) {
        switch (type) {
            case "STATS" -> applyStats(ServerStats.fromProtocol(f));
            case "TEAM_EVENT" -> {
                if (f.length >= 3) event("resposta", f[0] + " → " + f[1] + "  (" + f[2] + ")");
            }
            case "ROUND_EVENT" -> {
                if (f.length >= 3) {
                    boolean start = "START".equals(f[1]);
                    event("ronda", (start ? "início da ronda " : "fim da ronda ") + f[2]
                            + (f.length > 3 ? "  · " + f[3] + " equipas" : ""));
                }
            }
            case "GAME_STATUS" -> {
                if (f.length >= 2) event("partida", "estado → " + f[1]);
            }
            default -> { }
        }
    }

    private void applyStats(ServerStats s) {
        kpiThreads.setValue(String.valueOf(s.activeThreads()));
        kpiConns.setValue(String.valueOf(s.connections()));
        kpiPool.setValue(String.valueOf(s.poolSize()));
        kpiQTotal.setValue(String.valueOf(s.questionsTotal()));
        kpiQActive.setValue(String.valueOf(s.questionsActive()));
        kpiCats.setValue(String.valueOf(s.categories()));
        kpiTeams.setValue(s.teamsOnline() + " / " + s.teamsJoined());
        kpiAnswers.setValue(String.valueOf(s.answers()));
        kpiTimeouts.setValue(String.valueOf(s.timeouts()));

        if (s.gameId() < 0) {
            kpiRound.setValue("—");
            gameName.setText("—");
            gameBadgeHolder.setText("");
            gameInfo.setText("Sem partida registada.");
            topBox.getChildren().clear();
            return;
        }

        kpiRound.setValue(s.round() + "/" + s.rounds());
        gameName.setText("#" + s.gameId() + " · " + s.gameName());
        gameBadgeHolder.setText(s.gameStatus());
        gameBadgeHolder.getStyleClass().setAll("badge", "badge-" + Ui.variantForStatus(s.gameStatus()));
        gameInfo.setText("Estado: " + s.gameStatus()
                + "   ·   " + s.teamsOnline() + " online de " + s.teamsJoined() + " inscritas"
                + "   ·   " + s.answers() + " respostas   ·   " + s.timeouts() + " timeouts");

        topBox.getChildren().clear();
        if (s.top().isEmpty()) {
            topBox.getChildren().add(Ui.subtitle("Sem classificação ainda."));
        } else {
            for (ServerStats.TopEntry t : s.top()) {
                String medal = switch (t.position()) {
                    case 1 -> "🥇";
                    case 2 -> "🥈";
                    case 3 -> "🥉";
                    default -> "•";
                };
                Label row = new Label(medal + "  " + t.team() + "  ·  " + t.points() + " pts");
                row.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                topBox.getChildren().add(row);
            }
        }
    }

    private void event(String kind, String text) {
        events.add(0, HHMMSS.format(LocalTime.now()) + "  [" + kind + "]  " + text);
        if (events.size() > MAX_EVENTS) events.remove(MAX_EVENTS, events.size());
    }
}