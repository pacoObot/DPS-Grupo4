package quizrace.benchmark;

public record ResultadoBenchmark(int jogadores, double sequencialMs, double paraleloMs, int threads) {
    public ResultadoBenchmark {
        if (jogadores < 1 || threads < 1 || !Double.isFinite(sequencialMs) || !Double.isFinite(paraleloMs)
                || sequencialMs <= 0 || paraleloMs <= 0) throw new IllegalArgumentException("Medições inválidas.");
    }
    public double speedup() { return sequencialMs / paraleloMs; }
    public double eficiencia() { return speedup() / threads; }
}
