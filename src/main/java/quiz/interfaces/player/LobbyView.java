package quiz.interfaces.player;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import quiz.interfaces.common.Ui;

/**
 * LobbyView — sala de espera após JOIN_OK.
 * Recebe WAITING|nOnline|nTotal e (para o admin) GAME_STATUS.
 */
public final class LobbyView extends VBox {

    private final Label codeLabel = new Label("—");
    private final Label teamLabel = new Label("—");
    private final Label waitingLabel = new Label("A aguardar...");
    private final Label statusLabel = Ui.alert("O administrador ainda não iniciou a partida.", "info");

    public LobbyView() {
        setAlignment(Pos.CENTER);
        setSpacing(14);
        setPadding(new Insets(40));

        Label title = Ui.title("Sala de espera");

        codeLabel.setStyle("-fx-font-size: 34px; -fx-font-weight: bold; -fx-text-fill: "
                + quiz.interfaces.common.Theme.ACCENT + "; -fx-font-family: 'Consolas','DejaVu Sans Mono',monospace;");
        teamLabel.getStyleClass().add("subtitle");
        waitingLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: "
                + quiz.interfaces.common.Theme.GREEN + ";");

        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(480);
        statusLabel.setAlignment(Pos.CENTER);

        Label hint = Ui.subtitle("A pergunta aparece automaticamente quando a ronda começar.");

        HBox dot = new HBox(new Label("●"));
        dot.setAlignment(Pos.CENTER);

        VBox card = Ui.card(title, codeLabel, teamLabel, waitingLabel, statusLabel, hint);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(560);
        getChildren().add(card);
    }

    public void setJoined(String code, String teamName, String gameName) {
        codeLabel.setText(code);
        teamLabel.setText("Equipa " + teamName + "   ·   Partida " + gameName);
    }

    public void setWaiting(int online, int total) {
        waitingLabel.setText(online + " de " + total + " equipas ligadas");
    }

    public void setStatus(String text) {
        statusLabel.setText(text);
    }
}