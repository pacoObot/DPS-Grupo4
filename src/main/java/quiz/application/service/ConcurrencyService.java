package quiz.application.service;

import quiz.shared.dto.MonitorLine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ConcurrencyService — REGISTO DE CONEXÕES ATIVAS (observabilidade).
 *
 * Mantém um mapa partilhado entre todas as threads do servidor:
 *   conexão -> (thread do pool, equipa, estado)
 *
 * É isto que alimenta o MONITORAMENTO CONCORRENTE da apresentação:
 *
 *   Thread                 Equipe     Estado
 *   pool-1-thread-1        Alpha      ANSWERING
 *   pool-1-thread-2        Beta       ANSWERED
 *
 * ConcurrentHashMap: escritas por muitas threads (join/answer/disconnect)
 * sem locks globais — apenas operações atómicas por entrada.
 */
public class ConcurrencyService {

    public record ConnectionInfo(String connectionId, String threadName,
                                 String teamName, String state) {
    }

    private final Map<String, ConnectionInfo> connections = new ConcurrentHashMap<>();

    public void register(String connectionId, String threadName) {
        connections.put(connectionId, new ConnectionInfo(connectionId, threadName, "-", "CONNECTED"));
    }

    public void attachTeam(String connectionId, String teamName) {
        connections.computeIfPresent(connectionId, (k, v) ->
                new ConnectionInfo(k, v.threadName(), teamName, "WAITING"));
    }

    public void updateState(String connectionId, String state) {
        connections.computeIfPresent(connectionId, (k, v) ->
                new ConnectionInfo(k, v.threadName(), v.teamName(), state));
    }

    public void unregister(String connectionId) {
        connections.remove(connectionId);
    }

    public int activeConnections() {
        return connections.size();
    }

    /** Número de threads distintas a servir clientes (prova de paralelismo). */
    public int activeThreads() {
        return (int) connections.values().stream()
                .map(ConnectionInfo::threadName).distinct().count();
    }

    public List<MonitorLine> snapshot() {
        List<MonitorLine> out = new ArrayList<>();
        for (ConnectionInfo c : connections.values()) {
            out.add(new MonitorLine(c.threadName(), c.teamName(), c.state()));
        }
        out.sort((a, b) -> a.teamName().compareTo(b.teamName()));
        return out;
    }

    /** MONITOR|thread:team:state|thread:team:state|... */
    public String toProtocol() {
        StringBuilder sb = new StringBuilder("MONITOR");
        for (MonitorLine line : snapshot()) {
            sb.append('|').append(line.toProtocolFields());
        }
        return sb.toString();
    }
}
