package quiz.domain.valueobject;

/** Identificador imutável de uma partida. */
public record GameId(long value) {
    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
