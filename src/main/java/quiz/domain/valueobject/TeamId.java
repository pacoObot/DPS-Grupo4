package quiz.domain.valueobject;

/** Identificador imutável de uma equipa. */
public record TeamId(long value) {
    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
