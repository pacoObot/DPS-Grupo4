package quiz.infrastructure.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quiz.infrastructure.Config;

/**
 * =====================================================================
 *  ServerMain — ponto de entrada do QUIZ SERVER (java -jar quiz-server.jar)
 * =====================================================================
 *
 *  Sequência de arranque:
 *    1. carrega configuração (env / db.properties)
 *    2. aplica migrações Flyway (V1..V8) no PostgreSQL
 *    3. cria o administrador inicial (admin/admin) se não existir
 *    4. arranca o GameScheduler (thread das rondas)
 *    5. abre o ServerSocket e entra no loop de accept()
 */
public final class ServerMain {

    private static final Logger log = LoggerFactory.getLogger(ServerMain.class);

    private ServerMain() {}

    public static void main(String[] args) {
        Config config = Config.load();
        AppContext ctx = new AppContext(config);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("A encerrar o servidor...");
            ctx.stop();
        }, "shutdown-hook"));

        ctx.start();

        banner(ctx);

        try {
            // Bloqueia a main; o accept-loop corre noutra thread e os
            // clientes são atendidos pelo pool.
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void banner(AppContext ctx) {
        log.info("=====================================================================");
        log.info("  QUIZ MULTIPLAYER PARALELO — servidor pronto");
        log.info("  Porta TCP .......... {}", ctx.config().serverPort());
        log.info("  Pool de threads .... {}", ctx.threadPool().size());
        log.info("  Base de dados ...... {}", ctx.config().dbUrl());
        log.info("  Admin inicial ...... admin / admin");
        log.info("  Monitor ............ envie GET_MONITOR pelo cliente Admin");
        log.info("=====================================================================");
    }
}
