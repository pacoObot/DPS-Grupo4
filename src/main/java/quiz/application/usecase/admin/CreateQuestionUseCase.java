package quiz.application.usecase.admin;

import quiz.application.port.output.QuestionRepository;
import quiz.application.port.output.UnitOfWork;
import quiz.domain.entity.Question;
import quiz.domain.enums.Difficulty;
import quiz.shared.dto.QuestionRowDTO;

/**
 * Criar pergunta.
 * Fluxo: AdminApplication (JavaFX) -> TCP CREATE_QUESTION ->
 *        CreateQuestionUseCase -> QuestionRepository -> PostgreSQL.
 */
public class CreateQuestionUseCase {

    private final QuestionRepository questions;
    private final UnitOfWork uow;

    public CreateQuestionUseCase(QuestionRepository questions, UnitOfWork uow) {
        this.questions = questions;
        this.uow = uow;
    }

    public QuestionRowDTO call(String text, String category, String difficulty,
                               String a, String b, String c, String d,
                               char correct, int points, int timeLimit, boolean active) {
        return uow.call(() -> {
            Question q = Question.create(text, category, Difficulty.parse(difficulty),
                    a, b, c, d, correct, points, timeLimit, active);
            q.validate(); // regra de domínio — lança Validation se inválida
            Question saved = questions.save(q);
            return quiz.application.service.QuestionService.toRow(saved);
        });
    }
}
