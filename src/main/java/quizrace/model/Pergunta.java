package quizrace.model;

import java.util.List;

public record Pergunta(String enunciado, List<String> alternativas, char respostaCorrecta,
                       String categoria) {
    public static final int LIMITE_SEGUNDOS = 15;

    public Pergunta {
        if (enunciado == null || enunciado.isBlank()) throw new IllegalArgumentException("Enunciado obrigatório.");
        if (alternativas == null || alternativas.size() != 4
                || alternativas.stream().anyMatch(a -> a == null || a.isBlank())) {
            throw new IllegalArgumentException("São necessárias quatro alternativas.");
        }
        alternativas = List.copyOf(alternativas);
        respostaCorrecta = Character.toUpperCase(respostaCorrecta);
        if (respostaCorrecta < 'A' || respostaCorrecta > 'D') throw new IllegalArgumentException("Resposta deve ser A, B, C ou D.");
        categoria = categoria == null ? "" : categoria;
    }

    public boolean estaCorrecta(Resposta resposta) {
        return !resposta.tempoEsgotado() && resposta.tempoSegundos() <= LIMITE_SEGUNDOS
                && resposta.alternativa().equals(String.valueOf(respostaCorrecta));
    }
}
