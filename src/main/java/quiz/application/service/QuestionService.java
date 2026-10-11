package quiz.application.service;

import quiz.application.port.output.QuestionRepository;
import quiz.domain.entity.Question;
import quiz.shared.dto.QuestionRowDTO;

import java.util.ArrayList;
import java.util.List;

/** QuestionService — projeções/filtros sobre o banco de perguntas. */
public class QuestionService {

    private final QuestionRepository questions;

    public QuestionService(QuestionRepository questions) {
        this.questions = questions;
    }

    public List<QuestionRowDTO> search(String search, String difficulty, Boolean active) {
        List<QuestionRowDTO> out = new ArrayList<>();
        for (Question q : questions.findAll(search, difficulty, active)) {
            out.add(toRow(q));
        }
        return out;
    }

    public static QuestionRowDTO toRow(Question q) {
        return new QuestionRowDTO(q.getId().value(), q.getText(), q.getCategory(),
                q.getDifficulty().name(), q.getOptionA(), q.getOptionB(), q.getOptionC(),
                q.getOptionD(), q.getCorrectOption(), q.getPoints(), q.getTimeLimit(), q.isActive());
    }
}
