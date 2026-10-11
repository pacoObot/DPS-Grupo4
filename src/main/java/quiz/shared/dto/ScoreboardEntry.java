package quiz.shared.dto;

import quiz.shared.protocol.Protocol;

/** Uma linha da classificação. */
public record ScoreboardEntry(int position, String teamName, int points,
                              int correct, int wrong, String status) {

    /** SCOREBOARD|1:Alpha:80:5:1:PLAYING|2:Beta:70:4:2:PLAYING */
    public static String toProtocol(java.util.List<ScoreboardEntry> entries) {
        StringBuilder sb = new StringBuilder("SCOREBOARD");
        for (ScoreboardEntry e : entries) {
            sb.append('|').append(e.position).append(':')
              .append(Protocol.sanitize(e.teamName)).append(':')
              .append(e.points).append(':')
              .append(e.correct).append(':')
              .append(e.wrong).append(':')
              .append(e.status);
        }
        return sb.toString();
    }
}
