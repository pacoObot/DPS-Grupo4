package quizrace;

import quizrace.model.*;
import quizrace.service.*;
import quizrace.util.EntradaConsole;
import java.io.IOException;
import java.util.*;

public final class Main {
    public static void main(String[] args) {
        System.out.println("=== QuizRace — versão sequencial ===");
        EntradaConsole entrada = new EntradaConsole(System.in);
        try {
            List<Pergunta> perguntas = new GerenciadorPerguntas().carregar();
            int quantidade = 0;
            while (quantidade < 2 || quantidade > 4) {
                System.out.println("Número de jogadores (2 a 4):");
                try { quantidade = Integer.parseInt(entrada.ler().trim()); }
                catch (NumberFormatException e) { quantidade = 0; }
                if (quantidade < 2 || quantidade > 4) System.out.println("Indique um número entre 2 e 4.");
            }
            List<Jogador> jogadores = new ArrayList<>();
            while (jogadores.size() < quantidade) {
                System.out.println("Nome do jogador " + (jogadores.size() + 1) + ":");
                String nome = entrada.ler().trim();
                if (nome.isBlank() || jogadores.stream().anyMatch(j -> j.getNome().equalsIgnoreCase(nome))) {
                    System.out.println("Use um nome não vazio e diferente dos anteriores.");
                } else jogadores.add(new Jogador(nome));
            }
            double duracao = new QuizSequencial(entrada, System.out).executar(jogadores, perguntas);
            System.out.println("\n=== Ranking ===");
            System.out.printf("%-8s %-20s %10s %10s %8s %12s%n", "Posição", "Nome", "Pontuação", "Correctas", "Erradas", "Tempo (s)");
            int posicao = 1;
            for (Jogador jogador : new RankingService().ordenar(jogadores)) {
                System.out.printf("%-8d %-20s %10.2f %10d %8d %12.2f%n", posicao++, jogador.getNome(), jogador.getPontuacao(), jogador.getRespostasCorrectas(), jogador.getRespostasErradas(), jogador.getTempoTotal());
            }
            System.out.printf("Tempo total da versão sequencial: %.3f s%n", duracao);
        } catch (IOException | IllegalStateException e) {
            System.out.println("Jogo interrompido: " + e.getMessage());
        }
    }
}
