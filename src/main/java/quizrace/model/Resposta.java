package quizrace.model;

public record Resposta(String alternativa, double tempoSegundos, boolean tempoEsgotado,
                       Jogador jogador, Pergunta pergunta, boolean correcta) {
    public Resposta(String alternativa, double tempoSegundos, boolean tempoEsgotado) {
        this(alternativa, tempoSegundos, tempoEsgotado, null, null, false);
    }

    public static Resposta avaliar(Jogador jogador, Pergunta pergunta, String alternativa, double tempo, boolean timeout) {
        Resposta tentativa = new Resposta(alternativa, tempo, timeout);
        return new Resposta(tentativa.alternativa(), tempo, timeout, jogador, pergunta, pergunta.estaCorrecta(tentativa));
    }

    public Resposta {
        if (!Double.isFinite(tempoSegundos) || tempoSegundos < 0) {
            throw new IllegalArgumentException("Tempo inválido.");
        }
        alternativa = alternativa == null ? "" : alternativa.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
