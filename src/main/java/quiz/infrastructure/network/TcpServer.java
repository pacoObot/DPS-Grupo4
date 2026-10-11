package quiz.infrastructure.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quiz.application.service.ConcurrencyService;
import quiz.infrastructure.concurrency.ClientThreadPool;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * =====================================================================
 *  TcpServer — ponto de entrada TCP do sistema
 * =====================================================================
 *
 *  O accept() é o QUE recebe novas conexões:
 *
 *      ServerSocket ss = new ServerSocket(PORTA);
 *      while (true) {
 *          Socket s = ss.accept();     // devolve IMEDIATAMENTE quando um
 *          pool.submit(handler);       // cliente TCP conecta; a partir daí
 *      }                               // o handler corre noutra thread
 *
 *  Sem o pool, teríamos que processar um cliente de cada vez (sequencial).
 *  Com o pool, N clientes são atendidos CONCORRENTEMENTE.
 *
 *  Limites de segurança:
 *   - tamanho máximo do pool (QUIZ_THREADS)
 *   - SO_TIMEOUT por ligação ociosa
 *   - ProtocolHandler valida todas as mensagens
 */
public final class TcpServer {

    private static final Logger log = LoggerFactory.getLogger(TcpServer.class);

    private final int port;
    private final ClientThreadPool pool;
    private final ClientRegistry registry;
    private final AdminClientHandler adminHandler;
    private final PlayerClientHandler playerHandler;
    private final ConcurrencyService concurrency;

    private volatile ServerSocket serverSocket;
    private volatile boolean running;

    public TcpServer(int port, ClientThreadPool pool, ClientRegistry registry,
                     AdminClientHandler adminHandler, PlayerClientHandler playerHandler,
                     ConcurrencyService concurrency) {
        this.port = port;
        this.pool = pool;
        this.registry = registry;
        this.adminHandler = adminHandler;
        this.playerHandler = playerHandler;
        this.concurrency = concurrency;
    }

    /** Loop de accept() — corre na thread "accept-loop". */
    public void start() throws IOException {
        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(port));
        running = true;

        Thread acceptThread = new Thread(this::acceptLoop, "accept-loop");
        acceptThread.setDaemon(false);
        acceptThread.start();

        log.info("Servidor TCP iniciado na porta {} (pool de {} threads)",
                port, pool.size());
    }

    private void acceptLoop() {
        while (running) {
            try {
                // ===== ACEITA UMA NOVA CONEXÃO TCP =====
                // Bloqueia até um cliente conectar. Em seguida, submete o
                // atendimento ao pool — o loop volta a ouvir novos clientes
                // de imediato (multi-cliente, spec §5).
                Socket socket = serverSocket.accept();
                socket.setTcpNoDelay(true);          // latência mínima no quiz

                TcpConnection conn = new TcpConnection(socket);
                pool.submit(new ClientHandler(conn, registry, adminHandler,
                        playerHandler, concurrency));
            } catch (SocketException e) {
                if (running) log.warn("Socket fechado: {}", e.getMessage());
                return;
            } catch (IOException e) {
                if (running) log.error("Erro no accept(): {}", e.getMessage());
            }
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {
        }
        registry.closeAll();
        pool.shutdown();
        log.info("Servidor TCP encerrado");
    }

    public int port() { return port; }

    /** Porta REAL em uso (útil quando a porta pedida foi 0/aleatória — testes). */
    public int actualPort() {
        if (serverSocket == null || !serverSocket.isBound()) return port;
        return serverSocket.getLocalPort();
    }

    public boolean isRunning() { return running; }
}
