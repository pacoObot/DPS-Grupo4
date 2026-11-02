package quizrace.api.teste;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
public class TesteRevisao {
    @Id private UUID id;
    @Column(nullable = false, updatable = false) private UUID testeId;
    @Column(nullable = false, updatable = false) private long versao;
    @Column(nullable = false, updatable = false) private Instant guardadoEm;
    @Column(nullable = false, updatable = false, columnDefinition = "text") private String conteudo;

    protected TesteRevisao() {}
    public TesteRevisao(Teste teste, String conteudo) {
        id = UUID.randomUUID();
        testeId = teste.getId();
        versao = teste.getVersao();
        guardadoEm = Instant.now();
        this.conteudo = conteudo;
    }
    public String getConteudo() { return conteudo; }
    public ContratosTeste.Revisao resumo() { return new ContratosTeste.Revisao(versao, guardadoEm); }
}
