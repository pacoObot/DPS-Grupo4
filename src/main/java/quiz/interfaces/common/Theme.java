package quiz.interfaces.common;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * =====================================================================
 *  THEME — design system centralizado (CSS, sem FXML)
 * =====================================================================
 *
 *  Todas as cores, tipografia, espaçamentos e componentes visuais vivem
 *  AQUI, num único sítio. As vistas só aplicam classes CSS — nunca
 *  repetem estilos inline. A identidade segue a direção visual:
 *  azul-marinho profundo + azul elétrico + violeta discreto,
 *  com verde/vermelho/âmbar semânticos (sucesso/erro/aviso).
 *
 *  O CSS é escrito com tokens (@ACCENT, @PANEL, ...) e materializado
 *  por substituição — assim há uma única fonte de verdade para a paleta.
 */
public final class Theme {

    private static String stylesheetUrl;

    // ---------- tokens de cor (usados também em código ex.: status bar) ----
    public static final String BG          = "#0b1220";
    public static final String PANEL       = "#111a2e";
    public static final String PANEL_2     = "#16233c";
    public static final String PANEL_3     = "#1d2c4a";
    public static final String LINE        = "#24344f";
    public static final String TEXT        = "#e6ecf5";
    public static final String MUTED       = "#8fa3bd";
    public static final String ACCENT      = "#38bdf8";
    public static final String ACCENT_DEEP = "#0ea5e9";
    public static final String VIOLET      = "#8b5cf6";
    public static final String GREEN       = "#10b981";
    public static final String RED         = "#ef4444";
    public static final String AMBER       = "#f59e0b";
    public static final String WHITE       = "#ffffff";

    private Theme() {}

