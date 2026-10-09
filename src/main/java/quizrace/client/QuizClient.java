package quizrace.client;

import quizrace.network.*;
import quizrace.util.EntradaConsole;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import static quizrace.network.ProtocoloMensagem.Tipo.*;

public final class QuizClient {
    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : ConfiguracaoRede.HOST;
        EntradaConsole terminal = new EntradaConsole(System.in);
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, ConfiguracaoRede.PORTA), ConfiguracaoRede.LIGACAO_MS);
            System.out.println("Ligado a " + host + ":" + ConfiguracaoRede.PORTA);
            try (var entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 var saida = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
                // A recepção continua activa mesmo quando o utilizador não escreve no terminal.
                Thread envio = new Thread(() -> enviarDoTerminal(socket, terminal, saida), "envio-cliente");
                envio.setDaemon(true);
                envio.start();
                try {
                    String linha;
                    while ((linha = entrada.readLine()) != null) {
                        var mensagem = ProtocoloMensagem.interpretar(linha);
                        System.out.println(linha);
                        if (mensagem.tipo() == FIM) break;
                    }
                } finally {
                    socket.close();
                    envio.join(1000);
                }
            }
            System.out.println("Ligação encerrada.");
        } catch (IOException | IllegalArgumentException | IllegalStateException e) {
            System.err.println("Cliente: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void enviarDoTerminal(Socket socket, EntradaConsole terminal, BufferedWriter saida) {
        var tipo = NOME;
        System.out.println("Nome do jogador (depois escreva uma mensagem de teste e a resposta A–D):");
        try {
            while (!socket.isClosed()) {
                String linha = terminal.ler(200_000_000);
                if (linha == null) continue;
                if (linha.equalsIgnoreCase("/sair")) { socket.close(); return; }
                try {
                    String mensagem = ProtocoloMensagem.construir(tipo, linha.trim());
                    saida.write(mensagem);
                    saida.newLine();
                    saida.flush();
                    tipo = tipo == NOME ? MENSAGEM : RESPOSTA;
                } catch (IllegalArgumentException e) { System.out.println(e.getMessage()); }
            }
        } catch (IOException | IllegalStateException e) {
            if (!socket.isClosed()) {
                System.out.println("Envio terminado: " + e.getMessage());
                try { socket.shutdownOutput(); }
                catch (IOException ignorado) { /* A ligação pode já estar encerrada. */ }
            }
        }
    }
}
