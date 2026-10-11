package quiz.web.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import quiz.web.tcp.TcpQuizClient;

/**
 * QuizWebSocketHandler — um jogador web por sessão WebSocket.
 *
 *  Protocolo browser <-> gateway (JSON):
 *    -> {"action":"join","host":"...","port":5050,"code":"AB12","team":"Alpha"}
 *    -> {"action":"answer","qid":123,"option":"B"}
 *    -> {"action":"leave"}
 *    <- {"type":"JOIN_OK","fields":["..."],"raw":"JOIN_OK|..."}
 *    <- {"type":"LOCAL_ERROR","message":"..."} / {"type":"CONNECTED"} ...
 *
 *  O handler apenas faz ponte: cada linha recebida do servidor TCP é
 *  reenviada ao browser já separada em tipo + campos. Os envios são
 *  serializados por um ConcurrentWebSocketSessionDecorator (o relay corre
 *  na thread do TCP e os comandos na thread do WebSocket).
 */
@Component
public class QuizWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(QuizWebSocketHandler.class);

    private final ObjectMapper json = new ObjectMapper();

    @Value("${quiz.tcp.host:127.0.0.1}")
    private String defaultHost;

    @Value("${quiz.tcp.port:5050}")
    private int defaultPort;

    /** Estado por sessão. */
    private static final class Session {
        WebSocketSession ws;
        TcpQuizClient tcp;
        volatile long currentQid = -1;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Session st = new Session();
        st.ws = new ConcurrentWebSocketSessionDecorator(session, 10_000, 1 << 20);
        session.getAttributes().put("state", st);
        send(st, "CONNECTED", null, null);
        log.info("jogador web ligado: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession rawSession, TextMessage message) {
        Session st = state(rawSession);
        if (st == null) return;
        try {
            JsonNode m = json.readTree(message.getPayload());
            String action = m.path("action").asText("");
            switch (action) {
                case "join" -> doJoin(st, m);
                case "answer" -> doAnswer(st, m);
                case "leave" -> doLeave(st);
                default -> send(st, "LOCAL_ERROR", null, "ação desconhecida: " + action);
            }
        } catch (Exception e) {
            send(st, "LOCAL_ERROR", null, "mensagem inválida: " + e.getMessage());
        }
    }

    private void doJoin(Session st, JsonNode m) {
        if (st.tcp != null) {
            send(st, "LOCAL_ERROR", null, "já está numa partida");
            return;
        }
        String host = m.path("host").asText(defaultHost);
        int port = m.path("port").asInt(defaultPort);
        String code = m.path("code").asText("").trim();
        String team = m.path("team").asText("").trim();
        if (code.isEmpty() || team.isEmpty()) {
            send(st, "LOCAL_ERROR", null, "código e nome de equipa são obrigatórios");
            return;
        }

        TcpQuizClient client = new TcpQuizClient(host, port, new TcpQuizClient.Listener() {
            @Override public void onLine(String line) { relay(st, line); }
            @Override public void onClosed(String reason) {
                send(st, "DISCONNECTED", null, reason);
                log.info("jogador web: TCP fechado ({})", reason);
            }
        });
        try {
            client.connect();
        } catch (Exception e) {
            send(st, "LOCAL_ERROR", null,
                    "não foi possível ligar ao servidor do quiz em " + host + ":" + port
                            + " (" + e.getMessage() + ")");
            return;
        }
        st.tcp = client;
        st.tcp.send("JOIN_GAME|" + code + "|" + team);
    }

    private void doAnswer(Session st, JsonNode m) {
        if (st.tcp == null) { send(st, "LOCAL_ERROR", null, "não está numa partida"); return; }
        long qid = m.path("qid").asLong(st.currentQid);
        String opt = m.path("option").asText("").trim().toUpperCase();
        if (qid < 0) { send(st, "LOCAL_ERROR", null, "pergunta desconhecida"); return; }
        if (opt.length() != 1 || opt.charAt(0) < 'A' || opt.charAt(0) > 'D') {
            send(st, "LOCAL_ERROR", null, "opção inválida"); return;
        }
        st.tcp.send("ANSWER|" + qid + "|" + opt);
    }

    private void doLeave(Session st) {
        if (st.tcp != null) st.tcp.close();
    }

    /** Separa a linha do protocolo e envia-a ao browser como JSON. */
    private void relay(Session st, String line) {
        int sep = line.indexOf('|');
        String type = sep < 0 ? line : line.substring(0, sep);
        String[] fields = sep < 0 ? new String[0] : line.substring(sep + 1).split("\\|", -1);
        if (type.equals("QUESTION") && fields.length >= 3) {
            try { st.currentQid = Long.parseLong(fields[2]); } catch (NumberFormatException ignored) { }
        }
        send(st, type, fields, null);
    }

    private void send(Session st, String type, String[] fields, String message) {
        if (st == null || st.ws == null || !st.ws.isOpen()) return;
        ObjectNode out = json.createObjectNode();
        out.put("type", type);
        if (fields != null) {
            ArrayNode arr = out.putArray("fields");
            for (String f : fields) arr.add(f);
            out.put("raw", type + "|" + String.join("|", fields));
        }
        if (message != null) out.put("message", message);
        try {
            st.ws.sendMessage(new TextMessage(out.toString()));
        } catch (Exception e) {
            log.debug("falha ao enviar: {}", e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Session st = state(session);
        if (st != null && st.tcp != null) st.tcp.close();
        log.info("jogador web desligado: {}", session.getId());
    }

    private static Session state(WebSocketSession session) {
        Object o = session.getAttributes().get("state");
        return o instanceof Session s ? s : null;
    }
}