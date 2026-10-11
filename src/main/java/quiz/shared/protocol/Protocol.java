package quiz.shared.protocol;

/**
 * =====================================================================
 *  PROTOCOLO TCP — texto, campos separados por "|"
 * =====================================================================
 *
 *  Cliente -> Servidor (admin):
 *    ADMIN_LOGIN|user|pass
 *    CREATE_QUESTION|text|category|difficulty|A|B|C|D|correct|points|timeLimit|active
 *    UPDATE_QUESTION|id|...mesmos campos...
 *    DELETE_QUESTION|id
 *    LIST_QUESTIONS|search|difficulty|activeFilter
 *    SET_QUESTION_ACTIVE|id|true
 *    CREATE_GAME|name|q1,q2,q3|timePerQuestion
 *    OPEN_GAME|gameId
 *    START_GAME|gameId
 *    PAUSE_GAME|gameId
 *    RESUME_GAME|gameId
 *    STOP_GAME|gameId
 *    GET_TEAMS|gameId
 *    GET_SCOREBOARD|gameId
 *    GET_MONITOR
 *
 *  Cliente -> Servidor (jogador):
 *    JOIN_GAME|codigo|NomeEquipa
 *    GET_QUESTION
 *    ANSWER|questionId|B          <-- NUNCA envia pontuação (spec §17)
 *    LEAVE_GAME
 *    PING
 *
 *  Servidor -> Cliente:
 *    LOGIN_OK|admin  | LOGIN_ERROR|msg
 *    QUESTION_CREATED|id | QUESTION_UPDATED|id | QUESTION_DELETED|id
 *    QUESTION_ROW|id|text|category|difficulty|A|B|C|D|correct|points|time|active
 *    LIST_END|n
 *    GAME_CREATED|gameId|code | GAME_STATUS|gameId|status|round|total
 *    GAME_STARTED|id | GAME_PAUSED|id | GAME_RESUMED|id | GAME_STOPPED|id
 *    JOIN_OK|teamId|gameId|gameName | JOIN_ERROR|msg
 *    WAITING|nOnline|nTotal
 *    QUESTION|round|total|questionId|timeLimit|text|A|B|C|D
 *    ANSWER_RESULT|questionId|CORRECT|points|bonus|total
 *    ANSWER_RESULT|questionId|WRONG|0|0|total
 *    ANSWER_RESULT|questionId|TIMEOUT|0|0|total
 *    ANSWER_RESULT|questionId|DUPLICATE|0|0|total
 *    ANSWER_RESULT|questionId|LATE|0|0|total
 *    NEXT_ROUND|round
 *    SCOREBOARD|pos:team:pts:ok:err:status|...
 *    TEAMS|name:pts:status:thread|...
 *    MONITOR|thread:team:state|...
 *    GAME_OVER|fim
 *    ERROR|mensagem
 *
 * SEGURANÇA: o servidor valida tipo, número de campos, tamanho e faixas
 * numéricas de TODA mensagem recebida. Mensagens malformadas => ERROR.
 */
public final class Protocol {

    /** Limite de tamanho de qualquer mensagem (proteção contra abuso). */
    public static final int MAX_MESSAGE_LENGTH = 8192;
    public static final int MAX_FIELDS = 40;

    private Protocol() {}

    /** Mensagem parseada: tipo + campos. */
    public record Message(String type, String[] fields) {

        public String field(int i) {
            return (i >= 0 && i < fields.length) ? fields[i] : null;
        }

        public int intField(int i, int def) {
            try {
                String f = field(i);
                return f == null ? def : Integer.parseInt(f.trim());
            } catch (NumberFormatException e) {
                return def;
            }
        }

        public long longField(int i, long def) {
            try {
                String f = field(i);
                return f == null ? def : Long.parseLong(f.trim());
            } catch (NumberFormatException e) {
                return def;
            }
        }

        public boolean boolField(int i, boolean def) {
            String f = field(i);
            if (f == null) return def;
            return f.trim().equalsIgnoreCase("true") || f.trim().equals("1");
        }

        public int count() { return fields.length; }
    }

    /**
     * Converte uma linha lida do socket em Message.
     * Devolve null se inválida (vazia, gigante ou sem tipo) — o chamador
     * responde ERROR e nunca confia no conteúdo.
     */
    public static Message parse(String line) {
        if (line == null) return null;
        line = line.strip();
        if (line.isEmpty() || line.length() > MAX_MESSAGE_LENGTH) return null;

        String[] parts = line.split("\\|", -1);
        String type = parts[0].trim();
        if (type.isEmpty() || type.length() > 40) return null;
        if (parts.length - 1 > MAX_FIELDS) return null;
        if (!type.matches("[A-Za-z_]+")) return null;

        String[] fields = new String[parts.length - 1];
        System.arraycopy(parts, 1, fields, 0, fields.length);
        return new Message(type, fields);
    }

    /** Constrói uma mensagem de saída. */
    public static String build(Object... parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append('|');
            sb.append(parts[i]);
        }
        String out = sb.toString();
        if (out.length() > MAX_MESSAGE_LENGTH) return "ERROR|mensagem demasiado longa";
        return out;
    }

    /** Remove caracteres que quebram o protocolo. */
    public static String sanitize(String s) {
        if (s == null) return "";
        return s.replace('|', '-').replace('\n', ' ').replace('\r', ' ').strip();
    }
}
