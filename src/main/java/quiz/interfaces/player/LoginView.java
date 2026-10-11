package quiz.interfaces.player;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import quiz.interfaces.common.Theme;
import quiz.interfaces.common.Ui;

/**
 * Player LoginView — entrada moderna (JOIN_GAME|codigo|NomeEquipa).
 *
 *  Layout em dois painéis: identidade da aplicação à esquerda e formulário
 *  à direita. Valida código, nome (comprimento/estado) e endereço do
 *  servidor, com feedback claro junto aos campos.
 */
public final class LoginView extends VBox {

    public interface OnJoin {
        void request(String host, int port, String code, String team);
    }

    private final TextField hostField = new TextField("127.0.0.1");
    private final TextField portField = new TextField("5050");
    private final TextField codeField = new TextField();
    private final TextField teamField = new TextField();
    private final Label status = Ui.subtitle(
            "Introduza o código da partida e o nome da sua equipa.");
    private final Button joinBtn = Ui.button("ENTRAR NA PARTIDA", "success");

    public LoginView(OnJoin action) {
        setAlignment(Pos.CENTER);
        setPadding(new Insets(34));
        setSpacing(0);

        // ---------------- painel de marca ----------------
        Label logo = new Label("QUIZ");
        logo.setStyle("-fx-font-size: 46px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label logo2 = new Label("MULTIPLAYER PARALELO");
        logo2.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #e0f2fe;");
        Label blurb = new Label("Compete em tempo real.\n"
                + "Cada equipa numa thread — o servidor é a autoridade do tempo.");
        blurb.setStyle("-fx-text-fill: #dbeafe; -fx-font-size: 13px;");
        blurb.setWrapText(true);
        blurb.setMaxWidth(260);

        VBox brand = new VBox(10, logo, logo2, blurb);
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.setPadding(new Insets(34));
        brand.setMinWidth(300);
        brand.setStyle("-fx-background-color: linear-gradient(to bottom, #0ea5e9, #6366f1);"
                + "-fx-background-radius: 14 0 0 14;");

        // ---------------- formulário ----------------
        codeField.setPromptText("CÓDIGO (ex.: AB12)");
        codeField.getStyleClass().add("field");
        codeField.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        teamField.setPromptText("Nome da equipa (ex.: Alpha)");
        teamField.getStyleClass().add("field");

        hostField.getStyleClass().add("field");
        hostField.setPrefWidth(190);
        portField.getStyleClass().add("field");
        portField.setPrefWidth(90);

        HBox serverRow = new HBox(10, hostField, portField);

        joinBtn.setMaxWidth(Double.MAX_VALUE);
        joinBtn.setDefaultButton(true);
        joinBtn.setOnAction(e -> submit(action));

        // Enter no campo de equipa entra diretamente.
        teamField.setOnAction(e -> submit(action));

        region(portField, 120);
        region(hostField, 220);

        VBox form = new VBox(12,
                Ui.title("Entrar numa partida"),
                Ui.subtitle("Peça o código ao administrador e escolha um nome de equipa único."),
                Ui.field("Código da partida", codeField),
                Ui.field("Nome da equipa", teamField),
                Ui.field("Servidor (endereço · porta)", serverRow),
                joinBtn, status);
        form.setPadding(new Insets(30, 34, 30, 34));
        form.setMinWidth(400);
        form.setAlignment(Pos.CENTER_LEFT);
        form.setStyle("-fx-background-color: " + Theme.PANEL + "; -fx-background-radius: 0 14 14 0;");

        HBox card = new HBox(brand, form);
        card.getStyleClass().add("card");
        card.setMaxWidth(760);
        card.setAlignment(Pos.CENTER);
        card.setPadding(Insets.EMPTY);
        getChildren().add(card);
    }

    private static void region(Region r, double width) {
        r.setMaxWidth(width);
        r.setMinWidth(width);
    }

    private void submit(OnJoin action) {
        String code = codeField.getText() == null ? "" : codeField.getText().trim().toUpperCase();
        String team = teamField.getText() == null ? "" : teamField.getText().trim();
        markError(codeField, false);
        markError(teamField, false);

        if (code.isEmpty()) { setStatus("Introduza o código da partida.", true); markError(codeField, true); return; }
        if (code.length() > 12) { setStatus("Código inválido.", true); markError(codeField, true); return; }
        if (team.isEmpty()) { setStatus("Escolha um nome de equipa.", true); markError(teamField, true); return; }
        if (team.length() > 40) { setStatus("Nome de equipa demasiado longo (máx. 40).", true); markError(teamField, true); return; }
        if (!team.matches("[\\p{L}\\p{N} _.-]+")) {
            setStatus("O nome só pode conter letras, números, espaços, _ . -", true);
            markError(teamField, true);
            return;
        }
        int port = parseInt(portField.getText(), 5050);
        if (port < 1 || port > 65535) { setStatus("Porta do servidor inválida.", true); return; }

        action.request(hostField.getText().trim(), port, code, team);
    }

    private static void markError(TextField f, boolean error) {
        if (error) {
            if (!f.getStyleClass().contains("field-danger")) f.getStyleClass().add("field-danger");
        } else {
            f.getStyleClass().remove("field-danger");
        }
    }

    public void setStatus(String text, boolean error) {
        status.setText(text);
        status.getStyleClass().setAll("alert-banner", error ? "alert-error" : "alert-info");
        joinBtn.setDisable(false);
    }

    public void busy(boolean busy) {
        joinBtn.setDisable(busy);
        joinBtn.setText(busy ? "A LIGAR..." : "ENTRAR NA PARTIDA");
    }

    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }
}