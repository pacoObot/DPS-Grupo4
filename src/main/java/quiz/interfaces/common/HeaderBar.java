package quiz.interfaces.common;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * HeaderBar — cabeçalho comum das interfaces (admin e jogador).
 * Faixa com gradiente, título/subtítulo à esquerda e um "chip" de estado à direita.
 * Mantém o visual consistente entre os dois clientes.
 */
public final class HeaderBar extends HBox {

    private final Label title = new Label();
    private final Label subtitle = new Label();
    private final Label status = new Label("—");

    public HeaderBar(String titleText, String subtitleText) {
        getStyleClass().add("app-header");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(14);

        title.setText(titleText);
        title.getStyleClass().add("header-title");
        subtitle.setText(subtitleText);
        subtitle.getStyleClass().add("header-sub");

        VBox titles = new VBox(2, title, subtitle);
        titles.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        status.getStyleClass().add("chip");

        getChildren().addAll(titles, spacer, status);
    }

    public void setTitle(String text) { title.setText(text); }

    public void setSubtitle(String text) { subtitle.setText(text); }

    public void setStatus(String text) { status.setText(text); }

    /** Destaca o chip com uma cor (ex.: verde = ligado, vermelho = erro). */
    public void setStatus(String text, String hexColor) {
        status.setText(text);
        status.setStyle("-fx-background-color: " + hexColor + "; -fx-text-fill: white;");
    }
}