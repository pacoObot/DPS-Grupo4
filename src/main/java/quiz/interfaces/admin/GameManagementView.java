package quiz.interfaces.admin;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import quiz.interfaces.common.TcpClient;
import quiz.interfaces.common.Theme;
import quiz.shared.dto.QuestionRowDTO;
import quiz.shared.protocol.Protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GameManagementView — gestão da partida (spec §9):
 * criar, configurar tempo, selecionar perguntas, abrir inscrições,
 * iniciar, pausar, retomar, encerrar + ver equipes e classificação.
 */
public final class GameManagementView extends VBox {

    private final TcpClient tcp;
    private final ObservableList<QuestionRowDTO> questionRows = FXCollections.observableArrayList();
    private final TableView<QuestionRowDTO> questionTable = new TableView<>(questionRows);
    private final ObservableList<String[]> teamRows = FXCollections.observableArrayList();
    private final TableView<String[]> teamTable = new TableView<>(teamRows);
    private final ObservableList<String[]> boardRows = FXCollections.observableArrayList();
    private final TableView<String[]> boardTable = new TableView<>(boardRows);

    private final TextField nameField = new TextField("Partida Demo");
    private final Spinner<Integer> timeSpinner = new Spinner<>(5, 120, 15);
    private final TextField gameIdField = new TextField();
    private final TextField codeField = new TextField();
    private final Label status = new Label("Sem partida ativa.");
    private final Label teamsTitle = new Label("Equipes inscritas (0)");

    private long currentGameId = -1;

    public GameManagementView(TcpClient tcp) {
        this.tcp = tcp;
        setSpacing(10);
        setPadding(new Insets(14));

        buildQuestions();
        buildForm();
        buildTeams();
        buildBoard();
        refreshQuestions();
    }

