package quiz.infrastructure.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * BotClient — jogador automático usado para testar carga e concorrência.
 *
 * Liga-se ao servidor como um jogador normal (JOIN_GAME) e responde ao
 * broadcast QUESTION|... dentro do tempo limite. Não conhece a resposta certa
 * (o servidor nunca a envia), pelo que escolhe dentro da faixa de opções.
 *
 * IMPORTANTE (arquitetura): esta classe fala TCP diretamente, sem usar o
 * {@code quiz.interfaces.common.TcpClient} — a infraestrutura não deve
 * depender da camada de interface gráfica.
 */
public final class BotClient {

    private static final Logger log = LoggerFactory.getLogger(BotClient.class);

    private final String host;
    private final int port;
    private final String joinCode;
    private final String teamName;
    private final long maxDelayMs;
    private final int fixedOption;   // -1 = aleatório
    private final int accuracyPct;   // não usado p/ prever resposta; só relatórios

    private final Random rnd;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private Thread reader;
    private volatile long lastQuestionId = -1;

    public BotClient(String host, int port, String joinCode, String teamName,
                     long maxDelayMs, int fixedOption, int accuracyPct) {
        this.host = host;
        this.port = port;
        this.joinCode = joinCode;
        this.teamName = teamName;
        this.maxDelayMs = Math.max(0, maxDelayMs);
        this.fixedOption = (fixedOption >= 0 && fixedOption <= 3) ? fixedOption : -1;
        this.accuracyPct = accuracyPct;
        this.rnd = new Random(teamName.hashCode() * 31L + System.nanoTime());
    }

    public String teamName() { return teamName; }

    public void start() throws IOException {
        if (!running.compareAndSet(false, true)) return;
        socket = new Socket(host, port);
        socket.setTcpNoDelay(true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        send("JOIN_GAME|" + joinCode + "|" + teamName);
        reader = new Thread(this::loop, "bot-" + teamName);
        reader.setDaemon(true);
        reader.start();
    }

    private void loop() {
        try {
            String line;
            while (running.get() && (line = in.readLine()) != null) {
                dispatch(line);
            }
        } catch (IOException e) {
            // servidor encerrou ou rede caiu
        } finally {
            running.set(false);
        }
    }

    private void dispatch(String line) {
        int sep = line.indexOf('|');
        String type = sep < 0 ? line : line.substring(0, sep);
        String[] f = sep < 0 ? new String[0] : line.substring(sep + 1).split("\\|", -1);

        switch (type) {
            case "QUESTION" -> {
                if (f.length >= 4) {
                    long qid = parseLong(f[2], -1);
                    int timeLimit = f.length > 3 ? parseInt(f[3], 15) : 15;
                    lastQuestionId = qid;
                    scheduleAnswer(qid, timeLimit);
                }
            }
            case "GAME_OVER" -> {
                log.info("[{}] fim de jogo", teamName);
                close();
            }
            case "JOIN_ERROR" -> {
                log.warn("[{}] JOIN_ERROR: {}", teamName, f.length > 0 ? f[0] : "?");
                close();
            }
            default -> { /* JOIN_OK, WAITING, SCOREBOARD, ANSWER_RESULT, NEXT_ROUND, PONG... */ }
        }
    }

    /** Decide a opção e envia dentro de um atraso aleatório, respeitando o tempo limite. */
    private void scheduleAnswer(long qid, int timeLimit) {
        int option = fixedOption >= 0 ? fixedOption : rnd.nextInt(4);
        char letter = (char) ('A' + option);

        long budget = Math.min(maxDelayMs, Math.max(200, (long) (timeLimit * 1000 * 0.8)));
        long delay = budget <= 0 ? 0 : (long) (rnd.nextDouble() * budget);

        Thread t = new Thread(() -> {
            try {
                if (delay > 0) Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            if (running.get() && lastQuestionId == qid) {
                send("ANSWER|" + qid + "|" + letter);
            }
        }, "bot-answer-" + teamName);
        t.setDaemon(true);
        t.start();
    }

    private synchronized void send(String message) {
        if (!running.get() || out == null) return;
        try {
            out.write(message);
            out.newLine();
            out.flush();
        } catch (IOException e) {
            running.set(false);
        }
    }

    public void close() {
        running.set(false);
        try {
            if (out != null) { out.write("LEAVE_GAME"); out.newLine(); out.flush(); }
        } catch (IOException ignored) {
        }
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        }
    }

    public boolean isRunning() { return running.get(); }

    private static long parseLong(String s, long def) {
        try { return Long.parseLong(s.trim()); } catch (Exception e) { return def; }
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }
}