package quiz.infrastructure.concurrency;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ClientThreadPool — ExecutorService de tamanho fixo.
 *
 *  NÃO criamos new Thread() por cliente: submetemos Runnables a um pool,
 *  o que limita e reaproveita as threads (spec §6).
 *
 *  Cada ClientHandler fica preso ao seu socket (readLine bloqueante),
 *  por isso o pool precisa de uma thread por cliente CONECTADO —
 *  o tamanho do pool é QUIZ_THREADS (default 50) e o servidor recusa
 *  clientes acima disso de forma controlada.
 */
public final class ClientThreadPool {

    private final ExecutorService pool;
    private final int size;
    private final AtomicInteger submitted = new AtomicInteger();

    public ClientThreadPool(int size) {
        this.size = size;
        // nome explícito "quiz-pool-N" -> aparece nos logs:
        //   [quiz-pool-1-3] Alpha answered correctly
        this.pool = Executors.newFixedThreadPool(size, r -> {
            Thread t = new Thread(r, "quiz-pool-1-" + submitted.incrementAndGet());
            t.setDaemon(false);
            return t;
        });
    }

    public void submit(Runnable task) {
        pool.submit(task);
    }

    public int size() { return size; }

    public int activeCount() {
        return ((java.util.concurrent.ThreadPoolExecutor) pool).getActiveCount();
    }

    public int queuedCount() {
        return ((java.util.concurrent.ThreadPoolExecutor) pool).getQueue().size();
    }

    public void shutdown() {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) pool.shutdownNow();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            pool.shutdownNow();
        }
    }
}
