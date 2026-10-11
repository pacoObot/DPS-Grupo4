package quiz.infrastructure.network;

import quiz.shared.protocol.Protocol;

import java.util.Set;

/**
 * ProtocolHandler — validação das mensagens recebidas (defesa em profundidade).
 *
 *  1. Protocol.parse já garante tamanho máximo e formato.
 *  2. Aqui verificamos que o TIPO é conhecido para o PAPEL do remetente
 *     (admin não emite comandos de jogador e vice-versa).
 *  3. A validação de campos individuais acontece nos use cases.
 *
 *  Nenhuma mensagem malformada chega à lógica de negócio.
 */
public final class ProtocolHandler {

    private static final Set<String> ADMIN_COMMANDS = Set.of(
            "ADMIN_LOGIN", "CREATE_QUESTION", "UPDATE_QUESTION", "DELETE_QUESTION",
            "LIST_QUESTIONS", "SET_QUESTION_ACTIVE", "CREATE_GAME", "OPEN_GAME",
            "START_GAME", "PAUSE_GAME", "RESUME_GAME", "STOP_GAME",
            "GET_TEAMS", "GET_SCOREBOARD", "GET_MONITOR", "GET_STATS", "PING");

    private static final Set<String> PLAYER_COMMANDS = Set.of(
            "JOIN_GAME", "GET_QUESTION", "ANSWER", "LEAVE_GAME", "PING");

    private static final Set<String> PRE_AUTH = Set.of("ADMIN_LOGIN", "JOIN_GAME", "PING");

    private ProtocolHandler() {}

    /** true se o comando existe antes da autenticação. */
    public static boolean isPreAuthCommand(String type) {
        return PRE_AUTH.contains(type);
    }

    public static boolean isKnownAdminCommand(String type) {
        return ADMIN_COMMANDS.contains(type);
    }

    public static boolean isKnownPlayerCommand(String type) {
        return PLAYER_COMMANDS.contains(type);
    }

    /** Mensagem de erro amigável ou null se o comando é válido para o papel. */
    public static String validateForRole(Protocol.Message msg, boolean isAdmin, boolean isAuthenticated) {
        String type = msg.type();
        if (!isPreAuthCommand(type) && !isAuthenticated) {
            return "envie ADMIN_LOGIN ou JOIN_GAME primeiro";
        }
        if (isAdmin && !isKnownAdminCommand(type)) {
            return "comando desconhecido para admin: " + type;
        }
        if (!isAdmin && !isKnownPlayerCommand(type)) {
            return "comando desconhecido para jogador: " + type;
        }
        return null;
    }
}
