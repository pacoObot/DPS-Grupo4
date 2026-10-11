package quiz.interfaces.admin;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import quiz.interfaces.common.TcpClient;
import quiz.interfaces.common.Theme;
import quiz.shared.dto.QuestionRowDTO;
import quiz.shared.protocol.Protocol;

import java.util.ArrayList;
import java.util.List;

/**
 * QuestionManagementView — CRUD completo de perguntas (spec §9).
 * TODO o acesso ao banco passa pelo TCP -> UseCase -> Repository (spec §10);
 * esta classe apenas monta mensagens do protocolo.
 */
public final class QuestionManagementView extends VBox {

    private final TcpClient tcp;
    private final ObservableList<QuestionRowDTO> rows = FXCollections.observableArrayList();
    private final TableView<QuestionRowDTO> table = new TableView<>(rows);
    private final Label status = new Label();

    // formulário
    private final TextField searchField = new TextField();
    private final ComboBox<String> searchDiff = new ComboBox<>();
    private final ComboBox<String> searchActive = new ComboBox<>();
    private final TextField idField = new TextField();
    private final TextArea textArea = new TextArea();
    private final TextField categoryField = new TextField();
    private final ComboBox<String> difficultyBox = new ComboBox<>();
    private final TextField optA = new TextField(), optB = new TextField(),
            optC = new TextField(), optD = new TextField();
    private final ComboBox<String> correctBox = new ComboBox<>();
    private final TextField pointsField = new TextField("10");
    private final TextField timeField = new TextField("15");
    private final CheckBox activeCheck = new CheckBox("Ativa");

    private Long editingId = null;

    public QuestionManagementView(TcpClient tcp) {
        this.tcp = tcp;
        setSpacing(10);
        setPadding(new Insets(14));

        buildToolbar();
        buildTable();
        buildForm();
        refresh();
    }

    // ------------------------------------------------------------------ UI
    private void buildToolbar() {
        searchField.setPromptText("Pesquisar pergunta...");
        searchField.getStyleClass().add("field");
        searchField.setPrefWidth(240);
        searchField.setOnAction(e -> refresh());

        searchDiff.getItems().setAll("TODAS", "EASY", "MEDIUM", "HARD");
        searchDiff.setValue("TODAS");
        searchDiff.getStyleClass().add("field");

        searchActive.getItems().setAll("TODAS", "ATIVAS", "INATIVAS");
        searchActive.setValue("TODAS");
        searchActive.getStyleClass().add("field");

        Button searchBtn = new Button("Pesquisar");
        searchBtn.getStyleClass().add("btn");
        searchBtn.setOnAction(e -> refresh());

        Button newBtn = new Button("Nova pergunta");
        newBtn.getStyleClass().add("btn-ghost");
        newBtn.setOnAction(e -> clearForm());

        HBox bar = new HBox(8, searchField, searchDiff, searchActive, searchBtn, newBtn);
        bar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        getChildren().add(bar);
    }

