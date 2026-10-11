package quiz.infrastructure.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quiz.application.service.ConcurrencyService;
import quiz.shared.protocol.Protocol;

import java.io.EOFException;
import java.io.IOException;

/**
 * =====================================================================
 *  ClientHandler — UMA THREAD POR LIGAÇÃO (unidade de paralelismo)
 * =====================================================================
 *
 *  O TcpServer faz accept() e submete este Runnable ao ExecutorService:
 *
 *     while (true) {
 *         Socket s = serverSocket.accept();          // bloqueia até cliente chegar
 *         pool.submit(new ClientHandler(s, ...));    // passa a atendê-lo NOUTRA thread
 *     }
 *
 *  A partir daqui, o accept() volta a ouvir imediatamente e as ligações
 *  existentes continuam a ser lidas em paralelo, cada uma na sua thread
 *  do pool ("quiz-pool-1-N").
 *
 *  Ciclo de vida:
 *    1. regista a ligação + thread no ConcurrencyService (monitor)
 *    2. primeira mensagem: ADMIN_LOGIN (admin) ou JOIN_GAME (jogador)
 *    3. loop de comandos -> AdminClientHandler / PlayerClientHandler
 *    4. EOF/erro -> desconexão tolerada: o jogo CONTINUA
 */
public final class ClientHandler implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);
    private static final int SO_TIMEOUT_MS = 300_000; // 5 min sem atividade => fecha

    private final TcpConnection conn;
    private final ClientRegistry registry;
    private final AdminClientHandler adminHandler;
    private final PlayerClientHandler playerHandler;
    private final ConcurrencyService concurrency;

    public ClientHandler(TcpConnection conn, ClientRegistry registry,
                         AdminClientHandler adminHandler, PlayerClientHandler playerHandler,
                         ConcurrencyService concurrency) {
        this.conn = conn;
        this.registry = registry;
        this.adminHandler = adminHandler;
        this.playerHandler = playerHandler;
        this.concurrency = concurrency;
    }

    @Override
    public void run() {
        // A thread do POOL é quem atende esta ligação daqui em diante.
        conn.attachThread(Thread.currentThread().getName());
        registry.add(conn);
        concurrency.register(conn.id(), conn.threadName());
        log.info("Cliente conectado: {} -> thread={}", conn.remote(), conn.threadName());

        try {
            String line;
            while ((line = conn.readLine()) != null) {
                Protocol.Message msg = Protocol.parse(line);
                if (msg == null) {
                    conn.send("ERROR|mensagem inválida (vazia ou demasiado longa)");
                    continue;
                }

                boolean authenticated = conn.isAdmin() || conn.isPlayer();

                if (!authenticated) {
                    if (!ProtocolHandler.isPreAuthCommand(msg.type())) {
                        conn.send("ERROR|envie ADMIN_LOGIN ou JOIN_GAME primeiro");
                        continue;
                    }
                    if (msg.type().equals("ADMIN_LOGIN")) {
                        adminHandler.authenticate(msg, conn);
                    } else if (msg.type().equals("JOIN_GAME")) {
                        playerHandler.join(msg, conn);
                    } else {
                        conn.send("PONG");
                    }
                    continue;
                }

                String error = ProtocolHandler.validateForRole(msg, conn.isAdmin(), true);
                if (error != null) {
                    conn.send("ERROR|" + error);
                    continue;
                }

                if (conn.isAdmin()) adminHandler.handle(msg, conn);
                else playerHandler.handle(msg, conn);
            }
            // readLine()==null => cliente fechou a ligação (EOF normal)
        } catch (EOFException e) {
            log.debug("EOF de {}", conn.remote());
        } catch (IOException e) {
            log.debug("Ligação de {} terminada: {}", conn.remote(), e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        try {
            if (conn.isPlayer()) {
                playerHandler.onDisconnect(conn);
            }
        } catch (RuntimeException e) {
            log.warn("Erro ao tratar desconexão: {}", e.getMessage());
        } finally {
            concurrency.unregister(conn.id());
            registry.remove(conn);
            conn.close();
            log.info("Cliente {} removido ({} ligações ativas)",
                    conn.remote(), registry.count());
        }
    }
}
