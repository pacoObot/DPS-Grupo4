package quizrace.api.teste;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
import java.time.Duration;

@Entity
@Table(name="participante_sala",uniqueConstraints=@UniqueConstraint(name="uq_sala_identificacao",columnNames={"codigo_sala","identificacao_chave"}))
public class ParticipanteSala {
    @Id private UUID id;
    @Column(name="codigo_sala",nullable=false,length=16) private String codigoSala;
    @Column(nullable=false,length=100) private String nome;
    @Column(nullable=false,length=40) private String identificacao;
    @Column(name="identificacao_chave",nullable=false,length=100) private String identificacaoChave;
    @Column(nullable=false,length=64) private String tokenHash;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private ContratoSala.Dispositivo dispositivo;
    @Column(name="cursor_x") private Integer cursorX;
    @Column(name="cursor_y") private Integer cursorY;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private ContratoSala.Estado estado;
    @Column(nullable=false) private Instant entrouEm;
    @Column(nullable=false) private Instant vistoEm;
    @Column(name="pergunta_iniciada_em") private Instant perguntaIniciadaEm;
    @ElementCollection
    @CollectionTable(name="resposta_participante",joinColumns=@JoinColumn(name="participante_id"))
    @OrderColumn(name="ordem") private List<RespostaParticipante> respostas=new ArrayList<>();
    protected ParticipanteSala() {}
    public ParticipanteSala(String codigo,ContratoSala.Entrada pedido,String hash) {
        id=UUID.randomUUID();codigoSala=codigo;nome=pedido.nome().strip();identificacao=pedido.identificacao().strip();
        identificacaoChave=identificacao.toLowerCase(Locale.ROOT);tokenHash=hash;dispositivo=pedido.dispositivo();estado=ContratoSala.Estado.PENDENTE;
        entrouEm=Instant.now();vistoEm=entrouEm;
    }
    public UUID getId(){return id;} public String codigoSala(){return codigoSala;} public String tokenHash(){return tokenHash;}
    public int respondidas(){return respostas.size();} public boolean aprovada(){return estado==ContratoSala.Estado.APROVADO;}
    public void ver(){vistoEm=Instant.now();}
    public void mover(int x,int y){cursorX=x;cursorY=y;vistoEm=Instant.now();}
    public void iniciarPergunta(){vistoEm=Instant.now();if(perguntaIniciadaEm==null)perguntaIniciadaEm=Instant.now();}
    public Instant perguntaIniciadaEm(){return perguntaIniciadaEm;}
    public void resposta(String valor,boolean correcta){respostas.add(new RespostaParticipante(valor,correcta));perguntaIniciadaEm=null;vistoEm=Instant.now();}
    public void aprovar(boolean valor){estado=valor?ContratoSala.Estado.APROVADO:ContratoSala.Estado.RECUSADO;vistoEm=Instant.now();}
    public void fechar(){if(estado==ContratoSala.Estado.APROVADO)estado=ContratoSala.Estado.FECHADO;else if(estado==ContratoSala.Estado.PENDENTE)estado=ContratoSala.Estado.RECUSADO;}
    public int correctas(){return (int)respostas.stream().filter(RespostaParticipante::correcta).count();}
    public ContratoSala.Participante publico(int total,boolean salaAberta){
        double nota=total==0?0:Math.round(correctas()*1000.0/total)/10.0;
        return new ContratoSala.Participante(id.toString(),nome,identificacao,estado,respostas.size(),total, estado==ContratoSala.Estado.FECHADO?correctas():0,estado==ContratoSala.Estado.FECHADO?nota:0,dispositivo,Duration.between(vistoEm,Instant.now()).toSeconds()<8,cursorX,cursorY,entrouEm,vistoEm);
    }
    public ContratoSala.Estado estado(){return estado;}
}
