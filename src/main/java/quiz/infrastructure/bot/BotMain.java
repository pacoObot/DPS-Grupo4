package quiz.infrastructure.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * BotMain — arranca um enxame de jogadores automáticos para stress/concorrência.
 *
 * Uso:
 *   java -jar quiz-paralelo-1.0.0-bot.jar --code ABC123 --bots 50
 *   java -jar quiz-paralelo-1.0.0-bot.jar --code ABC123 --bots 100 --host 127.0.0.1 \
 *        --port 5050 --delay 1500 --option random
 *
 * Opções:
 *   --code   <código da partida>  (obrigatório)
 *   --bots   <nº de bots>         (default 20)
 *   --host   <host>               (default 127.0.0.1)
 *   --port   <porta>              (default 5050)
 *   --delay  <ms máximos>         (default 1500) — atraso aleatório antes de responder
 *   --option <auto|0|1|2|3|A|B|C|D>  (default auto = aleatório)
 *   --prefix <prefixo do nome>    (default "Bot")
 */
public final class BotMain {

    private static final Logger log = LoggerFactory.getLogger(BotMain.class);

    public static void main(String[] args) throws Exception {
        String host = "127.0.0.1";
        int port = 5050;
        String code = null;
        int bots = 20;
        long delay = 1500;
        String option = "auto";
        String prefix = "Bot";

        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            switch (a) {
                case "--host"   -> host = args[++i];
                case "--port"   -> port = Integer.parseInt(args[++i]);
                case "--code"   -> code = args[++i];
                case "--bots"   -> bots = Integer.parseInt(args[++i]);
                case "--delay"  -> delay = Long.parseLong(args[++i]);
                case "--option" -> option = args[++i];
                case "--prefix" -> prefix = args[++i];
                case "-h", "--help" -> { printUsage(); return; }
                default -> {
                    if (code == null) code = a; // compat: primeiro posicional = código
                }
            }
        }

        if (code == null || code.isBlank()) {
            printUsage();
            System.exit(2);
            return;
        }

        int fixed = parseOption(option);
        log.info("Enxame: {} bots -> {}:{}  código={}  atraso<= {}ms  opção={}",
                bots, host, port, code, delay, fixed < 0 ? "aleatória" : String.valueOf((char) ('A' + fixed)));

        List<BotClient> clients = new ArrayList<>();
        CountDownLatch ready = new CountDownLatch(bots);
        CountDownLatch done = new CountDownLatch(bots);

        for (int i = 1; i <= bots; i++) {
            String team = prefix + "-" + String.format("%03d", i);
            BotClient bot = new BotClient(host, port, code, team, delay, fixed, 0);
            clients.add(bot);
            Thread t = new Thread(() -> {
                try {
                    // arranque escalonado para não criar uma avalancha de conexões
                    Thread.sleep(30L + (long) (Math.random() * 70));
                    bot.start();
                    ready.countDown();
                    while (bot.isRunning()) Thread.sleep(200);
                } catch (Exception e) {
                    ready.countDown();
                    log.debug("bot {} terminou: {}", team, e.getMessage());
                } finally {
                    done.countDown();
                }
            }, "bot-launcher-" + i);
            t.setDaemon(true);
            t.start();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("A encerrar {} bots...", clients.size());
            clients.forEach(BotClient::close);
        }, "bot-shutdown"));

        ready.await();
        log.info("Todos os {} bots entraram. A jogar... (Ctrl+C para sair)", bots);

        done.await();
        log.info("Todos os bots terminaram.");
    }

    private static int parseOption(String option) {
        if (option == null || option.equalsIgnoreCase("auto") || option.equalsIgnoreCase("random")) return -1;
        String o = option.trim().toUpperCase();
        if (o.length() == 1) {
            char c = o.charAt(0);
            if (c >= 'A' && c <= 'D') return c - 'A';
            if (c >= '0' && c <= '3') return c - '0';
        }
        return -1;
    }

    private static void printUsage() {
        System.out.println("""
            Uso: java -jar quiz-paralelo-1.0.0-bot.jar --code <CODIGO> [opções]

              --code   <código da partida>   (obrigatório)
              --bots   <nº de bots>          (default 20)
              --host   <host>                (default 127.0.0.1)
              --port   <porta>               (default 5050)
              --delay  <ms máximos>          (default 1500)
              --option <auto|A|B|C|D|0-3>    (default auto)
              --prefix <prefixo das equipas> (default Bot)
            """);
    }

    private BotMain() {}
}