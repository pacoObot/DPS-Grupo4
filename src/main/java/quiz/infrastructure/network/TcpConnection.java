package quiz.infrastructure.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.UUID;

/**
 * TcpConnection — encapsula um socket TCP autenticado.
 *
 *  send() é synchronized: apenas uma thread escreve de cada vez por socket,
 *  evitando mensagens "entrecruzadas" (interleaving) quando, por exemplo,
 *  o GameScheduler faz broadcast e o AdminClientHandler responde em paralelo.
 */
public final class TcpConnection {

    private static final Logger log = LoggerFactory.getLogger(TcpConnection.class);

    private final String id = UUID.randomUUID().toString().substring(0, 8);
    private final Socket socket;
    private final BufferedReader in;
    private final BufferedWriter out;
    private final String remote;
    /** Nome da thread que REALMENTE atende esta ligação (definido no ClientHandler). */
    private volatile String threadName = "-";

    /** Papel e associações — mutáveis, mas volatile (lido por várias threads). */
    private volatile boolean admin;
    private volatile long teamId = -1;
    private volatile long gameId = -1;
    private volatile boolean open = true;

    public TcpConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        this.remote = String.valueOf(socket.getRemoteSocketAddress());
    }

    /** Chamado pela thread do pool que vai atender esta ligação. */
    public void attachThread(String name) {
        this.threadName = name;
    }

    public String readLine() throws IOException {
        return in.readLine();
    }

    /** Envio síncrono e seguro. */
    public synchronized void send(String message) {
        if (!open) return;
        try {
            out.write(message);
            out.newLine();
            out.flush();
        } catch (IOException e) {
            open = false;
            log.debug("Falha de envio para {}: {}", remote, e.getMessage());
        }
    }

    public void close() {
        open = false;
        try { socket.close(); } catch (IOException ignored) {}
    }

    public boolean isOpen() { return open && !socket.isClosed(); }
    public String id() { return id; }
    public String remote() { return remote; }
    public String threadName() { return threadName; }
    public boolean isAdmin() { return admin; }
    public void markAdmin() { this.admin = true; }
    public long teamId() { return teamId; }
    public void bindTeam(long teamId, long gameId) { this.teamId = teamId; this.gameId = gameId; }
    public long gameId() { return gameId; }
    public boolean isPlayer() { return teamId > 0; }

    @Override
    public String toString() {
        return "conn[" + id + " " + remote + " team=" + teamId + (admin ? " ADMIN" : "") + "]";
    }
}
