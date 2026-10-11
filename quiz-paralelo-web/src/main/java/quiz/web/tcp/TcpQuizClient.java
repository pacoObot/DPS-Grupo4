package quiz.web.tcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * TcpQuizClient — ligação TCP (texto, \n-framed) ao servidor do quiz.
 *
 *  Usada uma vez por cada jogador web: o gateway abre esta ligação e
 *  reencaminha mensagens para o browser via WebSocket.
 *
 *  Robustez: timeout de ligação, leitura limitada por linha (evita OOM)
 *  e notificação de fecho involuntário.
 */
public final class TcpQuizClient {

    public interface Listener {
        void onLine(String line);
        void onClosed(String reason);
    }

    private static final Logger log = LoggerFactory.getLogger(TcpQuizClient.class);

    private static final int MAX_LINE_CHARS = 1 << 20;
    private static final int CONNECT_TIMEOUT_MS = 8000;

    private final String host;
    private final int port;
    private final Listener listener;

    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private volatile boolean open;
    private volatile boolean closedByUs;

    public TcpQuizClient(String host, int port, Listener listener) {
        this.host = host;
        this.port = port;
        this.listener = listener;
    }

    public synchronized void connect() throws IOException {
        Socket s = new Socket();
        s.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
        s.setTcpNoDelay(true);
        socket = s;
        in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
        out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8));
        open = true;

        Thread reader = new Thread(this::readLoop, "web-tcp-reader");
        reader.setDaemon(true);
        reader.start();
    }

    private void readLoop() {
        String reason = "ligação terminada";
        try {
            String line;
            while ((line = readLineBounded()) != null) {
                listener.onLine(line);
            }
        } catch (IOException e) {
            reason = e.getMessage() == null ? "erro de rede" : e.getMessage();
            log.debug("leitura terminou: {}", reason);
        } finally {
            open = false;
            if (!closedByUs) listener.onClosed(reason);
        }
    }

    private String readLineBounded() throws IOException {
        if (in == null) return null;
        StringBuilder sb = new StringBuilder(128);
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\n') return sb.toString();
            if (c == '\r') continue;
            sb.append((char) c);
            if (sb.length() > MAX_LINE_CHARS) {
                throw new IOException("linha excede o limite de " + MAX_LINE_CHARS);
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    public synchronized void send(String message) {
        if (!open || out == null || message == null) return;
        String safe = message.replace('\n', ' ').replace('\r', ' ');
        try {
            out.write(safe);
            out.newLine();
            out.flush();
        } catch (IOException e) {
            log.warn("falha ao enviar: {}", e.getMessage());
            open = false;
        }
    }

    public synchronized void close() {
        closedByUs = true;
        open = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        }
    }

    public boolean isOpen() {
        return open;
    }
}