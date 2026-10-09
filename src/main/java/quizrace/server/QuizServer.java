package quizrace.server;

import quizrace.network.ConfiguracaoRede;
import quizrace.service.GerenciadorPerguntas;
import java.io.IOException;
import java.net.*;
import java.util.*;

public final class QuizServer implements AutoCloseable {
    private final ServerSocket servidor;
    private final Set<Socket> clientes = new HashSet<>();

    public QuizServer() throws IOException {
        servidor = new ServerSocket(ConfiguracaoRede.PORTA);
    }

    public void executar() throws IOException {
        var pergunta = new GerenciadorPerguntas().carregar().get(0);
        System.out.println("QuizServer à espera de clientes na porta " + servidor.getLocalPort());
        while (!servidor.isClosed()) {
            Socket cliente;
            try { cliente = servidor.accept(); }
            catch (SocketException e) {
                if (servidor.isClosed()) break;
                throw e;
            }
            // O registo só é partilhado para fechar as ligações durante o encerramento.
            synchronized (clientes) {
                if (servidor.isClosed()) { cliente.close(); break; }
                clientes.add(cliente);
            }
            System.out.println("Cliente ligado: " + cliente.getRemoteSocketAddress());
            Thread atendimento = new Thread(() -> {
                try { new ClienteHandler(cliente, pergunta).run(); }
                finally { synchronized (clientes) { clientes.remove(cliente); } }
            }, "cliente-" + cliente.getPort());
            atendimento.setDaemon(true);
            atendimento.start();
        }
    }

    @Override public void close() throws IOException {
        servidor.close();
        synchronized (clientes) {
            for (Socket cliente : clientes) {
                try { cliente.close(); }
                catch (IOException e) { System.err.println("Erro ao fechar cliente: " + e.getMessage()); }
            }
            clientes.clear();
        }
    }

    public static void main(String[] args) {
        try (QuizServer servidor = new QuizServer()) {
            Thread encerramento = new Thread(() -> {
                try { servidor.close(); }
                catch (IOException e) { System.err.println("Erro ao encerrar servidor: " + e.getMessage()); }
            }, "encerramento-servidor");
            Runtime.getRuntime().addShutdownHook(encerramento);
            try { servidor.executar(); }
            finally {
                try { Runtime.getRuntime().removeShutdownHook(encerramento); }
                catch (IllegalStateException e) { /* O encerramento da JVM já começou. */ }
            }
        } catch (IOException e) {
            System.err.println("Não foi possível manter o servidor: " + e.getMessage());
        }
    }
}
