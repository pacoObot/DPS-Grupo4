package quizrace.server;

import quizrace.network.ConfiguracaoRede;
import quizrace.model.Pergunta;
import quizrace.service.GerenciadorPerguntas;
import java.io.IOException;
import java.net.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

public final class QuizServer implements AutoCloseable {
    private final ServerSocket servidor;
    private final SalaQuiz sala;
    private final Set<Socket> clientes = ConcurrentHashMap.newKeySet();
    // Oito atendimentos cobrem quatro jogadores e ligações em identificação; não há fila ilimitada.
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(8, 8, 0, TimeUnit.SECONDS,
            new SynchronousQueue<>(), new ThreadPoolExecutor.AbortPolicy());

    public QuizServer() throws IOException { this(ConfiguracaoRede.porta(null)); }
    public QuizServer(int porta) throws IOException {
        this(porta, new GerenciadorPerguntas().carregar(), Duration.ofSeconds(15), Duration.ofSeconds(1));
    }
    public QuizServer(int porta, List<Pergunta> perguntas, Duration prazo, Duration contagem) throws IOException {
        sala = new SalaQuiz(perguntas, prazo, contagem);
        try { servidor = new ServerSocket(porta); }
        catch (IOException e) { sala.close(); executor.shutdownNow(); throw e; }
    }
    public int getPorta() { return servidor.getLocalPort(); }
    public SalaQuiz getSala() { return sala; }

    public void executar() throws IOException {
        System.out.println("[SERVER] QuizRace iniciado na porta " + getPorta());
        while (!servidor.isClosed()) {
            Socket cliente;
            try { cliente = servidor.accept(); }
            catch (SocketException e) { if (servidor.isClosed()) break; throw e; }
            synchronized (clientes) {
                if (servidor.isClosed()) { cliente.close(); break; }
                clientes.add(cliente);
            }
            System.out.println("[CONEXAO] Cliente conectado: " + cliente.getRemoteSocketAddress());
            try {
                ClienteHandler handler = new ClienteHandler(cliente, sala);
                executor.execute(() -> {
                    try { handler.run(); }
                    finally { clientes.remove(cliente); }
                });
            } catch (RejectedExecutionException | IOException e) {
                clientes.remove(cliente);
                cliente.close();
                System.out.println("[CONEXAO] Atendimento indisponível: " + e.getMessage());
            }
        }
    }

    @Override public void close() throws IOException {
        servidor.close();
        // Fechar os sockets desbloqueia read(); interromper uma thread sozinha não basta.
        synchronized (clientes) {
            for (Socket cliente : clientes) {
                try { cliente.close(); } catch (IOException e) { System.err.println(e.getMessage()); }
            }
        }
        sala.close();
        executor.shutdownNow();
        try { executor.awaitTermination(3, TimeUnit.SECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    public static void main(String[] args) {
        try (QuizServer servidor = new QuizServer(ConfiguracaoRede.porta(args.length > 0 ? args[0] : null))) {
            Thread encerramento = new Thread(() -> {
                try { servidor.close(); } catch (IOException e) { System.err.println(e.getMessage()); }
            }, "encerramento-servidor");
            Runtime.getRuntime().addShutdownHook(encerramento);
            try { servidor.executar(); }
            finally {
                try { Runtime.getRuntime().removeShutdownHook(encerramento); }
                catch (IllegalStateException e) { /* A JVM já está a encerrar. */ }
            }
        } catch (IOException | IllegalArgumentException e) { System.err.println("[SERVER] " + e.getMessage()); }
    }
}
