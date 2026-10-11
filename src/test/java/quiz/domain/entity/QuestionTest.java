package quiz.domain.entity;

import org.junit.jupiter.api.Test;
import quiz.domain.enums.Difficulty;

import static org.junit.jupiter.api.Assertions.*;

class QuestionTest {

    private Question valid() {
        return Question.create("Capital de Portugal?", "Geografia", Difficulty.EASY,
                "Lisboa", "Porto", "Madrid", "Paris", 'A', 10, 15, true);
    }

    @Test
    void perguntaValidaNaoLanca() {
        assertDoesNotThrow(() -> valid().validate());
    }

    @Test
    void textoVazioEhRejeitado() {
        Question q = Question.create("  ", "x", Difficulty.EASY, "a", "b", "c", "d", 'A', 10, 15, true);
        assertThrows(IllegalArgumentException.class, q::validate);
    }

    @Test
    void alternativaEmFaltaEhRejeitada() {
        Question q = Question.create("Pergunta?", "x", Difficulty.EASY, "a", "", "c", "d", 'A', 10, 15, true);
        assertThrows(IllegalArgumentException.class, q::validate);
    }

    @Test
    void pontosForaDoIntervaloSaoRejeitados() {
        Question q = Question.create("Pergunta?", "x", Difficulty.EASY, "a", "b", "c", "d", 'A', 0, 15, true);
        assertThrows(IllegalArgumentException.class, q::validate);
    }

    @Test
    void tempoForaDoIntervaloEhRejeitado() {
        Question q = Question.create("Pergunta?", "x", Difficulty.EASY, "a", "b", "c", "d", 'A', 10, 999, true);
        assertThrows(IllegalArgumentException.class, q::validate);
    }

    @Test
    void isCorrectIgnoraMaiusculasMinusculas() {
        Question q = valid();
        assertTrue(q.isCorrect('a'));
        assertTrue(q.isCorrect('A'));
        assertFalse(q.isCorrect('B'));
    }

    @Test
    void optionForDevolveAlternativaCerta() {
        Question q = valid();
        assertEquals("Lisboa", q.optionFor('A'));
        assertEquals("Paris", q.optionFor('d'));
        assertEquals(4, q.options().length);
    }

    @Test
    void correctOptionEhNormalizada() {
        Question q = Question.create("Pergunta?", "x", Difficulty.EASY, "a", "b", "c", "d", 'b', 10, 15, true);
        assertEquals('B', q.getCorrectOption());
    }
}