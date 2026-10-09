package quizrace.server;

/** Permite testar a sala sem abrir sockets. */
public interface CanalJogador {
    void enviarMensagem(String mensagem);
    void fechar();
}
