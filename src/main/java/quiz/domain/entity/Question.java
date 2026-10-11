package quiz.domain.entity;

import quiz.domain.enums.Difficulty;
import quiz.domain.valueobject.QuestionId;

import java.util.Objects;

/**
 * Pergunta do quiz — entidade de domínio pura.
 * A resposta CORRETA nunca é enviada ao cliente (regra de segurança da spec §17).
 */
public class Question {

    private final QuestionId id;
    private String text;
    private String category;
    private Difficulty difficulty;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private char correctOption;
    private int points;
    private int timeLimit;
    private boolean active;

    public Question(QuestionId id, String text, String category, Difficulty difficulty,
                    String optionA, String optionB, String optionC, String optionD,
                    char correctOption, int points, int timeLimit, boolean active) {
        this.id = id;
        this.text = text;
        this.category = category;
        this.difficulty = difficulty == null ? Difficulty.MEDIUM : difficulty;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.correctOption = Character.toUpperCase(correctOption);
        this.points = points;
        this.timeLimit = timeLimit;
        this.active = active;
    }

    /** Pergunta ainda não persistida (id preenchido pelo repositório). */
    public static Question create(String text, String category, Difficulty difficulty,
                                  String a, String b, String c, String d,
                                  char correct, int points, int timeLimit, boolean active) {
        return new Question(null, text, category, difficulty, a, b, c, d, correct, points, timeLimit, active);
    }

    /** Validação de negócio pura — lança IllegalArgumentException com mensagem amigável. */
    public void validate() {
        if (text == null || text.isBlank() || text.length() > 500)
            throw new IllegalArgumentException("Texto da pergunta inválido (1-500 caracteres)");
        if (optionA == null || optionA.isBlank() || optionB == null || optionB.isBlank()
                || optionC == null || optionC.isBlank() || optionD == null || optionD.isBlank())
            throw new IllegalArgumentException("As 4 alternativas são obrigatórias");
        if (correctOption < 'A' || correctOption > 'D')
            throw new IllegalArgumentException("Resposta correta deve ser A, B, C ou D");
        if (points <= 0 || points > 100)
            throw new IllegalArgumentException("Pontuação deve estar entre 1 e 100");
        if (timeLimit <= 0 || timeLimit > 300)
            throw new IllegalArgumentException("Tempo deve estar entre 1 e 300 segundos");
    }

    public boolean isCorrect(char selected) {
        return Character.toUpperCase(selected) == correctOption;
    }

    /** Devolve a alternativa por letra (A..D) — usado para serializar o protocolo. */
    public String optionFor(char letter) {
        return switch (Character.toUpperCase(letter)) {
            case 'A' -> optionA;
            case 'B' -> optionB;
            case 'C' -> optionC;
            case 'D' -> optionD;
            default -> "";
        };
    }

    public String[] options() {
        return new String[]{optionA, optionB, optionC, optionD};
    }

    // ---- getters/setters -------------------------------------------------
    public QuestionId getId() { return id; }
    public void setId(QuestionId id) { throw new UnsupportedOperationException("id é imutável; crie nova instância"); }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }
    public String getOptionA() { return optionA; }
    public void setOptionA(String optionA) { this.optionA = optionA; }
    public String getOptionB() { return optionB; }
    public void setOptionB(String optionB) { this.optionB = optionB; }
    public String getOptionC() { return optionC; }
    public void setOptionC(String optionC) { this.optionC = optionC; }
    public String getOptionD() { return optionD; }
    public void setOptionD(String optionD) { this.optionD = optionD; }
    public char getCorrectOption() { return correctOption; }
    public void setCorrectOption(char correctOption) { this.correctOption = Character.toUpperCase(correctOption); }
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
    public int getTimeLimit() { return timeLimit; }
    public void setTimeLimit(int timeLimit) { this.timeLimit = timeLimit; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Question q)) return false;
        return Objects.equals(id, q.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Question{" + id + ", '" + (text == null ? "" : text.substring(0, Math.min(30, text.length())))
                + "...', correct=" + correctOption + ", active=" + active + '}';
    }
}
