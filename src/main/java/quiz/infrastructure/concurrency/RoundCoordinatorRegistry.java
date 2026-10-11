package quiz.infrastructure.concurrency;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * =====================================================================
 *  RoundCoordinatorRegistry — um RoundCoordinator POR PARTIDA
 * =====================================================================
 *
 *  Cada partida tem o seu próprio coordenador de ronda. Isto permite
 *  várias partidas em paralelo, cada uma com o seu latch/deadline, sem
 *  que uma resposta de uma partida interfira na contagem de outra.
 *
 *  - GameScheduler usa forGame(gameId) para a sua ronda.
 *  - PlayerClientHandler usa forGame(conn.gameId()) para sinalizar a
 *    resposta na partida correta.
 */
public final class RoundCoordinatorRegistry {

    private final Map<Long, RoundCoordinator> byGame = new ConcurrentHashMap<>();

    /** Devolve (criando se necessário) o coordenador da partida. */
    public RoundCoordinator forGame(long gameId) {
        return byGame.computeIfAbsent(gameId, id -> new RoundCoordinator());
    }

    /** Liberta o coordenador quando a partida termina. */
    public void remove(long gameId) {
        byGame.remove(gameId);
    }

    public void clear() {
        byGame.clear();
    }
}
