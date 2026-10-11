package quiz.shared.dto;

import quiz.shared.protocol.Protocol;

/** Visão da partida para clientes e monitorização. */
public record GameInfoDTO(long gameId, String name, String joinCode, String status,
                          int currentRound, int totalRounds, int timePerQuestion,
                          int teamsOnline, int teamsTotal) {

    public String toProtocol() {
        return Protocol.build("GAME_STATUS", gameId, status, currentRound, totalRounds,
                joinCode, teamsOnline, teamsTotal, Protocol.sanitize(name));
    }
}
