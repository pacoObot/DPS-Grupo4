package quiz.interfaces.common;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * =====================================================================
 *  Ui — pequeno conjunto de componentes reutilizáveis (UI kit)
 * =====================================================================
 *
 *  Evita repetir estilos em cada ecrã: cada método devolve um Node já
 *  com as classes CSS do {@link Theme}. Sem abstrações desnecessárias —
 *  apenas os padrões que aparecem várias vezes (títulos, cartões,
 *  cartões de estatística, badges, alertas, botões e itens de navegação).
 */
public final class Ui {

    private Ui() {}

    // ------------------------------------------------------------ texto
    public static Label section(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("section");
        return l;
    }

    public static Label title(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("title");
        return l;
    }

    public static Label subtitle(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("subtitle");
        l.setWrapText(true);
        return l;
    }

    public static Label cardTitle(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("card-title");
        return l;
    }

    // ------------------------------------------------------------ layout
    public static VBox card(Node... children) {
        VBox box = new VBox(12, children);
        box.getStyleClass().add("card");
        return box;
    }

    public static VBox cardFlat(Node... children) {
        VBox box = new VBox(10, children);
        box.getStyleClass().add("card-flat");
        return box;
    }

    public static Region hSpacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    public static Region vSpacer() {
        Region r = new Region();
        VBox.setVgrow(r, Priority.ALWAYS);
        return r;
    }

    public static HBox row(Node... children) {
        HBox h = new HBox(10, children);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    // ------------------------------------------------------------ botões
    public static Button button(String text, String variant) {
        Button b = new Button(text);
        b.getStyleClass().add("btn-" + variant);
        return b;
    }

    public static Button navItem(String text, Runnable action) {
        Button b = new Button(text);
        b.getStyleClass().add("nav-item");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(e -> action.run());
        return b;
    }

    // ------------------------------------------------------------ badges
    /** variant: slate|success|danger|warn|info|violet */
    public static Label badge(String text, String variant) {
        Label l = new Label(text);
        l.getStyleClass().addAll("badge", "badge-" + variant);
        return l;
    }

    public static String variantForStatus(String status) {
        if (status == null) return "slate";
        return switch (status.toUpperCase()) {
            case "OPEN", "WAITING", "CONNECTED" -> "info";
            case "RUNNING", "ANSWERING" -> "success";
            case "PAUSED", "TIMEOUT" -> "warn";
            case "FINISHED", "STOPPED", "OFFLINE", "WRONG" -> "danger";
            case "CORRECT", "ANSWERED" -> "success";
            default -> "slate";
        };
    }

    // ------------------------------------------------------------ alertas
    /** variant: info|success|error|warn */
    public static Label alert(String text, String variant) {
        Label l = new Label(text);
        l.getStyleClass().addAll("alert-banner", "alert-" + variant);
        l.setWrapText(true);
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
    }

    // ------------------------------------------------------------ campos
    public static VBox field(String labelText, Node control) {
        Label l = new Label(labelText);
        l.getStyleClass().add("muted");
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
        if (control instanceof Region r) r.setMaxWidth(Double.MAX_VALUE);
        return new VBox(4, l, control);
    }

    /*
        Cartão de estatística (KPI). O valor pode ser atualizado em runtime
        (dados reais vindos do servidor), com destaque de cor configurável.
    */
    public static final class StatCard extends VBox {
        private final Label value = new Label("—");
        private final Label label;
        private final Label icon = new Label();

        public StatCard(String glyph, String labelText, String accentHex) {
            getStyleClass().add("stat-card");
            setSpacing(4);

            icon.setText(glyph);
            icon.getStyleClass().add("stat-icon");
            icon.setStyle("-fx-text-fill: " + accentHex + ";");

            value.getStyleClass().add("stat-value");
            value.setStyle("-fx-text-fill: " + accentHex + ";");

            label = new Label(labelText.toUpperCase());
            label.getStyleClass().add("stat-label");

            HBox top = new HBox(8, icon, new Region());
            HBox.setHgrow(top.getChildren().get(1), Priority.ALWAYS);
            getChildren().addAll(top, value, label);
        }

        public void setValue(String text) { value.setText(text); }
    }

    /** Linha "chave ....... valor" para painéis de detalhe. */
    public static HBox kv(String key, String value) {
        Label k = new Label(key);
        k.getStyleClass().add("muted");
        Label v = new Label(value);
        v.setStyle("-fx-font-weight: bold; -fx-text-fill: " + Theme.TEXT + ";");
        HBox h = new HBox(10, k, hSpacer(), v);
        h.setPadding(new Insets(2, 0, 2, 0));
        return h;
    }
}