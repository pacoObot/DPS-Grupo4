package quizrace.server;

import quizrace.model.Jogador;
import quizrace.network.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import static quizrace.network.ProtocoloMensagem.Tipo.*;

public final class ClienteHandler implements Runnable, CanalJogador {
    private final Socket socket;
    private final SalaQuiz sala;
    private final BufferedWriter saida;
    private Jogador jogador;
    private boolean registado;

    public ClienteHandler(Socket socket, SalaQuiz sala) throws IOException {
        this.socket = socket;
        this.sala = sala;
        this.saida = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }

    @Override public void run() {
        try {
            var entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            socket.setSoTimeout(ConfiguracaoRede.ESPERA_INICIAL_MS);
            enviarMensagem(ProtocoloMensagem.construir(MENSAGEM, "Envie o seu nome para iniciar."));
            String linha = ProtocoloMensagem.lerLinha(entrada);
            if (linha == null) return;
            var nome = ProtocoloMensagem.interpretar(linha);
            if (nome.tipo() != NOME) throw new IllegalArgumentException("A primeira mensagem deve ser NOME.");
            jogador = new Jogador(nome.campo(0));
            sala.entrar(jogador, this);
            registado = true;
            socket.setSoTimeout(0);
            while ((linha = ProtocoloMensagem.lerLinha(entrada)) != null) {
                try {
                    var mensagem = ProtocoloMensagem.interpretar(linha);
                    if (mensagem.tipo() == MENSAGEM) enviarMensagem(ProtocoloMensagem.construir(MENSAGEM, "Recebido: " + mensagem.campo(0)));
                    else if (mensagem.tipo() != RESPOSTA || !sala.responder(jogador, mensagem))
                        enviarMensagem(ProtocoloMensagem.construir(ERRO, "Resposta duplicada, fora do prazo ou de outra pergunta; use RESPOSTA com identificador."));
                } catch (IllegalArgumentException e) { enviarErro(e.getMessage()); }
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            enviarErro(e.getMessage());
        } catch (IOException e) {
            if (!socket.isClosed()) System.out.println("[CONEXAO] " + e.getMessage());
        } finally {
            if (registado) sala.sair(jogador);
            fechar();
            System.out.println("[CONEXAO] Cliente desligado: " + socket.getRemoteSocketAddress());
        }
    }

    private void enviarErro(String erro) { enviarMensagem(ProtocoloMensagem.construir(ERRO, erro)); }

    @Override public synchronized void enviarMensagem(String mensagem) {
        if (socket.isClosed()) return;
        try { saida.write(mensagem); saida.newLine(); saida.flush(); }
        catch (IOException e) { fechar(); }
    }

    @Override public void fechar() {
        try { socket.close(); }
        catch (IOException e) { System.err.println("[CONEXAO] Erro ao fechar socket: " + e.getMessage()); }
    }
}
