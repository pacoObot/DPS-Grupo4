package quizrace.benchmark;

import quizrace.model.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

public record JogadorSimulado(String nome, List<Resposta> respostas) {
    public JogadorSimulado { respostas = List.copyOf(respostas); }
    public record Resultado(Jogador jogador, long verificacao) { }

    public static List<JogadorSimulado> gerar(int quantidade, List<Pergunta> perguntas, long semente) {
        if (quantidade <= 0) throw new IllegalArgumentException("Quantidade inválida.");
        var aleatorio = new SplittableRandom(semente);
        List<JogadorSimulado> jogadores = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            List<Resposta> respostas = new ArrayList<>();
            for (Pergunta pergunta : perguntas) {
                char alternativa = aleatorio.nextDouble() < .75 ? pergunta.respostaCorrecta()
                        : (char) ('A' + (pergunta.respostaCorrecta() - 'A' + 1) % 4);
                double tempo = aleatorio.nextDouble(1, 16);
                respostas.add(new Resposta(String.valueOf(alternativa), Math.min(15, tempo), tempo >= 15));
            }
            jogadores.add(new JogadorSimulado("SIM-" + (i + 1), respostas));
        }
        return List.copyOf(jogadores);
    }

    public Resultado executar(List<Pergunta> perguntas, int iteracoes) {
        if (iteracoes < 1 || perguntas.size() != respostas.size()) throw new IllegalArgumentException("Carga inválida.");
        Jogador jogador = new Jogador(nome);
        long verificacao = 0;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (int i = 0; i < perguntas.size(); i++) {
                if (Thread.currentThread().isInterrupted()) throw new IllegalStateException("Simulação interrompida.");
                Pergunta pergunta = perguntas.get(i);
                Resposta prevista = respostas.get(i);
                // Carga de CPU artificial, igual nas duas versões; sem espera humana ou sleep.
                byte[] dados = (nome + pergunta.identificador()).getBytes(StandardCharsets.UTF_8);
                for (int j = 0; j < iteracoes; j++) dados = digest.digest(dados);
                for (byte valor : dados) verificacao = verificacao * 31 + valor;
                jogador.registarResposta(Resposta.avaliar(jogador, pergunta, prevista.alternativa(),
                        prevista.tempoSegundos(), prevista.tempoEsgotado()));
            }
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 indisponível.", e); }
        return new Resultado(jogador, verificacao);
    }
}
