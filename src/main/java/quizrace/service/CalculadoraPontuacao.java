package quizrace.service;

public final class CalculadoraPontuacao {
    public double calcular(boolean correcta, double tempoSegundos) {
        if (!Double.isFinite(tempoSegundos) || tempoSegundos < 0) throw new IllegalArgumentException("Tempo inválido.");
        return correcta ? Math.max(10, 100 - tempoSegundos * 5) : 0;
    }
}