    private static final String TEMPLATE = """
        /* ================= ROOT & TIPOGRAFIA ================= */
        .root {
            -fx-background-color: @BG;
            -fx-text-fill: @TEXT;
            -fx-font-family: "Inter", "Segoe UI", "DejaVu Sans", sans-serif;
            -fx-font-size: 13px;
            -fx-focus-color: @ACCENT;
            -fx-faint-focus-color: rgba(56,189,248,0.15);
        }
        .label { -fx-text-fill: @TEXT; }
        .title { -fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: @ACCENT; }
        .section { -fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: @TEXT; }
        .subtitle { -fx-font-size: 13px; -fx-text-fill: @MUTED; }
        .muted { -fx-text-fill: @MUTED; }
        .mono { -fx-font-family: "Cascadia Mono","Consolas","DejaVu Sans Mono",monospace; }

        /* ================= CARTÕES ================= */
        .card {
            -fx-background-color: @PANEL;
            -fx-background-radius: 14;
            -fx-border-color: @LINE;
            -fx-border-radius: 14;
            -fx-padding: 18;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 16, 0.15, 0, 4);
        }
        .card-flat {
            -fx-background-color: @PANEL;
            -fx-background-radius: 12;
            -fx-border-color: @LINE;
            -fx-border-radius: 12;
            -fx-padding: 14;
        }
        .card-title { -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: @TEXT; }
        .kpi-grid { -fx-hgap: 14; -fx-vgap: 14; }

        /* ---------- cartão de estatística ---------- */
        .stat-card {
            -fx-background-color: @PANEL;
            -fx-background-radius: 14;
            -fx-border-color: @LINE;
            -fx-border-radius: 14;
            -fx-padding: 16 18 16 18;
            -fx-min-width: 190;
            -fx-min-height: 104;
        }
        .stat-card:hover { -fx-border-color: @ACCENT_DEEP; }
        .stat-value { -fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: @TEXT; }
        .stat-label { -fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: @MUTED; }
        .stat-icon { -fx-font-size: 22px; }

        /* ================= BOTÕES ================= */
        .button {
            -fx-cursor: hand;
            -fx-background-radius: 9;
            -fx-border-radius: 9;
            -fx-font-weight: bold;
            -fx-padding: 8 16 8 16;
        }
        .button:focused { -fx-border-color: @ACCENT; -fx-border-width: 2; }
        .btn-primary { -fx-background-color: @ACCENT; -fx-text-fill: #062033; }
        .btn-primary:hover { -fx-background-color: @ACCENT_DEEP; }
        .btn-primary:pressed { -fx-background-color: @ACCENT_DEEP; }
        .btn-secondary { -fx-background-color: @PANEL_3; -fx-text-fill: @TEXT; -fx-border-color: @LINE; }
        .btn-secondary:hover { -fx-background-color: @ACCENT_DEEP; }
        .btn-danger { -fx-background-color: @RED; -fx-text-fill: white; }
        .btn-danger:hover { -fx-background-color: #f87171; }
        .btn-success { -fx-background-color: @GREEN; -fx-text-fill: #06281c; }
        .btn-success:hover { -fx-background-color: #34d399; }
        .btn-ghost {
            -fx-background-color: transparent;
            -fx-text-fill: @ACCENT;
            -fx-border-color: @LINE;
        }
        .btn-ghost:hover { -fx-background-color: rgba(56,189,248,0.12); -fx-text-fill: @ACCENT; }
        /* aliases de compatibilidade (vistas migradas gradualmente) */
        .btn { -fx-background-color: @ACCENT; -fx-text-fill: #062033; }
        .btn:hover { -fx-background-color: @ACCENT_DEEP; }
        .btn:pressed { -fx-background-color: @ACCENT_DEEP; }
        .btn-sm { -fx-padding: 5 12 5 12; -fx-font-size: 12px; }
        .btn-lg { -fx-padding: 12 26 12 26; -fx-font-size: 15px; }
        .btn-block { -fx-max-width: 100000; }

        /* ================= CAMPOS DE FORMULÁRIO ================= */
        .text-field, .password-field, .text-area, .combo-box, .spinner {
            -fx-background-color: @PANEL_2;
            -fx-text-fill: @TEXT;
            -fx-prompt-text-fill: @MUTED;
            -fx-background-radius: 9;
            -fx-border-color: @LINE;
            -fx-border-radius: 9;
            -fx-padding: 8 11 8 11;
        }
        .text-field:focused, .password-field:focused,
        .text-area:focused, .combo-box:focused, .spinner:focused { -fx-border-color: @ACCENT; }
        .text-field-error, .password-field-error { -fx-border-color: @RED !important; }
        .field { -fx-background-color: @PANEL_2; -fx-text-fill: @TEXT; -fx-prompt-text-fill: @MUTED;
                 -fx-background-radius: 9; -fx-border-color: @LINE; -fx-border-radius: 9;
                 -fx-padding: 8 11 8 11; }
        .field:focused { -fx-border-color: @ACCENT; }
        .field-danger { -fx-border-color: @RED !important; }
        .text-area .content { -fx-background-color: @PANEL_2; }
        .combo-box .list-cell { -fx-text-fill: @TEXT; }
        .spinner .increment-arrow-button, .spinner .decrement-arrow-button { -fx-background-color: @PANEL_3; }
        .check-box .box { -fx-background-color: @PANEL_2; -fx-border-color: @LINE; -fx-border-radius: 5; }
        .check-box:selected .mark { -fx-background-color: @ACCENT; }
        .radio-button .radio { -fx-border-color: @LINE; }
        .radio-button:selected .dot { -fx-background-color: @ACCENT; }

        /* ================= TABELAS ================= */
        .table-view {
            -fx-background-color: transparent;
            -fx-border-color: @LINE;
            -fx-border-radius: 10;
            -fx-background-radius: 10;
            -fx-padding: 0;
        }
        .table-view .column-header-background { -fx-background-color: @PANEL_3; -fx-background-radius: 10 10 0 0; }
        .table-view .column-header, .table-view .filler {
            -fx-background-color: transparent;
            -fx-text-background-color: @TEXT;
            -fx-font-weight: bold;
            -fx-border-color: transparent transparent @LINE transparent;
            -fx-padding: 10 8 10 8;
        }
        .table-view .cell {
            -fx-background-color: transparent;
            -fx-text-fill: @TEXT;
            -fx-border-color: transparent;
            -fx-padding: 7 8 7 8;
        }
        .table-row-cell { -fx-background-color: transparent; }
        .table-row-cell:odd { -fx-background-color: rgba(255,255,255,0.025); }
        .table-row-cell:hover { -fx-background-color: rgba(56,189,248,0.08); }
        .table-row-cell:filled:selected { -fx-background-color: @ACCENT; }
        .table-row-cell:filled:selected .cell { -fx-text-fill: #062033; }

        /* ================= TABS ================= */
        .tab-pane > .tab-header-area { -fx-background-color: transparent; }
        .tab { -fx-background-color: @PANEL; -fx-background-radius: 9 9 0 0; -fx-padding: 6 18 6 18; }
        .tab:selected { -fx-background-color: @PANEL_3; }
        .tab .tab-label { -fx-text-fill: @MUTED; -fx-font-weight: bold; }
        .tab:selected .tab-label { -fx-text-fill: #062033; }

        /* ================= NAVEGAÇÃO LATERAL ================= */
        .sidebar {
            -fx-background-color: @PANEL;
            -fx-border-color: transparent @LINE transparent transparent;
            -fx-border-width: 0 1 0 0;
            -fx-padding: 16 10 16 10;
            -fx-spacing: 6;
            -fx-min-width: 232;
        }
        .nav-item {
            -fx-background-color: transparent;
            -fx-text-fill: @MUTED;
            -fx-alignment: CENTER_LEFT;
            -fx-padding: 11 16 11 16;
            -fx-background-radius: 9;
            -fx-font-weight: bold;
            -fx-font-size: 13px;
            -fx-cursor: hand;
            -fx-max-width: 100000;
        }
        .nav-item:hover { -fx-background-color: rgba(56,189,248,0.10); -fx-text-fill: @ACCENT; }
        .nav-item:focused { -fx-border-color: @ACCENT_DEEP; -fx-border-width: 1; }
        .nav-item.selected {
            -fx-background-color: rgba(56,189,248,0.16);
            -fx-text-fill: @ACCENT;
            -fx-border-color: @ACCENT_DEEP;
            -fx-border-width: 0 0 0 3;
        }
        .nav-caption { -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: @MUTED; -fx-padding: 10 16 2 16; }

        /* ================= BADGES / CHIPS ================= */
        .badge { -fx-background-color: @PANEL_2; -fx-text-fill: white; -fx-background-radius: 999;
                 -fx-padding: 3 11 3 11; -fx-font-size: 11px; -fx-font-weight: bold; }
        .badge-slate   { -fx-background-color: @MUTED; }
        .badge-success { -fx-background-color: @GREEN; -fx-text-fill: #06281c; }
        .badge-danger  { -fx-background-color: @RED; }
        .badge-warn    { -fx-background-color: @AMBER; -fx-text-fill: #3b2406; }
        .badge-info    { -fx-background-color: @ACCENT; -fx-text-fill: #062033; }
        .badge-violet  { -fx-background-color: @VIOLET; }
        .chip { -fx-background-color: rgba(255,255,255,0.14); -fx-text-fill: white;
                -fx-background-radius: 999; -fx-padding: 4 12 4 12; -fx-font-size: 12px; -fx-font-weight: bold; }

        /* ================= ALERTAS ================= */
        .alert-banner { -fx-background-radius: 10; -fx-padding: 11 14 11 14; -fx-font-weight: bold; -fx-text-fill: white; }
        .alert-info    { -fx-background-color: rgba(56,189,248,0.16); -fx-text-fill: @ACCENT; }
        .alert-success { -fx-background-color: rgba(16,185,129,0.16); -fx-text-fill: @GREEN; }
        .alert-error   { -fx-background-color: rgba(239,68,68,0.16);  -fx-text-fill: @RED; }
        .alert-warn    { -fx-background-color: rgba(245,158,11,0.16); -fx-text-fill: @AMBER; }

        /* ================= QUIZ (jogador) ================= */
        .big-timer { -fx-font-size: 42px; -fx-font-weight: bold; -fx-text-fill: @AMBER;
                     -fx-font-family: "Cascadia Mono","Consolas","DejaVu Sans Mono",monospace; }
        .big-timer.critical { -fx-text-fill: @RED; }
        .question-text { -fx-font-size: 21px; -fx-font-weight: bold; -fx-text-fill: @TEXT; -fx-line-spacing: 3; }
        .option-btn {
            -fx-background-color: #0f172a;
            -fx-text-fill: white;
            -fx-font-size: 15px;
            -fx-font-weight: bold;
            -fx-background-radius: 12;
            -fx-padding: 15 16 15 16;
            -fx-cursor: hand;
            -fx-alignment: CENTER_LEFT;
            -fx-border-color: @LINE;
            -fx-border-radius: 12;
        }
        .option-btn:hover { -fx-border-color: @ACCENT; -fx-border-width: 1; }
        .option-btn:disabled { -fx-opacity: 0.55; }
        .opt-0 { -fx-background-color: #b45309; }
        .opt-1 { -fx-background-color: #0369a1; }
        .opt-2 { -fx-background-color: #047857; }
        .opt-3 { -fx-background-color: #6d28d9; }
        .option-letter { -fx-background-color: rgba(255,255,255,0.18); -fx-text-fill: white;
                         -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 8; -fx-padding: 5 10 5 10; }
        .answer-correct { -fx-border-color: @GREEN !important; -fx-border-width: 3; }
        .answer-wrong   { -fx-border-color: @RED !important; -fx-border-width: 3; -fx-opacity: 0.5; }

        .progress-bar { -fx-accent: @ACCENT; -fx-background-radius: 8; }
        .progress-bar .track { -fx-background-color: @LINE; -fx-background-radius: 8; -fx-pref-height: 14; }
        .progress-bar .bar { -fx-background-color: @ACCENT; -fx-background-radius: 8; }
        .progress-low .bar { -fx-background-color: @RED; }

        /* ================= CABEÇALHO ================= */
        .app-header {
            -fx-background-color: linear-gradient(to right, #06b6d4, #0ea5e9, #6366f1);
            -fx-padding: 14 22 14 22;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 12, 0.1, 0, 2);
        }
        .app-header .header-title { -fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white; }
        .app-header .header-sub { -fx-font-size: 12px; -fx-text-fill: #e0f2fe; }
        .app-header .header-status { -fx-font-size: 12px; -fx-text-fill: #e2e8f0; }

        /* ================= MISC ================= */
        .round-badge { -fx-background-color: @ACCENT; -fx-text-fill: #062033; -fx-background-radius: 8;
                       -fx-padding: 6 14 6 14; -fx-font-size: 15px; -fx-font-weight: bold; }
        .score-big { -fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: @TEXT; }
        .log { -fx-background-color: #070c16; -fx-text-fill: #7ee2a8;
               -fx-font-family: "Cascadia Mono","Consolas","DejaVu Sans Mono",monospace; -fx-font-size: 11px; }
        .log .content { -fx-background-color: #070c16; }
        .sep { -fx-background-color: @LINE; -fx-pref-height: 1; }
        .podium { -fx-background-color: @PANEL_3; -fx-background-radius: 14; -fx-padding: 10; }
        .podium-1 { -fx-background-color: rgba(245,158,11,0.18); -fx-background-radius: 12; -fx-padding: 12; }
        .podium-2 { -fx-background-color: rgba(148,163,184,0.14); -fx-background-radius: 12; -fx-padding: 12; }
        .podium-3 { -fx-background-color: rgba(180,83,9,0.16);   -fx-background-radius: 12; -fx-padding: 12; }
        """;

