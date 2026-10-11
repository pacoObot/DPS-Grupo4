package quiz.shared.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * =====================================================================
 *  ServerStats — fotografia agregada do servidor para o dashboard admin
 * =====================================================================
 *
 *  Formato do protocolo (uma linha, chave:valor separadas por "|"):
 *
 *    STATS|port:5050|pool:50|threads:2|conns:5|qTotal:20|qActive:18|cats:6
 *         |gameId:3|status:RUNNING|name:Demo|round:2|rounds:3
 *         |teams:12|online:9|answers:18|timeouts:2|top:1,Alpha,120;2,Beta,80
 *
 *  Todos os números vêm de dados REAIS (repositórios + registry). O
 *  parser tolera campos em falta (defaults) — evolução compatível.
 */
public record ServerStats(
        int port, int poolSize, int activeThreads, int connections,
        long questionsTotal, long questionsActive, long categories,
        long gameId, String gameStatus, String gameName, int round, int rounds,
        int teamsJoined, int teamsOnline, long answers, long timeouts,
        List<TopEntry> top) {

    public record TopEntry(int position, String team, long points) {}

    public static final ServerStats EMPTY = new ServerStats(
            0, 0, 0, 0, 0, 0, 0, -1, "-", "-", 0, 0, 0, 0, 0, 0, List.of());

    public String toProtocol() {
        StringBuilder sb = new StringBuilder("STATS");
        sb.append("|port:").append(port)
          .append("|pool:").append(poolSize)
          .append("|threads:").append(activeThreads)
          .append("|conns:").append(connections)
          .append("|qTotal:").append(questionsTotal)
          .append("|qActive:").append(questionsActive)
          .append("|cats:").append(categories)
          .append("|gameId:").append(gameId)
          .append("|status:").append(clean(gameStatus))
          .append("|name:").append(clean(gameName))
          .append("|round:").append(round)
          .append("|rounds:").append(rounds)
          .append("|teams:").append(teamsJoined)
          .append("|online:").append(teamsOnline)
          .append("|answers:").append(answers)
          .append("|timeouts:").append(timeouts);
        if (top != null && !top.isEmpty()) {
            sb.append("|top:");
            for (int i = 0; i < top.size(); i++) {
                TopEntry t = top.get(i);
                if (i > 0) sb.append(';');
                sb.append(t.position()).append(',').append(clean(t.team())).append(',').append(t.points());
            }
        }
        return sb.toString();
    }

    public static ServerStats fromProtocol(String[] f) {
        int port = 0, pool = 0, threads = 0, conns = 0, round = 0, rounds = 0;
        int teams = 0, online = 0;
        long qTotal = 0, qActive = 0, cats = 0, gameId = -1, answers = 0, timeouts = 0;
        String status = "-", name = "-";
        List<TopEntry> top = new ArrayList<>();

        for (int i = 1; i < f.length; i++) {
            String raw = f[i];
            int c = raw.indexOf(':');
            if (c < 0) continue;
            String key = raw.substring(0, c);
            String val = raw.substring(c + 1);
            switch (key) {
                case "port" -> port = parseInt(val);
                case "pool" -> pool = parseInt(val);
                case "threads" -> threads = parseInt(val);
                case "conns" -> conns = parseInt(val);
                case "qTotal" -> qTotal = parseLong(val);
                case "qActive" -> qActive = parseLong(val);
                case "cats" -> cats = parseLong(val);
                case "gameId" -> gameId = parseLong(val);
                case "status" -> status = val;
                case "name" -> name = val;
                case "round" -> round = parseInt(val);
                case "rounds" -> rounds = parseInt(val);
                case "teams" -> teams = parseInt(val);
                case "online" -> online = parseInt(val);
                case "answers" -> answers = parseLong(val);
                case "timeouts" -> timeouts = parseLong(val);
                case "top" -> top = parseTop(val);
                default -> { /* ignora chaves desconhecidas (compatibilidade) */ }
            }
        }
        return new ServerStats(port, pool, threads, conns, qTotal, qActive, cats,
                gameId, status, name, round, rounds, teams, online, answers, timeouts, top);
    }

    private static List<TopEntry> parseTop(String value) {
        List<TopEntry> out = new ArrayList<>();
        if (value == null || value.isBlank()) return out;
        for (String entry : value.split(";")) {
            String[] p = entry.split(",");
            if (p.length >= 3) {
                out.add(new TopEntry(parseInt(p[0]), p[1], parseLong(p[2])));
            }
        }
        return out;
    }

    /** Remove separadores que quebrariam o formato chave:valor. */
    private static String clean(String s) {
        if (s == null) return "-";
        return s.replace('|', '-').replace(':', '-').replace(';', '-').replace(',', '-').strip();
    }

    private static int parseInt(String s) {
        try { return (int) parseLong(s); } catch (RuntimeException e) { return 0; }
    }

    private static long parseLong(String s) {
        try { return Long.parseLong(s.trim()); } catch (RuntimeException e) { return 0; }
    }
}