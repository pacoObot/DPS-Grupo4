package quiz.shared.dto;

import quiz.shared.protocol.Protocol;

/** Linha do monitor de concorrência: thread | equipa | estado. */
public record MonitorLine(String threadName, String teamName, String state) {

    public String toProtocolFields() {
        return Protocol.sanitize(threadName) + ":" + Protocol.sanitize(teamName) + ":" + Protocol.sanitize(state);
    }
}
