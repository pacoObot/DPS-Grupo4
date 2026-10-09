package quizrace.client;

import quizrace.network.*;
import quizrace.model.Pergunta;
import quizrace.service.GerenciadorPerguntas;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import static quizrace.network.ProtocoloMensagem.Tipo.*;

public final class ClienteSimulado {
    public record Configuracao(String nome, int atrasoMinMs, int atrasoMaxMs, double probabilidade) {
        public Configuracao {
            new quizrace.model.Jogador(nome);
            if (atrasoMinMs < 0 || atrasoMaxMs < atrasoMinMs || atrasoMaxMs > 60_000
                    || !Double.isFinite(probabilidade) || probabilidade < 0 || probabilidade > 1)
                throw new IllegalArgumentException("Atrasos ou probabilidade inválidos.");
        }
    }

    public static void main(String[] args) throws Exception {
        String host = ConfiguracaoRede.host(args.length > 0 ? args[0] : null);
        int porta = ConfiguracaoRede.porta(args.length > 1 ? args[1] : null);
        var config = new Configuracao(valor(args, 2, "QUIZ_NOME", "BOT-1"),
                Integer.parseInt(valor(args, 3, "QUIZ_ATRASO_MIN_MS", "100")),
                Integer.parseInt(valor(args, 4, "QUIZ_ATRASO_MAX_MS", "500")),
                Double.parseDouble(valor(args, 5, "QUIZ_ACERTO", "0.75")));
        executar(host, porta, config);
    }

    private static String valor(String[] args, int indice, String variavel, String padrao) {
        return args.length > indice ? args[indice] : System.getenv().getOrDefault(variavel, padrao);
    }

    public static void executar(String host, int porta, Configuracao config) throws Exception {
        var perguntas = new GerenciadorPerguntas().carregar();
        Random aleatorio = new Random(config.nome().hashCode());
        ScheduledExecutorService atrasos = Executors.newSingleThreadScheduledExecutor();
        try (Socket socket = ligar(host, porta)) {
            socket.setSoTimeout(60_000);
            var entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            var saida = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            enviar(saida, ProtocoloMensagem.construir(NOME, config.nome()));
            int perguntaId = 0;
            String enunciado = "";
            ScheduledFuture<?> respostaPendente = null;
            boolean terminou = false;
            String linha;
            while ((linha = ProtocoloMensagem.lerLinha(entrada)) != null) {
                System.out.println("[" + config.nome() + "] " + linha);
                var mensagem = ProtocoloMensagem.interpretar(linha);
                if (mensagem.tipo() == PERGUNTA) {
                    if (respostaPendente != null) respostaPendente.cancel(false);
                    perguntaId = Integer.parseInt(mensagem.campo(0));
                    enunciado = mensagem.campo(1);
                } else if (mensagem.tipo() == TEMPO) {
                    int id = perguntaId;
                    String texto = enunciado;
                    // O bot conhece o banco de demonstração, mas nunca envia pontos nem a resposta correcta ao servidor.
                    Optional<Pergunta> pergunta = perguntas.stream().filter(p -> p.enunciado().equals(texto)).findFirst();
                    char alternativa = (char) ('A' + aleatorio.nextInt(4));
                    if (pergunta.isPresent()) {
                        char correcta = pergunta.get().respostaCorrecta();
                        alternativa = aleatorio.nextDouble() < config.probabilidade() ? correcta
                                : (char) ('A' + (correcta - 'A' + 1 + aleatorio.nextInt(3)) % 4);
                    }
                    String resposta = ProtocoloMensagem.construir(RESPOSTA, String.valueOf(id), String.valueOf(alternativa));
                    int atraso = config.atrasoMinMs() + aleatorio.nextInt(config.atrasoMaxMs() - config.atrasoMinMs() + 1);
                    respostaPendente = atrasos.schedule(() -> {
                        try { enviar(saida, resposta); }
                        catch (IOException e) { try { socket.close(); } catch (IOException ignorado) { } }
                    }, atraso, TimeUnit.MILLISECONDS);
                } else if (mensagem.tipo() == RESULTADO && respostaPendente != null) respostaPendente.cancel(false);
                else if (mensagem.tipo() == FIM) { terminou = true; break; }
            }
            if (!terminou) throw new EOFException("Servidor desligado antes do fim da partida.");
        } finally {
            atrasos.shutdownNow();
            atrasos.awaitTermination(2, TimeUnit.SECONDS);
        }
    }

    private static void enviar(BufferedWriter saida, String mensagem) throws IOException {
        synchronized (saida) { saida.write(mensagem); saida.newLine(); saida.flush(); }
    }
    private static Socket ligar(String host, int porta) throws IOException, InterruptedException {
        IOException ultima = null;
        for (int i = 0; i < 40; i++) {
            Socket socket = new Socket();
            try { socket.connect(new InetSocketAddress(host, porta), ConfiguracaoRede.LIGACAO_MS); return socket; }
            catch (IOException e) { ultima = e; socket.close(); Thread.sleep(250); }
        }
        throw new IOException("Não foi possível ligar o bot ao servidor.", ultima);
    }
}
