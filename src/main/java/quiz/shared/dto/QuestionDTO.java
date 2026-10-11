package quiz.shared.dto;

import quiz.shared.protocol.Protocol;

/**
 * Pergunta tal como chega ao cliente — SEM a resposta correta.
 * Serialização: QUESTION|round|total|questionId|timeLimit|text|A|B|C|D
 */
public record QuestionDTO(int round, int total, long questionId, int timeLimit,
                          String text, String optionA, String optionB,
                          String optionC, String optionD) {

    public String toProtocol() {
        return Protocol.build("QUESTION", round, total, questionId, timeLimit,
                Protocol.sanitize(text),
                Protocol.sanitize(optionA), Protocol.sanitize(optionB),
                Protocol.sanitize(optionC), Protocol.sanitize(optionD));
    }

    public String[] options() {
        return new String[]{optionA, optionB, optionC, optionD};
    }
}
