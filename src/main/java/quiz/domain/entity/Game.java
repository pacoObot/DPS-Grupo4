package quiz.domain.entity;

import quiz.domain.enums.GameStatus;
import quiz.domain.valueobject.GameId;
import quiz.domain.valueobject.QuestionId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Partida — entidade de domínio pura.
 * Controla o estado global (máquina de estados) usado pelo GameScheduler.
 */
public class Game {

    private final GameId id;
    private String name;
    private String joinCode;
    private GameStatus status;
    private int totalRounds;
    private int currentRound;
    private int timePerQuestion;
    private Instant roundStartedAt;
    private final Instant createdAt;
    private Instant startedAt;
    private Instant finishedAt;
    private final List<QuestionId> questionIds;

    public Game(GameId id, String name, String joinCode, GameStatus status,
                int totalRounds, int currentRound, int timePerQuestion,
                Instant roundStartedAt, Instant createdAt, Instant startedAt,
                Instant finishedAt, List<QuestionId> questionIds) {
        this.id = id;
        this.name = name;
        this.joinCode = joinCode;
        this.status = status;
        this.totalRounds = totalRounds;
        this.currentRound = currentRound;
        this.timePerQuestion = timePerQuestion;
        this.roundStartedAt = roundStartedAt;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.questionIds = questionIds == null ? new ArrayList<>() : new ArrayList<>(questionIds);
    }

    public static Game create(String name, int timePerQuestion, List<QuestionId> questionIds) {
        if (questionIds == null || questionIds.isEmpty())
            throw new IllegalArgumentException("A partida precisa de pelo menos 1 pergunta");
        return new Game(null, name, null, GameStatus.OPEN,
                questionIds.size(), 0, timePerQuestion,
                null, Instant.now(), null, null, questionIds);
    }

    public void validate() {
        if (name == null || name.isBlank() || name.length() > 150)
            throw new IllegalArgumentException("Nome da partida inválido (1-150 caracteres)");
        if (timePerQuestion <= 0 || timePerQuestion > 300)
            throw new IllegalArgumentException("Tempo por pergunta deve estar entre 1 e 300 segundos");
    }

    // ---- máquina de estados ---------------------------------------------
    public synchronized void open() {
        if (status != GameStatus.OPEN) throw new IllegalStateException("Só uma partida OPEN pode abrir inscrições");
        status = GameStatus.OPEN;
    }

    public synchronized void start() {
        if (status != GameStatus.OPEN) throw new IllegalStateException("Só se pode iniciar uma partida OPEN");
        status = GameStatus.RUNNING;
        startedAt = Instant.now();
        currentRound = 1;
    }

    public synchronized void pause() {
        if (status != GameStatus.RUNNING) throw new IllegalStateException("Só RUNNING pode ser pausada");
        status = GameStatus.PAUSED;
    }

    public synchronized void resume() {
        if (status != GameStatus.PAUSED) throw new IllegalStateException("Só PAUSED pode ser retomada");
        status = GameStatus.RUNNING;
    }

    public synchronized void finish() {
        status = GameStatus.FINISHED;
        finishedAt = Instant.now();
    }

    /** Avança para a próxima ronda; devolve false quando acabaram. */
    public synchronized boolean nextRound() {
        if (currentRound >= totalRounds) return false;
        currentRound++;
        return true;
    }

    public synchronized void setRoundStartedAt(Instant t) { this.roundStartedAt = t; }

    /** Deadline da ronda atual = início da ronda + tempo por pergunta (autoridade: servidor). */
    public synchronized Instant currentDeadline() {
        if (roundStartedAt == null) return null;
        return roundStartedAt.plusSeconds(timePerQuestion);
    }

    // ---- getters ---------------------------------------------------------
    public GameId getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getJoinCode() { return joinCode; }
    public void setJoinCode(String joinCode) { this.joinCode = joinCode; }
    public synchronized GameStatus getStatus() { return status; }
    public synchronized void setStatus(GameStatus status) { this.status = status; }
    public int getTotalRounds() { return totalRounds; }
    public void setTotalRounds(int totalRounds) { this.totalRounds = totalRounds; }
    public synchronized int getCurrentRound() { return currentRound; }
    public synchronized void setCurrentRound(int currentRound) { this.currentRound = currentRound; }
    public int getTimePerQuestion() { return timePerQuestion; }
    public void setTimePerQuestion(int timePerQuestion) { this.timePerQuestion = timePerQuestion; }
    public synchronized Instant getRoundStartedAt() { return roundStartedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public List<QuestionId> getQuestionIds() { return List.copyOf(questionIds); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Game g)) return false;
        return Objects.equals(id, g.id);
    }

    @Override
    public int hashCode() { return Objects.hashCode(id); }

    @Override
    public String toString() {
        return "Game{" + id + " '" + name + "' code=" + joinCode + " " + status
                + " round=" + currentRound + "/" + totalRounds + "}";
    }
}
