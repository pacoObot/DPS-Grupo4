package quizrace.util;

public final class MedidorDesempenho {
    public double medir(Runnable tarefa) {
        Cronometro cronometro = new Cronometro();
        tarefa.run();
        return cronometro.segundosDecorridos();
    }
}
