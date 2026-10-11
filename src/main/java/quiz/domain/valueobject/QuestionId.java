package quiz.domain.valueobject;

/** Identificador imutável de uma pergunta. */
public record QuestionId(long value) {
    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
