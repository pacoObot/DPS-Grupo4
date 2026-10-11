package quiz.shared.dto;

import quiz.shared.protocol.Protocol;

/** Linha de pergunta para a gestão administrativa (inclui resposta correta). */
public record QuestionRowDTO(long id, String text, String category, String difficulty,
                             String optionA, String optionB, String optionC, String optionD,
                             char correct, int points, int timeLimit, boolean active) {

    public String toProtocol() {
        return Protocol.build("QUESTION_ROW", id, Protocol.sanitize(text),
                Protocol.sanitize(category), difficulty,
                Protocol.sanitize(optionA), Protocol.sanitize(optionB),
                Protocol.sanitize(optionC), Protocol.sanitize(optionD),
                correct, points, timeLimit, active);
    }

    public static QuestionRowDTO fromProtocol(String[] f) {
        return new QuestionRowDTO(
                Long.parseLong(f[0]), f[1], f[2], f[3], f[4], f[5], f[6], f[7],
                f[8].charAt(0), Integer.parseInt(f[9]), Integer.parseInt(f[10]),
                Boolean.parseBoolean(f[11]));
    }
}
