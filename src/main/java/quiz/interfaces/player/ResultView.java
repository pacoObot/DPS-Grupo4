package quiz.interfaces.player;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import quiz.interfaces.common.Theme;
import quiz.interfaces.common.Ui;

/**
 * ResultView — fim de jogo (GAME_OVER).
 * Recebe o SCOREBOARD final logo antes de GAME_OVER.
 * Mostra um pódio (top 3) destacado e a restante classificação.
 */
public final class ResultView extends VBox {

    private final Label title = new Label("Fim de jogo");
    private final HBox podium = new HBox(14);
    private final VBox rest = new VBox(6);

    public ResultView() {
        setAlignment(Pos.CENTER);
        setSpacing(16);
        setPadding(new Insets(40));

        title.getStyleClass().add("title");
        Label hint = Ui.subtitle("Obrigado a todas as equipas! Classificação final:");
        podium.setAlignment(Pos.BOTTOM_CENTER);
        rest.setAlignment(Pos.CENTER);

        VBox card = Ui.card(title, hint, podium, rest);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(780);
        getChildren().add(card);
    }

    /** SCOREBOARD|pos:team:pts:ok:err:status|... */
    public void showScoreboard(String[] f) {
        podium.getChildren().clear();
        rest.getChildren().clear();

        int shown = 0;
        for (int i = 1; i < f.length; i++) {
            String[] p = f[i].split(":");
            if (p.length < 6) continue;
            int pos = parseInt(p[0], i);
            if (pos <= 3 && shown < 3) {
                podium.getChildren().add(podiumCard(pos, p[1], p[2], p[3], p[4]));
            } else {
                Label row = new Label(String.format("%-4s %-24s %6s pts   ✔%s  ✘%s   [%s]",
                        p[0] + "º", p[1], p[2], p[3], p[4], p[5]));
                row.getStyleClass().add("mono");
                row.setStyle("-fx-text-fill: " + Theme.TEXT + ";");
                rest.getChildren().add(row);
            }
            shown++;
        }
    }

    private VBox podiumCard(int pos, String team, String pts, String ok, String err) {
        String medal = switch (pos) {
            case 1 -> "🥇";
            case 2 -> "🥈";
            default -> "🥉";
        };
        Label medalL = new Label(medal);
        medalL.setStyle("-fx-font-size: 30px;");
        Label teamL = new Label(team);
        teamL.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: " + Theme.TEXT + ";");
        Label ptsL = new Label(pts + " pts");
        ptsL.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: " + Theme.ACCENT + ";");
        Label sub = new Label("✔ " + ok + "    ✘ " + err);
        sub.getStyleClass().add("muted");

        VBox box = new VBox(4, medalL, teamL, ptsL, sub);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("podium-" + pos);
        box.setMinWidth(pos == 1 ? 210 : 180);
        return box;
    }

    public void setGameName(String name) {
        title.setText("Fim de jogo — " + name);
    }

    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }
}