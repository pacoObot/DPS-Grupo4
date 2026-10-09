package quizrace;

import quizrace.model.*;
import quizrace.service.*;
import quizrace.util.EntradaConsole;
import java.io.IOException;
import java.util.*;

public final class Main {
    public static void main(String[] args) throws Exception {
        EntradaConsole entrada = new EntradaConsole(System.in);
        if (args.length > 0 && args[0].equals("--local")) { jogarLocal(entrada); return; }
        try {
            System.out.println("=== QUIZRACE ===\n1 - Jogar versão local sequencial\n2 - Iniciar servidor\n3 - Iniciar cliente\n4 - Executar benchmark\n0 - Sair");
            switch (entrada.ler().trim()) {
                case "1" -> jogarLocal(entrada);
                case "2" -> quizrace.server.QuizServer.main(new String[0]);
                case "3" -> quizrace.client.QuizClient.executar(new String[0], entrada);
                case "4" -> quizrace.benchmark.BenchmarkRunner.main(new String[0]);
                case "0" -> { }
                default -> System.out.println("Opção inválida. Execute novamente para escolher.");
            }
        } catch (IllegalStateException e) { System.out.println(e.getMessage()); }
    }

    private static void jogarLocal(EntradaConsole entrada) {
        System.out.println("=== QuizRace — versão sequencial ===");
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
