package quiz.interfaces.player;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import quiz.interfaces.common.TcpClient;
import quiz.interfaces.common.Theme;
import quiz.interfaces.common.Ui;

/**
 * QuizView — ecrã de jogo do jogador (redesenhado).
 *
 *   QUESTION|round|total|qid|timeLimit|text|A|B|C|D   (broadcast)
 *   ANSWER|qid|X                                      (resposta)
 *   ANSWER_RESULT|qid|status|pts|bonus|total
 *   SCOREBOARD / NEXT_ROUND / PAUSED
 *
 *  Estados visuais: em jogo, a aguardar, pausado, tempo crítico.
 */
public final class QuizView extends VBox {

    private final TcpClient tcp;
    private final Label roundLabel = new Label("Ronda —");
    private final Label timerLabel = new Label("—");
    private final Label pointsLabel = new Label("0");
    private final Label textLabel = new Label();
    private final Label feedbackLabel = new Label(" ");
    private final Label pausedBanner = Ui.alert("⏸  Jogo em pausa — aguarde o administrador retomar.", "warn");
    private final ProgressBar progress = new ProgressBar(1);
    private final Button[] options = new Button[4];
    private final VBox scoreboardBox = new VBox(5);

    private long questionId = -1;
    private int timeLimit = 15;
    private int totalTime = 15;
    private Timeline countdown;
    private boolean answered;
    private boolean paused;

    public QuizView(TcpClient tcp) {
        this.tcp = tcp;
        setSpacing(14);
        setPadding(new Insets(18));

        roundLabel.getStyleClass().add("round-badge");
        timerLabel.getStyleClass().add("big-timer");
        pointsLabel.getStyleClass().add("big-timer");
        pointsLabel.setStyle("-fx-text-fill: " + Theme.GREEN + ";");

        Label ptsCaption = new Label("pontos");
        ptsCaption.getStyleClass().add("muted");

        VBox pointsBox = new VBox(0, pointsLabel, ptsCaption);
        pointsBox.setAlignment(Pos.CENTER_RIGHT);

        HBox top = new HBox(20, roundLabel, Ui.hSpacer(), timerLabel, pointsBox);
        top.setAlignment(Pos.CENTER_LEFT);

        pausedBanner.setVisible(false);
        pausedBanner.setManaged(false);

        textLabel.setWrapText(true);
        textLabel.getStyleClass().add("question-text");
        textLabel.setMaxWidth(Double.MAX_VALUE);
        textLabel.setMinHeight(70);

        progress.setPrefHeight(12);
        progress.setMaxWidth(Double.MAX_VALUE);

        feedbackLabel.getStyleClass().add("subtitle");
        feedbackLabel.setMinHeight(30);
        feedbackLabel.setWrapText(true);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        for (int i = 0; i < 4; i++) {
            final char opt = (char) ('A' + i);
            Button b = new Button();
            b.getStyleClass().add("option-btn");
            b.getStyleClass().add("opt-" + i);
            b.setMaxWidth(Double.MAX_VALUE);
            b.setMinHeight(60);
            b.setWrapText(true);
            b.setOnAction(e -> answer(opt));
            options[i] = b;
            grid.add(b, i % 2, i / 2);
            GridPane.setHgrow(b, Priority.ALWAYS);
        }

        Label boardTitle = Ui.section("Classificação ao vivo");
        scoreboardBox.getStyleClass().add("card-flat");

        getChildren().addAll(top, pausedBanner, textLabel, progress, feedbackLabel, grid,
                boardTitle, scoreboardBox);
        VBox.setVgrow(scoreboardBox, Priority.ALWAYS);
        showPlaceholder();
    }

    private void showPlaceholder() {
        textLabel.setText("A aguardar a próxima pergunta do servidor...");
        timerLabel.setText("—");
        for (Button b : options) {
            b.setText("");
            b.setDisable(true);
            b.getStyleClass().removeAll("answer-correct", "answer-wrong");
        }
        progress.setProgress(0);
    }

    // ------------------------------------------------------------ receção
    /** QUESTION|round|total|qid|timeLimit|text|A|B|C|D */
    public void showQuestion(String[] f) {
        if (f.length < 10) return;
        stopTimer();
        answered = false;
        setPaused(false);
        roundLabel.setText("Ronda " + f[0] + " / " + f[1]);
        questionId = Long.parseLong(f[2]);
        timeLimit = parse(f[3], 15);
        totalTime = timeLimit <= 0 ? 15 : timeLimit;
        textLabel.setText(f[4]);
        feedbackLabel.setText(" ");
        feedbackLabel.setStyle("");
        for (int i = 0; i < 4; i++) {
            options[i].setText((char) ('A' + i) + "     " + f[5 + i]);
            options[i].setDisable(false);
            options[i].getStyleClass().removeAll("answer-correct", "answer-wrong");
        }
        startTimer();
    }

