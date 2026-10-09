package quizrace.server;

import quizrace.model.*;
import quizrace.network.*;
import quizrace.service.CalculadoraPontuacao;
import quizrace.util.Cronometro;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import static quizrace.network.ProtocoloMensagem.Tipo.*;

public final class ClienteHandler implements Runnable {
    private final Socket socket;
    private final Pergunta pergunta;

    public ClienteHandler(Socket socket, Pergunta pergunta) {
        this.socket = socket;
        this.pergunta = pergunta;
    }

    @Override public void run() {
        try (socket;
             var entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             var saida = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            socket.setSoTimeout(ConfiguracaoRede.ESPERA_INICIAL_MS);
            enviar(saida, MENSAGEM, "Envie o seu nome para iniciar.");
            var identificacao = receber(entrada);
            if (identificacao.tipo() != NOME) throw new IllegalArgumentException("A primeira mensagem deve ser NOME.");
            Jogador jogador = new Jogador(identificacao.campo(0));
            System.out.println("Jogador registado: " + jogador.getNome());
            enviar(saida, MENSAGEM, "Bem-vindo, " + jogador.getNome() + "!");
            enviar(saida, MENSAGEM, "Escreva uma mensagem para testar a ligação.");
            var mensagem = receber(entrada);
            if (mensagem.tipo() != MENSAGEM) throw new IllegalArgumentException("Esperada uma MENSAGEM de teste.");
            System.out.println(jogador.getNome() + " enviou: " + mensagem.campo(0));
            enviar(saida, MENSAGEM, "Recebido: " + mensagem.campo(0));
            enviar(saida, PERGUNTA, pergunta.enunciado());
            for (int i = 0; i < 4; i++) enviar(saida, OPCAO, String.valueOf((char) ('A' + i)), pergunta.alternativas().get(i));
            enviar(saida, MENSAGEM, "Responda A, B, C ou D em 15 segundos.");
            Cronometro cronometro = new Cronometro();
            Resposta resposta;
            while (true) {
                double restante = Pergunta.LIMITE_SEGUNDOS - cronometro.segundosDecorridos();
                if (restante <= 0) {
                    resposta = new Resposta("", Pergunta.LIMITE_SEGUNDOS, true);
                    break;
                }
                socket.setSoTimeout(Math.max(1, (int) Math.ceil(restante * 1000)));
                try {
                    resposta = ProtocoloMensagem.paraResposta(receber(entrada), cronometro.segundosDecorridos());
                    break;
                } catch (SocketTimeoutException e) {
                    resposta = new Resposta("", Pergunta.LIMITE_SEGUNDOS, true);
                    break;
                } catch (IllegalArgumentException e) {
                    enviar(saida, ERRO, e.getMessage());
                }
            }
            boolean correcta = pergunta.estaCorrecta(resposta);
            double tempo = Math.min(resposta.tempoSegundos(), Pergunta.LIMITE_SEGUNDOS);
            double pontos = new CalculadoraPontuacao().calcular(correcta, tempo);
            jogador.registarResposta(correcta, pontos, tempo);
            enviar(saida, RESULTADO, resposta.tempoEsgotado() ? "TEMPO_ESGOTADO" : correcta ? "CORRECTO" : "ERRADO",
                    String.format(Locale.ROOT, "%.2f", jogador.getPontuacao()));
            enviar(saida, FIM, "Teste concluído. Obrigado, " + jogador.getNome() + "!");
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("Ligação " + socket.getRemoteSocketAddress() + " terminada: " + e.getMessage());
        } finally {
            System.out.println("Cliente desligado: " + socket.getRemoteSocketAddress());
        }
    }

    private static ProtocoloMensagem.Mensagem receber(BufferedReader entrada) throws IOException {
        String linha = entrada.readLine();
        if (linha == null) throw new EOFException("O cliente desligou-se.");
        return ProtocoloMensagem.interpretar(linha);
    }

    private static void enviar(BufferedWriter saida, ProtocoloMensagem.Tipo tipo, String... campos) throws IOException {
        saida.write(ProtocoloMensagem.construir(tipo, campos));
        saida.newLine();
        saida.flush();
    }
}