    @SuppressWarnings("unchecked")
    private void buildTable() {
        table.setPrefHeight(260);
        table.getStyleClass().add("table-view");

        TableColumn<QuestionRowDTO, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(c -> new javafx.beans.property.SimpleLongProperty(c.getValue().id()));
        idCol.setPrefWidth(55);

        TableColumn<QuestionRowDTO, String> textCol = new TableColumn<>("Pergunta");
        textCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().text()));
        textCol.setPrefWidth(340);

        TableColumn<QuestionRowDTO, String> catCol = new TableColumn<>("Categoria");
        catCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().category()));

        TableColumn<QuestionRowDTO, String> diffCol = new TableColumn<>("Dific.");
        diffCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().difficulty()));
        diffCol.setPrefWidth(70);

        TableColumn<QuestionRowDTO, String> correctCol = new TableColumn<>("Certa");
        correctCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().correct())));
        correctCol.setPrefWidth(55);

        TableColumn<QuestionRowDTO, Number> ptsCol = new TableColumn<>("Pts");
        ptsCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().points()));
        ptsCol.setPrefWidth(50);

        TableColumn<QuestionRowDTO, Number> timeCol = new TableColumn<>("Tempo");
        timeCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().timeLimit()));
        timeCol.setPrefWidth(60);

        TableColumn<QuestionRowDTO, Boolean> activeCol = new TableColumn<>("Ativa");
        activeCol.setCellValueFactory(c -> new SimpleBooleanProperty(c.getValue().active()));
        activeCol.setPrefWidth(55);

        table.getColumns().setAll(List.of(idCol, textCol, catCol, diffCol, correctCol, ptsCol, timeCol, activeCol));
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) loadSelected();
        });
        getChildren().add(table);
        VBox.setVgrow(table, Priority.ALWAYS);
    }

    private void buildForm() {
        idField.setEditable(false);
        idField.setPrefWidth(60);
        idField.getStyleClass().add("field");
        textArea.setPromptText("Texto da pergunta");
        textArea.setPrefRowCount(2);
        textArea.getStyleClass().add("field");
        categoryField.setPromptText("Categoria");
        categoryField.getStyleClass().add("field");

        difficultyBox.getItems().setAll("EASY", "MEDIUM", "HARD");
        difficultyBox.setValue("MEDIUM");
        difficultyBox.getStyleClass().add("field");

        for (TextField f : new TextField[]{optA, optB, optC, optD}) {
            f.setPromptText("Alternativa");
            f.getStyleClass().add("field");
        }
        correctBox.getItems().setAll("A", "B", "C", "D");
        correctBox.setValue("A");
        correctBox.getStyleClass().add("field");
        pointsField.getStyleClass().add("field");
        timeField.getStyleClass().add("field");
        activeCheck.setSelected(true);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        ColumnConstraints c1 = new ColumnConstraints(70);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setMinWidth(0);
        c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);

        grid.add(new Label("ID"), 0, 0);            grid.add(idField, 1, 0);
        grid.add(new Label("Pergunta"), 0, 1);      grid.add(textArea, 1, 1);
        grid.add(new Label("Categoria"), 0, 2);     grid.add(categoryField, 1, 2);
        grid.add(new Label("Dificuldade"), 0, 3);   grid.add(difficultyBox, 1, 3);
        grid.add(new Label("Alternativas"), 0, 4);
        HBox opts = new HBox(8, optA, optB, optC, optD);
        HBox.setHgrow(optA, Priority.ALWAYS); HBox.setHgrow(optB, Priority.ALWAYS);
        HBox.setHgrow(optC, Priority.ALWAYS); HBox.setHgrow(optD, Priority.ALWAYS);
        grid.add(opts, 1, 4);
        grid.add(new Label("Correta"), 0, 5);
        HBox correctRow = new HBox(10, correctBox, new Label("Pontos"), pointsField,
                new Label("Tempo(s)"), timeField, activeCheck);
        correctRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        grid.add(correctRow, 1, 5);

        Button saveBtn = new Button("SALVAR");
        saveBtn.getStyleClass().add("btn-success");
        saveBtn.setOnAction(e -> save());

        Button editBtn = new Button("EDITAR");
        editBtn.getStyleClass().add("btn");
        editBtn.setOnAction(e -> loadSelected());

        Button delBtn = new Button("ELIMINAR");
        delBtn.getStyleClass().add("btn-danger");
        delBtn.setOnAction(e -> deleteSelected());

        Button onBtn = new Button("ATIVAR");
        onBtn.getStyleClass().add("btn-ghost");
        onBtn.setOnAction(e -> setActiveSelected(true));

        Button offBtn = new Button("DESATIVAR");
        offBtn.getStyleClass().add("btn-ghost");
        offBtn.setOnAction(e -> setActiveSelected(false));

        HBox buttons = new HBox(8, saveBtn, editBtn, delBtn, onBtn, offBtn);
        buttons.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        status.getStyleClass().add("subtitle");

        VBox form = new VBox(10, new Label("FORMULÁRIO DE PERGUNTA"), grid, buttons, status);
        form.getStyleClass().add("card");
        getChildren().add(form);
    }

    // -------------------------------------------------------------- ações
    public void refresh() {
        String search = searchField.getText() == null ? "" : searchField.getText().trim();
        String diff = searchDiff.getValue() == null || searchDiff.getValue().equals("TODAS")
                ? "" : searchDiff.getValue();
        String act = searchActive.getValue() == null ? "" : switch (searchActive.getValue()) {
            case "ATIVAS" -> "true";
            case "INATIVAS" -> "false";
            default -> "";
        };
        rows.clear();
        tcp.send(Protocol.build("LIST_QUESTIONS", search, diff, act));
    }

    private void save() {
        List<String> parts = new ArrayList<>();
        parts.add(textArea.getText().trim());
        parts.add(categoryField.getText().trim());
        parts.add(difficultyBox.getValue());
        parts.add(optA.getText().trim());
        parts.add(optB.getText().trim());
        parts.add(optC.getText().trim());
        parts.add(optD.getText().trim());
        parts.add(correctBox.getValue());
        parts.add(pointsField.getText().trim());
        parts.add(timeField.getText().trim());
        parts.add(String.valueOf(activeCheck.isSelected()));

        if (parts.get(0).isEmpty() || parts.get(3).isEmpty() || parts.get(4).isEmpty()
                || parts.get(5).isEmpty() || parts.get(6).isEmpty()) {
            status("Preencha o texto e as 4 alternativas.", true);
            return;
        }
        if (parts.get(1).isEmpty()) {
            status("Indique a categoria.", true);
            return;
        }
        if (!validRange(parts.get(8), 1, 1000)) {
            status("Pontuação deve ser um número entre 1 e 1000.", true);
            return;
        }
        if (!validRange(parts.get(9), 5, 300)) {
            status("Tempo deve ser um número entre 5 e 300 segundos.", true);
            return;
        }

        if (editingId == null) {
            tcp.send("CREATE_QUESTION|" + String.join("|", parts));
        } else {
            tcp.send("UPDATE_QUESTION|" + editingId + "|" + String.join("|", parts));
        }
    }

    private static boolean validRange(String value, int min, int max) {
        try {
            int v = Integer.parseInt(value.trim());
            return v >= min && v <= max;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void deleteSelected() {
        QuestionRowDTO sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { status("Selecione uma pergunta na tabela.", true); return; }
        // Confirmação antes de uma operação destrutiva (acessibilidade/robustez).
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Eliminar a pergunta #" + sel.id() + "?\n\n"
                        + "Perguntas já usadas em partidas não podem ser eliminadas "
                        + "(preservação do histórico) — desative-as.",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText("Confirmar eliminação");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) tcp.send("DELETE_QUESTION|" + sel.id());
        });
    }

    private void setActiveSelected(boolean active) {
        QuestionRowDTO sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { status("Selecione uma pergunta na tabela.", true); return; }
        tcp.send("SET_QUESTION_ACTIVE|" + sel.id() + "|" + active);
    }

    private void loadSelected() {
        QuestionRowDTO sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        editingId = sel.id();
        idField.setText(String.valueOf(sel.id()));
        textArea.setText(sel.text());
        categoryField.setText(sel.category());
        difficultyBox.setValue(sel.difficulty());
        optA.setText(sel.optionA());
        optB.setText(sel.optionB());
        optC.setText(sel.optionC());
        optD.setText(sel.optionD());
        correctBox.setValue(String.valueOf(sel.correct()));
        pointsField.setText(String.valueOf(sel.points()));
        timeField.setText(String.valueOf(sel.timeLimit()));
        activeCheck.setSelected(sel.active());
        status("Editando pergunta #" + sel.id(), false);
    }

    private void clearForm() {
        editingId = null;
        idField.clear();
        textArea.clear();
        categoryField.clear();
        difficultyBox.setValue("MEDIUM");
        optA.clear(); optB.clear(); optC.clear(); optD.clear();
        correctBox.setValue("A");
        pointsField.setText("10");
        timeField.setText("15");
        activeCheck.setSelected(true);
        status("Nova pergunta.", false);
    }

    // ----------------------------------------------------------- dispatch
    /** Mensagens do servidor relacionadas com perguntas. */
    public void dispatch(String type, String[] f) {
        switch (type) {
            case "QUESTION_ROW" -> {
                if (f.length >= 12) {
                    try {
                        rows.add(QuestionRowDTO.fromProtocol(f));
                    } catch (RuntimeException ignored) {
                    }
                }
            }
            case "LIST_END" -> { /* fim da listagem */ }
            case "QUESTION_CREATED" -> { status("Pergunta #" + f[0] + " criada.", false); refresh(); }
            case "QUESTION_UPDATED" -> { status("Pergunta #" + f[0] + " atualizada.", false); refresh(); }
            case "QUESTION_DELETED" -> {
                status("Pergunta #" + f[0] + " eliminada.", false);
                if (editingId != null && String.valueOf(editingId).equals(f[0])) clearForm();
                refresh();
            }
            default -> { }
        }
    }

    private void status(String msg, boolean error) {
        status.setText(msg);
        status.setStyle("-fx-text-fill: " + (error ? Theme.RED : Theme.GREEN) + ";");
    }
}
