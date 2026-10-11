package quiz.interfaces.admin;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import quiz.interfaces.common.Ui;

/**
 * LoginView (admin) — ligação TCP + autenticação, com identidade visual
 * consistente com o resto da consola.
 */
public final class LoginView extends VBox {

    public interface OnLogin {
        void request(String host, int port, String user, String password);
    }

    private final TextField hostField = new TextField("127.0.0.1");
    private final TextField portField = new TextField("5050");
    private final TextField userField = new TextField("admin");
    private final PasswordField passField = new PasswordField();
    private final Label status = Ui.alert("Introduza as credenciais do administrador.", "info");
    private final Button loginBtn = Ui.button("ENTRAR", "primary");

    public LoginView(OnLogin action) {
        setAlignment(Pos.CENTER);
        setSpacing(14);
        setPadding(new Insets(34));

        Label title = Ui.title("Admin Quiz Paralelo");
        Label sub = Ui.subtitle("Consola de administração via TCP Socket.");

        hostField.setPromptText("IP do servidor");
        portField.setPromptText("Porta");
        userField.setPromptText("Utilizador");
        passField.setPromptText("Palavra-passe");
        for (TextField f : new TextField[]{hostField, portField, userField}) f.getStyleClass().add("field");
        passField.getStyleClass().add("field");
        hostField.setPrefWidth(200);
        portField.setPrefWidth(100);

        HBox hostRow = new HBox(10, hostField, portField);

        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setDefaultButton(true);
        passField.setOnAction(e -> submit(action));
        loginBtn.setOnAction(e -> submit(action));

        status.setWrapText(true);
        status.setMaxWidth(Double.MAX_VALUE);

        VBox card = Ui.card(title, sub,
                Ui.field("Servidor (endereço · porta)", hostRow),
                Ui.field("Utilizador", userField),
                Ui.field("Palavra-passe", passField),
                loginBtn, status);
        card.setMaxWidth(440);
        getChildren().add(card);
    }

    private void submit(OnLogin action) {
        String host = hostField.getText() == null ? "" : hostField.getText().trim();
        if (host.isEmpty()) { setStatus("Indique o endereço do servidor.", true); return; }
        int port = parseInt(portField.getText(), 5050);
        if (port < 1 || port > 65535) { setStatus("Porta inválida.", true); return; }
        action.request(host, port, userField.getText().trim(), passField.getText());
    }

    public void setStatus(String text, boolean error) {
        status.setText(text);
        status.getStyleClass().setAll("alert-banner", error ? "alert-error" : "alert-info");
        loginBtn.setDisable(false);
    }

    public void busy(boolean busy) {
        loginBtn.setDisable(busy);
        loginBtn.setText(busy ? "A LIGAR..." : "ENTRAR");
    }

    public String host() { return hostField.getText().trim(); }
    public int port() { return parseInt(portField.getText(), 5050); }

    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }
}