    /** ANSWER_RESULT|qid|status|points|bonus|total */
    public void showResult(String[] f) {
        if (f.length < 6) return;
        answered = true;
        stopTimer();
        for (Button b : options) b.setDisable(true);

        String status = f[1];
        int points = parse(f[2], 0);
        int bonus = parse(f[3], 0);
        int total = parse(f[4], 0);
        pointsLabel.setText(String.valueOf(total));

        switch (status) {
            case "CORRECT" -> alertLine("✔  Correto!  +" + points + " pts (bónus de tempo: " + bonus + ")", "success");
            case "WRONG" -> alertLine("✘  Errada. Veja a classificação.", "error");
            case "TIMEOUT" -> alertLine("⏱  Tempo esgotado!", "warn");
            default -> {
                feedbackLabel.setText("Resposta registada: " + status);
                feedbackLabel.getStyleClass().setAll("subtitle");
            }
        }
    }

    private void alertLine(String text, String variant) {
        feedbackLabel.setText(text);
        feedbackLabel.getStyleClass().setAll("alert-banner", "alert-" + variant);
    }

    /** SCOREBOARD|pos:team:pts:ok:err:status|... */
    public void showScoreboard(String[] f) {
        scoreboardBox.getChildren().clear();
        Label head = label(String.format("%-4s %-22s %7s %5s %5s  %s",
                "#", "Equipa", "Pontos", "✓", "✗", "Estado"));
        head.setStyle("-fx-text-fill: " + Theme.ACCENT + "; -fx-font-weight: bold;");
        scoreboardBox.getChildren().add(head);
        for (int i = 1; i < f.length; i++) {
            String[] p = f[i].split(":");
            if (p.length >= 6) {
                Label row = label(String.format("%-4s %-22s %7s %5s %5s  %s",
                        p[0], p[1], p[2], p[3], p[4], p[5]));
                if (p[1].equals(ownTeam)) {
                    row.setStyle("-fx-font-weight: bold; -fx-text-fill: " + Theme.ACCENT
                            + "; -fx-font-family: 'Consolas','DejaVu Sans Mono',monospace;");
                }
                scoreboardBox.getChildren().add(row);
            }
        }
    }

    private String ownTeam = "";

    /** Permite destacar a própria equipa na classificação. */
    public void setOwnTeam(String team) {
        this.ownTeam = team == null ? "" : team;
    }

    public void nextRound(String round) {
        stopTimer();
        setPaused(false);
        textLabel.setText("Ronda " + round + " a começar...");
        for (Button b : options) b.setDisable(true);
        timerLabel.setText("—");
        progress.setProgress(0);
    }

    public void addPoints(int total) {
        pointsLabel.setText(String.valueOf(total));
    }

    public void reset() {
        stopTimer();
        pointsLabel.setText("0");
        scoreboardBox.getChildren().clear();
        showPlaceholder();
    }

    /** Estado de pausa (GAME_STATUS PAUSED). */
    public void setPaused(boolean value) {
        this.paused = value;
        pausedBanner.setVisible(value);
        pausedBanner.setManaged(value);
        if (value) {
            stopTimer();
            for (Button b : options) b.setDisable(true);
        }
    }

    public boolean isPaused() {
        return paused;
    }

    // ------------------------------------------------------------ envio
    private void answer(char option) {
        if (answered || paused || questionId < 0) return;
        answered = true;
        stopTimer();
        tcp.send("ANSWER|" + questionId + "|" + option);
        for (Button b : options) b.setDisable(true);
        options[option - 'A'].getStyleClass().add("answer-correct");
        feedbackLabel.setText("Resposta enviada (" + option + ") — a aguardar as outras equipas...");
        feedbackLabel.getStyleClass().setAll("subtitle");
    }

    private void startTimer() {
        timerLabel.setText(timeLimit + "s");
        timerLabel.getStyleClass().remove("critical");
        progress.setProgress(1.0);
        countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            timeLimit--;
            int remaining = Math.max(0, timeLimit);
            timerLabel.setText(remaining + "s");
            progress.setProgress(remaining / (double) totalTime);
            if (timeLimit <= 5) {
                if (!timerLabel.getStyleClass().contains("critical")) timerLabel.getStyleClass().add("critical");
            }
            if (timeLimit <= 0) {
                stopTimer();
                for (Button b : options) b.setDisable(true);
                alertLine("⏱  Tempo esgotado!", "warn");
            }
        }));
        countdown.setCycleCount(Timeline.INDEFINITE);
        countdown.play();
    }

    private void stopTimer() {
        if (countdown != null) {
            countdown.stop();
            countdown = null;
        }
        timerLabel.getStyleClass().remove("critical");
    }

    private static int parse(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static Label label(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("mono");
        l.setStyle("-fx-text-fill: " + Theme.TEXT + ";");
        return l;
    }
}