    /** CSS final: substitui os tokens @NOME pelas cores da paleta. */
    public static String css() {
        return TEMPLATE
                .replace("@ACCENT_DEEP", ACCENT_DEEP) // antes de @ACCENT
                .replace("@ACCENT", ACCENT)
                .replace("@PANEL_2", PANEL_2)         // antes de @PANEL
                .replace("@PANEL_3", PANEL_3)
                .replace("@PANEL", PANEL)
                .replace("@BG", BG)
                .replace("@LINE", LINE)
                .replace("@TEXT", TEXT)
                .replace("@MUTED", MUTED)
                .replace("@VIOLET", VIOLET)
                .replace("@GREEN", GREEN)
                .replace("@RED", RED)
                .replace("@AMBER", AMBER)
                .replace("@WHITE", WHITE);
    }

    /** Garante o ficheiro de CSS e aplica-o a uma raiz (opcional). */
    public static synchronized void apply(javafx.scene.Parent root) {
        if (stylesheetUrl == null) {
            try {
                Path f = Files.createTempFile("quiz-paralelo-theme-", ".css");
                f.toFile().deleteOnExit();
                Files.writeString(f, css(), StandardCharsets.UTF_8);
                stylesheetUrl = f.toUri().toString();
            } catch (IOException e) {
                throw new IllegalStateException("Falha ao materializar tema: " + e.getMessage(), e);
            }
        }
        if (root != null) {
            if (!root.getStylesheets().contains(stylesheetUrl)) root.getStylesheets().add(stylesheetUrl);
            if (!root.getStyleClass().contains("root")) root.getStyleClass().add("root");
        }
    }

    /** Lista de stylesheets para Scene.getStylesheets(). */
    public static synchronized String[] stylesheets() {
        apply(null);
        return new String[]{stylesheetUrl};
    }
}