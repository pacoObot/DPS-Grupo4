package quiz.infrastructure.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ClientRegistry — registo de ligações ativas para broadcast.
 *
 *  CopyOnWriteArrayList: leituras (broadcast) muito frequentes,
 *  adições/remoções raras (join/leave) -> cópia barata, iteração sem lock.
 *
 *  É o que permite o GameScheduler entregar QUESTION/SCOREBOARD a todas
 *  as equipas SIMULTANEAMENTE, enquanto cada ClientHandler continua a
 *  processar respostas na sua própria thread.
 */
public final class ClientRegistry {

    private static final Logger log = LoggerFactory.getLogger(ClientRegistry.class);

    private final List<TcpConnection> connections = new CopyOnWriteArrayList<>();

    public void add(TcpConnection c) {
        connections.add(c);
    }

    public void remove(TcpConnection c) {
        connections.remove(c);
    }

    public List<TcpConnection> all() {
        return connections;
    }

    public int count() {
        return connections.size();
    }

    /** Envia a todos os clientes ligados. */
    public void broadcast(String message) {
        for (TcpConnection c : connections) c.send(message);
    }

    /** Envia a todas as equipas de uma partida (ex.: QUESTIONS/SCOREBOARD). */
    public void broadcastToGame(long gameId, String message) {
        for (TcpConnection c : connections) {
            if (c.isPlayer() && c.gameId() == gameId) c.send(message);
        }
    }

    /** Envia à equipa específica (ex.: RESULT só vai para quem respondeu). */
    public void sendToTeam(long teamId, String message) {
        for (TcpConnection c : connections) {
            if (c.teamId() == teamId) {
                c.send(message);
                return;
            }
        }
    }

    /** Envia a todos os administradores ligados. */
    public void broadcastToAdmins(String message) {
        for (TcpConnection c : connections) {
            if (c.isAdmin()) c.send(message);
        }
    }

    public List<TcpConnection> gameConnections(long gameId) {
        return connections.stream()
                .filter(c -> c.isPlayer() && c.gameId() == gameId)
                .toList();
    }

    public int onlineTeams(long gameId) {
        return (int) gameConnections(gameId).size();
    }

    public void closeAll() {
        for (TcpConnection c : connections) {
            c.close();
        }
        connections.clear();
        log.info("Todas as ligações encerradas");
    }
}
