package quizrace.api.teste;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
public class Teste {
    @Id private UUID id;
    @Column(nullable = false, updatable = false) private UUID docenteId;
    @Column(nullable = false, length = 150) private String titulo;
    @Column(nullable = false, length = 2000) private String descricao;
    @Column(nullable = false) private boolean disponivelPratica;
    @Column(nullable = false, updatable = false) private Instant criadoEm;
    @Column(nullable = false) private Instant actualizadoEm;
    @Version private Long versao;
    @ElementCollection
    @CollectionTable(name = "teste_questao", joinColumns = @JoinColumn(name = "teste_id"))
    @OrderColumn(name = "ordem")
    private List<QuestaoTeste> perguntas = new ArrayList<>();

    protected Teste() {}
    public Teste(UUID docenteId, ContratosTeste.Guardar pedido) {
        id = UUID.randomUUID();
        this.docenteId = docenteId;
        criadoEm = Instant.now();
        actualizar(pedido);
    }
    public void actualizar(ContratosTeste.Guardar pedido) {
        titulo = pedido.titulo().strip();
        descricao = pedido.descricao() == null ? "" : pedido.descricao().strip();
        disponivelPratica = Boolean.TRUE.equals(pedido.disponivelPratica());
        perguntas.clear();
        pedido.perguntas().stream().map(QuestaoTeste::new).forEach(perguntas::add);
        actualizadoEm = Instant.now();
    }
    public UUID getId() { return id; }
    public Long getVersao() { return versao; }
    public ContratosTeste.Detalhe detalhe() {
        return new ContratosTeste.Detalhe(id, titulo, descricao, versao, disponivelPratica, criadoEm, actualizadoEm,
                perguntas.stream().map(QuestaoTeste::publico).toList());
    }
    public ContratosTeste.Resumo resumo() {
        return new ContratosTeste.Resumo(id, titulo, perguntas.size(), versao, disponivelPratica, actualizadoEm);
    }
}
