package quizrace.util;

public final class Cronometro {
    private final long inicio = System.nanoTime();
    public double segundosDecorridos() { return (System.nanoTime() - inicio) / 1_000_000_000.0; }
}
