package quizrace.benchmark;

import quizrace.service.*;
import quizrace.util.MedidorDesempenho;
import java.util.*;

public final class BenchmarkRunner {
    public static void main(String[] args) throws Exception {
        int threads = args.length > 0 ? Integer.parseInt(args[0]) : Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors()));
        int repeticoes = args.length > 1 ? Integer.parseInt(args[1]) : 5;
        int iteracoes = args.length > 2 ? Integer.parseInt(args[2]) : 1000;
        if (threads < 1 || threads > 256 || repeticoes < 1 || iteracoes < 1) throw new IllegalArgumentException("Parâmetros inválidos.");
        var perguntas = new GerenciadorPerguntas().carregar();
        System.out.printf("=== BENCHMARK QUIZRACE ===%nJava: %s | CPU disponíveis: %d | Threads: %d%n", System.getProperty("java.version"), Runtime.getRuntime().availableProcessors(), threads);
        System.out.printf("Carga: %d perguntas, %d SHA-256 por pergunta | 1 warm-up + %d medições%n", perguntas.size(), iteracoes, repeticoes);
        System.out.println("Jogadores;Sequencial_ms;Paralelo_ms;Speedup;Eficiencia");
        for (int quantidade : new int[]{10, 100, 500, 1000}) {
            var jogadores = JogadorSimulado.gerar(quantidade, perguntas, 42);
            try (var paralelo = new QuizParalelo(threads)) {
                verificar(QuizSequencial.executarSimulados(jogadores, perguntas, iteracoes), paralelo.executar(jogadores, perguntas, iteracoes));
                double somaSequencial = 0, somaParalelo = 0;
                for (int i = 0; i < repeticoes; i++) {
                    var tempoSequencial = new MedidorDesempenho();
                    var tempoParalelo = new MedidorDesempenho();
                    List<JogadorSimulado.Resultado> sequenciais, paralelos;
                    // Alternar a ordem reduz o favorecimento sistemático de uma versão por aquecimento.
                    if (i % 2 == 0) {
                        tempoSequencial.iniciar(); sequenciais = QuizSequencial.executarSimulados(jogadores, perguntas, iteracoes); tempoSequencial.terminar();
                        tempoParalelo.iniciar(); paralelos = paralelo.executar(jogadores, perguntas, iteracoes); tempoParalelo.terminar();
                    } else {
                        tempoParalelo.iniciar(); paralelos = paralelo.executar(jogadores, perguntas, iteracoes); tempoParalelo.terminar();
                        tempoSequencial.iniciar(); sequenciais = QuizSequencial.executarSimulados(jogadores, perguntas, iteracoes); tempoSequencial.terminar();
                    }
                    verificar(sequenciais, paralelos);
                    somaSequencial += tempoSequencial.getMilissegundos();
                    somaParalelo += tempoParalelo.getMilissegundos();
                }
                var resultado = new ResultadoBenchmark(quantidade, somaSequencial / repeticoes, somaParalelo / repeticoes, threads);
                System.out.printf(Locale.ROOT, "%d;%.3f;%.3f;%.3f;%.3f%n", quantidade, resultado.sequencialMs(), resultado.paraleloMs(), resultado.speedup(), resultado.eficiencia());
            }
        }
    }

    public static void verificar(List<JogadorSimulado.Resultado> a, List<JogadorSimulado.Resultado> b) {
        if (a.size() != b.size()) throw new IllegalStateException("Resultados diferentes.");
        for (int i = 0; i < a.size(); i++) {
            var x = a.get(i); var y = b.get(i);
            if (x.verificacao() != y.verificacao() || !x.jogador().getNome().equals(y.jogador().getNome())
                    || Double.compare(x.jogador().getPontuacao(), y.jogador().getPontuacao()) != 0
                    || Double.compare(x.jogador().getTempoTotal(), y.jogador().getTempoTotal()) != 0
                    || x.jogador().getRespostasCorrectas() != y.jogador().getRespostasCorrectas()
                    || x.jogador().getRespostasErradas() != y.jogador().getRespostasErradas()
                    || x.jogador().getTimeouts() != y.jogador().getTimeouts()) throw new IllegalStateException("Resultados diferentes.");
        }
    }
}
