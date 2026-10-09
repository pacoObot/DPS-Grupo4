package quizrace.service;

import quizrace.model.*;
import quizrace.util.*;
import java.io.PrintStream;
import java.util.List;

public final class QuizSequencial {
    private final EntradaConsole entrada;
    private final PrintStream saida;
    private final CalculadoraPontuacao calculadora = new CalculadoraPontuacao();

    public QuizSequencial(EntradaConsole entrada, PrintStream saida) {
        this.entrada = entrada;
        this.saida = saida;
    }

    public double executar(List<Jogador> jogadores, List<Pergunta> perguntas) {
        if (jogadores.size() < 2 || jogadores.size() > 4) throw new IllegalArgumentException("Registe entre 2 e 4 jogadores.");
        if (perguntas.isEmpty()) throw new IllegalArgumentException("São necessárias perguntas.");
        return new MedidorDesempenho().medir(() -> {
            for (Jogador jogador : jogadores) {
                saida.println("\nVez de " + jogador.getNome());
                for (Pergunta pergunta : perguntas) {
                    saida.println("\n" + pergunta.enunciado());
                    for (int i = 0; i < 4; i++) saida.println((char) ('A' + i) + ") " + pergunta.alternativas().get(i));
                    saida.println("Responda A, B, C ou D (15 segundos):");
                    saida.flush();
                    Cronometro cronometro = new Cronometro();
                    String alternativa;
                    double tempo;
                    while (true) {
                        long restante = (long) ((Pergunta.LIMITE_SEGUNDOS - cronometro.segundosDecorridos()) * 1_000_000_000L);
                        alternativa = entrada.ler(restante);
                        tempo = cronometro.segundosDecorridos();
                        if (alternativa == null || tempo >= Pergunta.LIMITE_SEGUNDOS) break;
                        if (alternativa.trim().matches("(?i)[ABCD]")) break;
                        saida.println("Alternativa inválida. Use A, B, C ou D; o prazo continua.");
                    }
                    boolean esgotado = alternativa == null || tempo >= Pergunta.LIMITE_SEGUNDOS;
                    Resposta resposta = new Resposta(alternativa, esgotado ? Pergunta.LIMITE_SEGUNDOS : tempo, esgotado);
                    boolean correcta = pergunta.estaCorrecta(resposta);
                    double pontos = calculadora.calcular(correcta, resposta.tempoSegundos());
                    if (esgotado) jogador.registrarTimeout(resposta.tempoSegundos());
                    else jogador.registarResposta(correcta, pontos, resposta.tempoSegundos());
                    saida.printf("%s Pontos: %.2f | Tempo: %.2f s%n", esgotado ? "Tempo esgotado!" : correcta ? "Correcto!" : "Errado!", pontos, resposta.tempoSegundos());
                }
            }
        });
    }
}
