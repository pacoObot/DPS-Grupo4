package quizrace.service;

import quizrace.benchmark.JogadorSimulado;
import quizrace.model.Pergunta;
import quizrace.parallel.JogadorTask;
import java.util.*;
import java.util.concurrent.*;

public final class QuizParalelo implements AutoCloseable {
    private final ExecutorService executor;
    public QuizParalelo(int threads) {
        if (threads < 1) throw new IllegalArgumentException("Número de threads inválido.");
        executor = Executors.newFixedThreadPool(threads);
    }

    public List<JogadorSimulado.Resultado> executar(List<JogadorSimulado> jogadores, List<Pergunta> perguntas, int iteracoes) {
        var tarefas = jogadores.stream().map(j -> new JogadorTask(j, perguntas, iteracoes)).toList();
        List<JogadorSimulado.Resultado> resultados = new ArrayList<>();
        try {
            // invokeAll aguarda todas as tarefas; get publica os resultados de forma segura.
            for (var futuro : executor.invokeAll(tarefas)) resultados.add(futuro.get());
            return resultados;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Execução interrompida.", e);
        } catch (ExecutionException e) { throw new IllegalStateException("Falha numa tarefa.", e.getCause()); }
    }
    @Override public void close() {
        executor.shutdownNow();
        try { executor.awaitTermination(5, TimeUnit.SECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