    // ------------------------------------------------------------------ UI
    @SuppressWarnings("unchecked")
    private void buildQuestions() {
        Label title = new Label("1. PERGUNTAS DISPONÍVEIS (selecione com Ctrl/Shift)");
        title.getStyleClass().add("subtitle");

        questionTable.setPrefHeight(170);
        questionTable.getStyleClass().add("table-view");
        questionTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        TableColumn<QuestionRowDTO, String> textCol = new TableColumn<>("Pergunta");
        textCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().text()));
        textCol.setPrefWidth(420);
        TableColumn<QuestionRowDTO, String> diffCol = new TableColumn<>("Dificuldade");
        diffCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().difficulty()));
        TableColumn<QuestionRowDTO, Number> ptsCol = new TableColumn<>("Pontos");
        ptsCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().points()));
        TableColumn<QuestionRowDTO, String> activeCol = new TableColumn<>("Estado");
        activeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().active() ? "ATIVA" : "inativa"));
        questionTable.getColumns().setAll(List.of(textCol, diffCol, ptsCol, activeCol));

        Button refreshBtn = new Button("Atualizar lista");
        refreshBtn.getStyleClass().add("btn-ghost");
        refreshBtn.setOnAction(e -> refreshQuestions());

        HBox head = new HBox(10, title, refreshBtn);
        head.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        getChildren().addAll(head, questionTable);
        VBox.setVgrow(questionTable, Priority.ALWAYS);
    }

    private void buildForm() {
        Label title = new Label("2. CONFIGURAR E CRIAR PARTIDA");
        title.getStyleClass().add("subtitle");

        nameField.setPromptText("Nome da partida");
        nameField.getStyleClass().add("field");
        nameField.setPrefWidth(220);
        timeSpinner.setPrefWidth(80);
        timeSpinner.setEditable(true);

        Button createBtn = new Button("CRIAR PARTIDA");
        createBtn.getStyleClass().add("btn-success");
        createBtn.setOnAction(e -> createGame());

        gameIdField.setPromptText("ID");
        gameIdField.getStyleClass().add("field");
        gameIdField.setPrefWidth(70);
        codeField.setPromptText("código");
        codeField.getStyleClass().add("field");
        codeField.setPrefWidth(90);
        codeField.setEditable(false);

        Button openBtn = btn("ABRIR INSCRIÇÕES", "btn", () -> sendGameCmd("OPEN_GAME"));
        Button startBtn = btn("INICIAR", "btn-success", () -> sendGameCmd("START_GAME"));
        Button pauseBtn = btn("PAUSAR", "btn", () -> sendGameCmd("PAUSE_GAME"));
        Button resumeBtn = btn("RETOMAR", "btn", () -> sendGameCmd("RESUME_GAME"));
        Button stopBtn = btn("ENCERRAR", "btn-danger", () -> sendGameCmd("STOP_GAME"));

        HBox row1 = new HBox(8, title, nameField, new Label("Tempo (s)"), timeSpinner, createBtn);
        row1.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox row2 = new HBox(8, new Label("Partida ID"), gameIdField, new Label("Código"), codeField,
                openBtn, startBtn, pauseBtn, resumeBtn, stopBtn);
        row2.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        status.getStyleClass().add("subtitle");
        getChildren().addAll(row1, row2, status);
    }

    @SuppressWarnings("unchecked")
    private void buildTeams() {
        teamTable.setPrefHeight(130);
        teamTable.getStyleClass().add("table-view");
        TableColumn<String[], String> nameCol = new TableColumn<>("Equipe");
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[0]));
        nameCol.setPrefWidth(200);
        TableColumn<String[], String> ptsCol = new TableColumn<>("Pontos");
        ptsCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[1]));
        ptsCol.setPrefWidth(80);
        TableColumn<String[], String> stCol = new TableColumn<>("Estado");
        stCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[2]));
        stCol.setPrefWidth(110);
        TableColumn<String[], String> thCol = new TableColumn<>("Thread (paralelismo)");
        thCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[3]));
        thCol.setPrefWidth(180);
        teamTable.getColumns().setAll(List.of(nameCol, ptsCol, stCol, thCol));

        Button refreshBtn = new Button("Atualizar equipes");
        refreshBtn.getStyleClass().add("btn-ghost");
        refreshBtn.setOnAction(e -> refreshTeams());

        HBox head = new HBox(10, teamsTitle, refreshBtn);
        head.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        getChildren().addAll(head, teamTable);
    }

    @SuppressWarnings("unchecked")
    private void buildBoard() {
        boardTable.setPrefHeight(130);
        boardTable.getStyleClass().add("table-view");
        TableColumn<String[], String> posCol = new TableColumn<>("#");
        posCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[0]));
        posCol.setPrefWidth(40);
        TableColumn<String[], String> nameCol = new TableColumn<>("Equipe");
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[1]));
        nameCol.setPrefWidth(220);
        TableColumn<String[], String> ptsCol = new TableColumn<>("Pontos");
        ptsCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[2]));
        ptsCol.setPrefWidth(80);
        TableColumn<String[], String> okCol = new TableColumn<>("✓");
        okCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[3]));
        okCol.setPrefWidth(50);
        TableColumn<String[], String> errCol = new TableColumn<>("✗");
        errCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[4]));
        errCol.setPrefWidth(50);
        TableColumn<String[], String> stCol = new TableColumn<>("Estado");
        stCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[5]));
        stCol.setPrefWidth(110);
        boardTable.getColumns().setAll(List.of(posCol, nameCol, ptsCol, okCol, errCol, stCol));

        Button refreshBtn = new Button("Atualizar classificação");
        refreshBtn.getStyleClass().add("btn-ghost");
        refreshBtn.setOnAction(e -> refreshScoreboard());

        Label title = new Label("CLASSIFICAÇÃO");
        title.getStyleClass().add("subtitle");
        HBox head = new HBox(10, title, refreshBtn);
        head.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        getChildren().addAll(head, boardTable);
        VBox.setVgrow(boardTable, Priority.ALWAYS);
    }

    // -------------------------------------------------------------- ações
    public void refreshQuestions() {
        questionRows.clear();
        tcp.send("LIST_QUESTIONS|||true");
    }

    private void createGame() {
        List<Long> ids = questionTable.getSelectionModel().getSelectedItems().stream()
                .map(QuestionRowDTO::id)
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            status("Selecione pelo menos 1 pergunta.", true);
            return;
        }
        if (nameField.getText().isBlank()) {
            status("Dê um nome à partida.", true);
            return;
        }
        tcp.send(Protocol.build("CREATE_GAME", nameField.getText().trim(),
                ids.stream().map(String::valueOf).collect(Collectors.joining(",")),
                timeSpinner.getValue()));
    }

    private void sendGameCmd(String cmd) {
        long id = parseId();
        if (id < 0) {
            status("Indique o ID da partida (crie uma primeiro).", true);
            return;
        }
        if ("STOP_GAME".equals(cmd)) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Encerrar a partida #" + id + "? As equipas verão o resultado final.",
                    javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);
            confirm.setHeaderText("Confirmar encerramento");
            confirm.showAndWait().ifPresent(bt -> {
                if (bt == javafx.scene.control.ButtonType.OK) tcp.send(cmd + "|" + id);
            });
            return;
        }
        tcp.send(cmd + "|" + id);
    }

    public void refreshTeams() {
        long id = parseId();
        if (id >= 0) tcp.send("GET_TEAMS|" + id);
    }

    public void refreshScoreboard() {
        long id = parseId();
        if (id >= 0) tcp.send("GET_SCOREBOARD|" + id);
    }

    private long parseId() {
        try {
            return Long.parseLong(gameIdField.getText().trim());
        } catch (Exception e) {
            return currentGameId;
        }
    }

    private void status(String msg, boolean error) {
        status.setText(msg);
        status.setStyle("-fx-text-fill: " + (error ? Theme.RED : Theme.GREEN) + ";");
    }

    private Button btn(String text, String cls, Runnable action) {
        Button b = new Button(text);
        b.getStyleClass().add(cls);
        b.setOnAction(e -> action.run());
        return b;
    }

    // ----------------------------------------------------------- dispatch
    public void dispatch(String type, String[] f) {
        switch (type) {
            case "QUESTION_ROW" -> {
                if (f.length >= 12) {
                    try {
                        questionRows.add(QuestionRowDTO.fromProtocol(f));
                    } catch (RuntimeException ignored) {
                    }
                }
            }
            case "GAME_CREATED" -> {
                currentGameId = Long.parseLong(f[0]);
                gameIdField.setText(f[0]);
                codeField.setText(f[1]);
                status("Partida criada! Código para os jogadores: " + f[1] + " — abra as inscrições.", false);
            }
            case "GAME_STATUS" -> {
                if (f.length >= 2) {
                    gameIdField.setText(f[0]);
                    try {
                        currentGameId = Long.parseLong(f[0]);
                    } catch (NumberFormatException ignored) {
                    }
                    status("Partida " + f[0] + " -> " + f[1]
                            + (f.length > 2 ? " (ronda " + f[2] + ")" : ""), false);
                    refreshTeams();
                    refreshScoreboard();
                }
            }
            case "TEAMS" -> {
                teamRows.clear();
                teamsTitle.setText("Equipes inscritas (" + (f.length - 1) + ")");
                for (int i = 1; i < f.length; i++) {
                    String[] p = f[i].split(":");
                    if (p.length >= 4) teamRows.add(p);
                }
            }
            case "SCOREBOARD" -> {
                boardRows.clear();
                for (int i = 1; i < f.length; i++) {
                    String[] p = f[i].split(":");
                    if (p.length >= 6) boardRows.add(p);
                }
            }
            case "TEAM_EVENT" -> {
                if (f.length >= 3) {
                    status("[" + f[2] + "] " + f[0] + " -> " + f[1], false);
                    refreshScoreboard();
                }
            }
            case "ROUND_EVENT" -> {
                if (f.length >= 3) {
                    status("Ronda " + f[2] + " " + (f[1].equals("START") ? "iniciada" : "concluída")
                            + " (" + (f.length > 3 ? f[3] : "") + ")", false);
                    refreshTeams();
                    refreshScoreboard();
                }
            }
            default -> { }
        }
    }
}
