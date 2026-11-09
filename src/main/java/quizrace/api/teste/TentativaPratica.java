package quizrace.api.teste;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="tentativa_pratica")
public class TentativaPratica {
 @Id private UUID id;
 @Column(name="utilizador_id",nullable=false) private UUID utilizadorId;
 @Column(name="teste_id",nullable=false) private UUID testeId;
 @Column(nullable=false,length=150) private String titulo;
 @Column(name="total_perguntas",nullable=false) private int total;
 @Column(nullable=false) private int correctas;
 @Column(name="nota_percentual",nullable=false) private double nota;
 @Column(name="realizada_em",nullable=false) private Instant realizadaEm;
 protected TentativaPratica(){}
 public TentativaPratica(UUID utilizador,UUID teste,String titulo,int total,int correctas,double nota){id=UUID.randomUUID();utilizadorId=utilizador;testeId=teste;this.titulo=titulo;this.total=total;this.correctas=correctas;this.nota=nota;realizadaEm=Instant.now();}
 public UUID utilizadorId(){return utilizadorId;}
 public TentativaResumo resumo(){return new TentativaResumo(id,titulo,total,correctas,nota,realizadaEm);}
 public record TentativaResumo(UUID id,String titulo,int totalPerguntas,int correctas,double notaPercentual,Instant realizadaEm){}
}
