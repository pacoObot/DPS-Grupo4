package quiz.application.usecase.admin;

import quiz.application.exception.BusinessException;
import quiz.application.port.output.QuestionRepository;
import quiz.application.port.output.UnitOfWork;

/** Eliminar pergunta. */
public class DeleteQuestionUseCase {

    private final QuestionRepository questions;
    private final UnitOfWork uow;

    public DeleteQuestionUseCase(QuestionRepository questions, UnitOfWork uow) {
        this.questions = questions;
        this.uow = uow;
    }

    public void call(long id) {
        uow.call(() -> {
            if (questions.findById(id).isEmpty())
                throw new BusinessException.NotFound("Pergunta " + id + " não encontrada");
            // Preserva o histórico: se a pergunta já foi usada numa partida
            // (FK em game_questions/answers), a eliminação destruiria dados
            // necessários. Nesse caso sugerimos desativá-la.
            if (questions.isReferenced(id))
                throw new BusinessException.Conflict(
                        "Pergunta " + id + " já foi usada em partidas — desative-a em vez de eliminar.");
            questions.delete(id);
            return null;
        });
    }
}
