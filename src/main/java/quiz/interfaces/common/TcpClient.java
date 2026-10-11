package quiz.interfaces.common;

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
 * TcpClient — cliente TCP partilhado pelas interfaces (Admin, Player, Bots).
 *
 *  Robustez:
 *   - ligação com timeout (não fica pendurado num host inexistente);
 *   - leitura LIMITADA por linha (evita esgotar memória com uma linha enorme);
 *   - sanitização de "\n"/"\r" no envio (o protocolo é \n-framed — impede
 *     que um valor com quebra de linha injete mensagens);
 *   - onClosed() só é entregue quando a quebra é involuntária (não num
 *     fecho deliberado pela própria aplicação).
 */
public final class TcpClient {

    public interface Listener {
        void onMessage(String line);
        void onClosed(String reason);
    }

    private static final Logger log = LoggerFactory.getLogger(TcpClient.class);

    /** Limite de uma linha recebida (1 MiB) — defensivo. */
    private static final int MAX_LINE_CHARS = 1 << 20;
    /** Timeout de estabelecimento de ligação (ms). */
    private static final int CONNECT_TIMEOUT_MS = 8000;

    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private volatile Listener listener;
    private volatile boolean open;
    private volatile boolean closedByUs;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public synchronized void connect(String host, int port) throws IOException {
        close();
        closedByUs = false;
        Socket s = new Socket();
        s.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
        s.setTcpNoDelay(true);
        socket = s;
        in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
        out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8));
        open = true;

        Thread reader = new Thread(this::readLoop, "tcp-client-reader");
        reader.setDaemon(true);
        reader.start();
    }

    private void readLoop() {
        String reason = "ligação terminada";
        try {
            String line;
            while ((line = readLineBounded()) != null) {
                Listener l = listener;
                if (l != null) l.onMessage(line);
            }
        } catch (IOException e) {
            reason = e.getMessage() == null ? "erro de rede" : e.getMessage();
            log.debug("leitura terminou: {}", reason);
        } finally {
            open = false;
            if (!closedByUs) {
                Listener l = listener;
                if (l != null) l.onClosed(reason);
            }
        }
    }

    /**
     * Lê uma linha com limite de tamanho. Devolve null no fim do stream.
     * Lança IOException se a linha exceder {@link #MAX_LINE_CHARS}.
     */
    private String readLineBounded() throws IOException {
        if (in == null) return null;
        StringBuilder sb = new StringBuilder(128);
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\n') return sb.toString();
            if (c == '\r') continue;
            sb.append((char) c);
            if (sb.length() > MAX_LINE_CHARS) {
                throw new IOException("linha excede o limite de " + MAX_LINE_CHARS + " caracteres");
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    /** Envio síncrono (uma linha por mensagem). Quebras de linha são removidas. */
    public synchronized void send(String message) {
        if (!open || out == null || message == null) return;
        String safe = message.replace('\n', ' ').replace('\r', ' ');
        try {
            out.write(safe);
            out.newLine();
            out.flush();
        } catch (IOException e) {
            log.warn("Falha ao enviar: {}", e.getMessage());
            open = false;
        }
    }

    /** Fecho deliberado (não dispara onClosed). */
    public synchronized void close() {
        closedByUs = true;
        open = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        }
    }

    public boolean isOpen() {
        return open && socket != null && !socket.isClosed();
    }
}