package quizrace.parallel;

import quizrace.benchmark.JogadorSimulado;
import quizrace.model.Pergunta;
import java.util.List;
import java.util.concurrent.Callable;

/** A tarefa possui um jogador exclusivo; os resultados só são lidos depois de Future.get(). */
public final class JogadorTask implements Callable<JogadorSimulado.Resultado> {
    private final JogadorSimulado jogador;
    private final List<Pergunta> perguntas;
    private final int iteracoes;

    public JogadorTask(JogadorSimulado jogador, List<Pergunta> perguntas, int iteracoes) {
        this.jogador = jogador;
        this.perguntas = perguntas;
        this.iteracoes = iteracoes;
    }
    @Override public JogadorSimulado.Resultado call() { return jogador.executar(perguntas, iteracoes); }
}
