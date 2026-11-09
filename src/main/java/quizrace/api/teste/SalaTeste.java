package quizrace.api.teste;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
public class SalaTeste {
    @Id @Column(length=16) private String codigo;
    @Column(nullable=false) private UUID docenteId;
    @Column(nullable=false) private UUID testeId;
    @Column(nullable=false,length=100) private String nome;
    @Column(nullable=false,length=150) private String tituloTeste;
    @Column(nullable=false) private int totalPerguntas;
    @Column(nullable=false) private int segundosPorPergunta;
    @Column(nullable=false,columnDefinition="text") private String snapshot;
    @Column(nullable=false) private Instant criadaEm;
    private Instant fechadaEm;

    protected SalaTeste() {}
    public SalaTeste(UUID docente, ContratoSala.Criar pedido, ContratosTeste.Detalhe teste, String snapshot, String codigo) {
        docenteId=docente; testeId=teste.id(); nome=pedido.nome().strip(); tituloTeste=teste.titulo();
        totalPerguntas=teste.perguntas().size(); segundosPorPergunta=pedido.segundosPorPergunta();
        this.snapshot=snapshot; this.codigo=codigo; criadaEm=Instant.now();
    }
    public boolean pertence(UUID docente) { return docenteId.equals(docente); }
    public boolean aberta() { return fechadaEm==null; }
    public String getCodigo() { return codigo; }
    public UUID getDocenteId() { return docenteId; }
    public UUID getTesteId() { return testeId; }
    public int getTotalPerguntas() { return totalPerguntas; }
    public int getSegundosPorPergunta() { return segundosPorPergunta; }
    public Instant criadaEm() { return criadaEm; }
    public String snapshot() { return snapshot; }
    public void fechar() { if (fechadaEm==null) fechadaEm=Instant.now(); }
    public ContratoSala.Sala publico() { return new ContratoSala.Sala(codigo,nome,tituloTeste,totalPerguntas,segundosPorPergunta,aberta(),criadaEm,fechadaEm); }
}
