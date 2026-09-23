package quizrace.model;

public record Resposta(String alternativa, double tempoSegundos, boolean tempoEsgotado) {
    public Resposta {
        if (!Double.isFinite(tempoSegundos) || tempoSegundos < 0) {
            throw new IllegalArgumentException("Tempo inválido.");
        }
        alternativa = alternativa == null ? "" : alternativa.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
