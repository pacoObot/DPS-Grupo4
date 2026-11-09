package quizrace.api.teste;

import jakarta.persistence.*;

@Embeddable
public class RespostaParticipante {
    @Column(name="resposta",length=1) private String resposta;
    @Column(name="correcta",nullable=false) private boolean correcta;
    @Column(name="respondida_em",nullable=false) private java.time.Instant respondidaEm;
    protected RespostaParticipante() {}
    public RespostaParticipante(String resposta, boolean correcta) { this.resposta=resposta;this.correcta=correcta;respondidaEm=java.time.Instant.now(); }
    public boolean correcta() { return correcta; }
}
