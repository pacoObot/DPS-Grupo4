package quiz.application.usecase.admin;

import quiz.application.port.output.QuestionRepository;
import quiz.shared.dto.QuestionRowDTO;

import java.util.ArrayList;
import java.util.List;

/** Listar/pesquisar/filtrar perguntas (com ou sem ativas). */
public class ListQuestionsUseCase {

    private final QuestionRepository questions;

    public ListQuestionsUseCase(QuestionRepository questions) {
        this.questions = questions;
    }

    /**
     * @param search    texto parcial (opcional)
     * @param difficulty EASY/MEDIUM/HARD ou null
     * @param active    true/false ou null (todas)
     */
    public List<QuestionRowDTO> call(String search, String difficulty, Boolean active) {
        List<QuestionRowDTO> out = new ArrayList<>();
        questions.findAll(search, difficulty, active)
                .forEach(q -> out.add(quiz.application.service.QuestionService.toRow(q)));
        return out;
    }
}
