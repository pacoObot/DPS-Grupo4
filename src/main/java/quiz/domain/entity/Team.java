package quiz.domain.entity;

import quiz.domain.enums.TeamStatus;
import quiz.domain.valueobject.GameId;
import quiz.domain.valueobject.TeamId;

import java.time.Instant;
import java.util.Objects;

/**
 * Equipa inscrita numa partida — entidade de domínio pura.
 * Possui um Score agregado e um estado (máquina de estados da spec §16).
 */
public class Team {

    private final TeamId id;
    private final GameId gameId;
    private final String name;
    private final Score score;
    private TeamStatus status;
    private final Instant connectedAt;
    private Instant disconnectedAt;

    public Team(TeamId id, GameId gameId, String name, Score score,
                TeamStatus status, Instant connectedAt, Instant disconnectedAt) {
        this.id = id;
        this.gameId = gameId;
        this.name = name;
        this.score = score == null ? new Score() : score;
        this.status = status == null ? TeamStatus.WAITING : status;
        this.connectedAt = connectedAt == null ? Instant.now() : connectedAt;
        this.disconnectedAt = disconnectedAt;
    }

    public static Team create(GameId gameId, String name) {
        return new Team(null, gameId, name, new Score(), TeamStatus.WAITING, Instant.now(), null);
    }

    public void validate() {
        if (name == null || name.isBlank() || name.length() > 24)
            throw new IllegalArgumentException("Nome da equipa inválido (1-24 caracteres)");
        if (!name.matches("[\\p{L}\\p{N} _-]+"))
            throw new IllegalArgumentException("Nome da equipa contém caracteres inválidos");
    }

    // ---- transições de estado (máquina de estados) ----------------------
    public synchronized void markPlaying()  { if (status != TeamStatus.OFFLINE) status = TeamStatus.PLAYING; }
    public synchronized void markAnswered() { status = TeamStatus.ANSWERED; }
    public synchronized void markTimeout()  { if (status != TeamStatus.OFFLINE) { status = TeamStatus.TIMEOUT; score.addWrong(); } }
    public synchronized void markOffline()  { status = TeamStatus.OFFLINE; disconnectedAt = Instant.now(); }
    public synchronized void markFinished() { if (status != TeamStatus.OFFLINE) status = TeamStatus.FINISHED; }
    public synchronized void markWaiting()  { if (status != TeamStatus.OFFLINE) status = TeamStatus.WAITING; }

    public synchronized void registerCorrectAnswer(int points) {
        score.addCorrect(points);
        status = TeamStatus.ANSWERED;
    }

    public synchronized void registerWrongAnswer() {
        score.addWrong();
        status = TeamStatus.ANSWERED;
    }

    public boolean isOnline() { return status != TeamStatus.OFFLINE; }

    // ---- getters ---------------------------------------------------------
    public TeamId getId() { return id; }
    public GameId getGameId() { return gameId; }
    public String getName() { return name; }
    public Score getScore() { return score; }
    public synchronized TeamStatus getStatus() { return status; }
    public synchronized void setStatus(TeamStatus status) { this.status = status; }
    public Instant getConnectedAt() { return connectedAt; }
    public Instant getDisconnectedAt() { return disconnectedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Team t)) return false;
        return Objects.equals(id, t.id);
    }

    @Override
    public int hashCode() { return Objects.hashCode(id); }

    @Override
    public String toString() {
        return String.format("%-14s %3d pts  ok=%d err=%d [%s]",
                name, score.getPoints(), score.getCorrectCount(), score.getWrongCount(), status);
    }
}
