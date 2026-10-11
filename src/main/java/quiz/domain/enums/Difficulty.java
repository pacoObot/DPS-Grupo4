package quiz.domain.enums;

/** Dificuldade de uma pergunta. */
public enum Difficulty {
    EASY,
    MEDIUM,
    HARD;

    public static Difficulty parse(String raw) {
        if (raw == null) return MEDIUM;
        try {
            return Difficulty.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return MEDIUM;
        }
    }
}
