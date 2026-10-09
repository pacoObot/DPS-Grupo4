package quizrace.util;

public final class MedidorDesempenho {
    private long inicio;
    private long nanos;
    private boolean iniciado;
    private boolean terminado;

    public void iniciar() { inicio = System.nanoTime(); iniciado = true; terminado = false; }
    public void terminar() {
        if (!iniciado || terminado) throw new IllegalStateException("Medição não está activa.");
        nanos = System.nanoTime() - inicio;
        terminado = true;
    }
    public long getNanos() {
        if (!terminado) throw new IllegalStateException("Termine a medição primeiro.");
        return nanos;
    }
    public double getMilissegundos() { return getNanos() / 1_000_000.0; }
    public double medir(Runnable tarefa) {
        iniciar();
        tarefa.run();
        terminar();
        return getNanos() / 1_000_000_000.0;
    }
}
