package quiz.domain.valueobject;

/** Identificador imutável de um jogador. */
public record PlayerId(long value) {
    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
