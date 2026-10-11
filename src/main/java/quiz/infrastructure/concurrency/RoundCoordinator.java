package quiz.infrastructure.concurrency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * =====================================================================
 *  RoundCoordinator — coordenação de fim de ronda
 * =====================================================================
 *
 *  Uma ronda só termina quando:
 *      (a) TODAS as equipas online responderam   -> CountDownLatch a 0
 *   OU (b) o tempo do SERVIDOR expirou            -> deadline atingido
 *
 *  Mecanismos escolhidos e porquê:
 *
 *  - CountDownLatch: cada equipa que responde faz countDown() UMA vez;
 *    a thread do GameScheduler espera no await() até todas chegarem.
 *    Fácil de explicar na apresentação e sem busy-wait.
 *
 *  - deadlineNanos (System.nanoTime): o servidor é a autoridade do tempo —
 *    o contador do cliente é só visual.
 *
 *  - Set concorrente de "pendentes": protege contra sinalização dupla e
 *    permite dizer QUEM ficou em TIMEOUT ao fim da ronda.
 *
 *  - freeze()/unfreeze(): pausar a partida congela o deadline; ao retomar,
 *    o tempo restante é deslocado pelo período pausado.
 */
public final class RoundCoordinator {

    private static final Logger log = LoggerFactory.getLogger(RoundCoordinator.class);

    private volatile CountDownLatch latch = new CountDownLatch(0);
    private volatile Set<Long> pending = ConcurrentHashMap.newKeySet();
    private volatile long deadlineNanos;
    private volatile long frozenAtNanos = -1;
    private volatile boolean active;
    private volatile int expectedCount;

    /** Prepara a ronda: equipas esperadas + deadline absoluto. */
    public synchronized void beginRound(Collection<Long> teamIds, long deadlineNanos) {
        this.pending = ConcurrentHashMap.newKeySet();
        this.pending.addAll(teamIds);
        this.deadlineNanos = deadlineNanos;
        this.frozenAtNanos = -1;
        this.latch = new CountDownLatch(teamIds.size());
        this.expectedCount = teamIds.size();
        this.active = true;
        log.debug("Ronda iniciada: {} equipas esperadas, latch={}", teamIds.size(), latch.getCount());
    }

    /** Chamado por cada ClientHandler quando uma equipa responde. */
    public void signal(long teamId) {
        if (!active) return;
        if (pending.remove(teamId)) {   // primeira sinalização desta equipa
            latch.countDown();
        }
    }

    /**
     * O GameScheduler consulta isto num loop curto (200ms) para poder
     * reagir a PAUSE no meio da ronda — ver GameScheduler.runRoundLoop.
     *
     *  Nota: se não houver NENHUMA equipa online, a ronda NÃO termina
     *  imediatamente (senão todas as rondas passariam a correr instantâneas
     *  e o jogo terminaria num instante). Nesse caso espera-se o deadline.
     */
    public boolean allAnswered() {
        return expectedCount > 0 && latch.getCount() == 0;
    }

    public boolean isExpired() {
        if (frozenAtNanos >= 0) return false;  // pausado: o tempo não corre
        return System.nanoTime() >= deadlineNanos;
    }

    /** Equipas que ainda não responderam (candidatas a TIMEOUT). */
    public Set<Long> pendingTeams() {
        return new HashSet<>(pending);
    }

    public long remainingMillis() {
        if (frozenAtNanos >= 0) {
            return Math.max(0, TimeUnit.NANOSECONDS.toMillis(deadlineNanos - frozenAtNanos));
        }
        return Math.max(0, TimeUnit.NANOSECONDS.toMillis(deadlineNanos - System.nanoTime()));
    }

    /** Pausa: congela o deadline. */
    public synchronized void freeze() {
        if (frozenAtNanos < 0 && active) {
            frozenAtNanos = System.nanoTime();
            log.info("Ronda CONGELADA (pausa) — restavam {} ms", remainingMillis());
        }
    }

    /** Retoma: desloca o deadline pelo tempo em pausa. */
    public synchronized void unfreeze() {
        if (frozenAtNanos >= 0 && active) {
            long paused = System.nanoTime() - frozenAtNanos;
            deadlineNanos += paused;
            frozenAtNanos = -1;
            log.info("Ronda RETOMADA — +{} ms de tempo", TimeUnit.NANOSECONDS.toMillis(paused));
        }
    }

    public void endRound() {
        active = false;
        expectedCount = 0;
        latch = new CountDownLatch(0);
        pending = ConcurrentHashMap.newKeySet();
    }
}
