package quiz.application.usecase.admin;

import quiz.application.exception.BusinessException;
import quiz.application.port.output.QuestionRepository;
import quiz.application.port.output.UnitOfWork;
import quiz.domain.entity.Question;
import quiz.domain.enums.Difficulty;
import quiz.shared.dto.QuestionRowDTO;

/** Editar pergunta existente (validação sempre no domínio). */
public class UpdateQuestionUseCase {

    private final QuestionRepository questions;
    private final UnitOfWork uow;

    public UpdateQuestionUseCase(QuestionRepository questions, UnitOfWork uow) {
        this.questions = questions;
        this.uow = uow;
    }

    public QuestionRowDTO call(long id, String text, String category, String difficulty,
                               String a, String b, String c, String d,
                               char correct, int points, int timeLimit, boolean active) {
        return uow.call(() -> {
            Question q = questions.findById(id)
                    .orElseThrow(() -> new BusinessException.NotFound("Pergunta " + id + " não encontrada"));
            q.setText(text);
            q.setCategory(category);
            q.setDifficulty(Difficulty.parse(difficulty));
            q.setOptionA(a);
            q.setOptionB(b);
            q.setOptionC(c);
            q.setOptionD(d);
            q.setCorrectOption(correct);
            q.setPoints(points);
            q.setTimeLimit(timeLimit);
            q.setActive(active);
            q.validate();
            questions.update(q);
            return quiz.application.service.QuestionService.toRow(q);
        });
    }

    /** Ativar/desativar pergunta (SET_QUESTION_ACTIVE|id|true). */
    public QuestionRowDTO setActive(long id, boolean active) {
        return uow.call(() -> {
            Question q = questions.findById(id)
                    .orElseThrow(() -> new BusinessException.NotFound("Pergunta " + id + " não encontrada"));
            q.setActive(active);
            questions.update(q);
            return quiz.application.service.QuestionService.toRow(q);
        });
    }
}
