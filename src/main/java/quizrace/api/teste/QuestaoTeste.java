package quizrace.api.teste;

import jakarta.persistence.*;

@Embeddable
public class QuestaoTeste {
    @Column(nullable = false, length = 2000) private String enunciado;
    @Column(name = "alternativa_a", nullable = false, length = 500) private String alternativaA;
    @Column(name = "alternativa_b", nullable = false, length = 500) private String alternativaB;
    @Column(name = "alternativa_c", nullable = false, length = 500) private String alternativaC;
    @Column(name = "alternativa_d", nullable = false, length = 500) private String alternativaD;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 1)
    private ContratosTeste.Alternativa correcta;
    @Column(nullable = false, length = 100) private String categoria;

    protected QuestaoTeste() {}
    public QuestaoTeste(ContratosTeste.Questao q) {
        enunciado = q.enunciado().strip();
        alternativaA = q.alternativaA().strip();
        alternativaB = q.alternativaB().strip();
        alternativaC = q.alternativaC().strip();
        alternativaD = q.alternativaD().strip();
        correcta = q.correcta();
        categoria = q.categoria() == null ? "" : q.categoria().strip();
    }
    public ContratosTeste.Questao publico() {
        return new ContratosTeste.Questao(enunciado, alternativaA, alternativaB,
                alternativaC, alternativaD, correcta, categoria);
    }
}
