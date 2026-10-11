package quiz.domain.entity;

import quiz.domain.valueobject.PlayerId;
import quiz.domain.valueobject.TeamId;

import java.util.Objects;

/**
 * Jogador humano — entidade de domínio pura.
 * Cada jogador pertence a uma equipa; a equipa é a unidade de pontuação.
 */
public class Player {

    private final PlayerId id;
    private final String nickname;
    private final TeamId teamId;

    public Player(PlayerId id, String nickname, TeamId teamId) {
        this.id = id;
        this.nickname = nickname;
        this.teamId = teamId;
    }

    public void validate() {
        if (nickname == null || nickname.isBlank() || nickname.length() > 24)
            throw new IllegalArgumentException("Nickname inválido (1-24 caracteres)");
    }

    public PlayerId getId() { return id; }
    public String getNickname() { return nickname; }
    public TeamId getTeamId() { return teamId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Player p)) return false;
        return Objects.equals(id, p.id);
    }

    @Override
    public int hashCode() { return Objects.hashCode(id); }

    @Override
    public String toString() {
        return nickname + " (team=" + teamId + ")";
    }
}
