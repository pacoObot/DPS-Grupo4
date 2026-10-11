package quiz.shared.dto;

/**
 * Resultado da avaliação de uma resposta — decidido 100% pelo servidor.
 * status: CORRECT | WRONG | TIMEOUT | DUPLICATE | LATE
 */
public record AnswerResultDTO(long questionId, String status, int points, int bonus, int totalScore) {
}
