package quizrace.client;

import quizrace.network.*;
import quizrace.util.EntradaConsole;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static quizrace.network.ProtocoloMensagem.Tipo.*;

public final class QuizClient {
    public static void main(String[] args) { executar(args, new EntradaConsole(System.in)); }

    public static void executar(String[] args, EntradaConsole terminal) {
        try (Socket socket = new Socket()) {
            String host = ConfiguracaoRede.host(args.length > 0 ? args[0] : null);
            int porta = ConfiguracaoRede.porta(args.length > 1 ? args[1] : null);
            socket.connect(new InetSocketAddress(host, porta), ConfiguracaoRede.LIGACAO_MS);
            System.out.println("Ligado a " + host + ":" + porta + ". Use /sair para desligar.");
            var entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            var saida = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            AtomicInteger perguntaAberta = new AtomicInteger();
            ExecutorService envio = Executors.newSingleThreadExecutor();
            envio.submit(() -> enviarDoTerminal(socket, terminal, saida, perguntaAberta));
            try {
                int pergunta = 0;
                String linha;
                while ((linha = ProtocoloMensagem.lerLinha(entrada)) != null) {
                    var mensagem = ProtocoloMensagem.interpretar(linha);
                    if (mensagem.tipo() == PERGUNTA) {
                        perguntaAberta.set(0);
                        pergunta = Integer.parseInt(mensagem.campo(0));
                    } else if (mensagem.tipo() == TEMPO) perguntaAberta.set(pergunta);
                    else if (mensagem.tipo() == RESULTADO || mensagem.tipo() == FIM) perguntaAberta.set(0);
                    mostrar(mensagem);
                    if (mensagem.tipo() == FIM) break;
                }
            } finally {
                socket.close();
                envio.shutdownNow();
                envio.awaitTermination(2, TimeUnit.SECONDS);
            }
            System.out.println("Ligação encerrada.");
        } catch (IOException | IllegalArgumentException e) { System.err.println("Cliente: " + e.getMessage()); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private static void mostrar(ProtocoloMensagem.Mensagem mensagem) {
        switch (mensagem.tipo()) {
            case PERGUNTA -> System.out.println("\nPergunta " + mensagem.campo(0) + ": " + mensagem.campo(1));
            case OPCAO -> System.out.println(mensagem.campo(0) + ") " + mensagem.campo(1));
            case TEMPO -> System.out.println("Responda A/B/C/D em " + mensagem.campo(0) + " segundos:");
            case RANKING -> System.out.printf("%s.º %s — %s pontos | correctas %s | erradas %s | timeouts %s | tempo %ss%n",
                    mensagem.campo(0), mensagem.campo(1), mensagem.campo(2), mensagem.campo(3), mensagem.campo(4), mensagem.campo(5), mensagem.campo(6));
            case VENCEDOR -> System.out.println("Vencedor: " + mensagem.campo(0) + " — " + mensagem.campo(1) + " pontos");
            default -> System.out.println(mensagem.tipo() + ": " + String.join(" — ", mensagem.campos()));
        }
    }

    private static void enviarDoTerminal(Socket socket, EntradaConsole terminal, BufferedWriter saida, AtomicInteger perguntaAberta) {
        boolean nomeEnviado = false;
        System.out.println("Nome do jogador:");
        try {
            while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
                String linha = terminal.ler(200_000_000);
                if (linha == null) continue;
                linha = linha.trim();
                if (linha.equalsIgnoreCase("/sair")) { socket.close(); return; }
                try {
                    String mensagem;
                    if (!nomeEnviado) mensagem = ProtocoloMensagem.construir(NOME, linha);
                    else {
                        if (!linha.matches("(?i)[ABCD]")) { System.out.println("Use A, B, C ou D."); continue; }
                        int id = perguntaAberta.getAndSet(0);
                        if (id == 0) { System.out.println("Aguarde a próxima pergunta ou o resultado."); continue; }
                        mensagem = ProtocoloMensagem.construir(RESPOSTA, String.valueOf(id), linha);
                    }
                    saida.write(mensagem); saida.newLine(); saida.flush();
                    nomeEnviado = true;
                } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }
            }
        } catch (IOException | IllegalStateException e) {
            if (!socket.isClosed()) {
                try { socket.shutdownOutput(); } catch (IOException ignorado) { }
            }
        }
    }
}
