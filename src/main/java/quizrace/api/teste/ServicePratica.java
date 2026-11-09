package quizrace.api.teste;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static quizrace.api.teste.ContratosTeste.*;

@Service @Transactional(readOnly=true)
public class ServicePratica {
 private final TesteRepository testes; private final TentativaRepository tentativas;
 public ServicePratica(TesteRepository testes,TentativaRepository tentativas){this.testes=testes;this.tentativas=tentativas;}
 private PageRequest page(int p,int n,String campo){if(p<0||n<1||n>50)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Paginação inválida.");return PageRequest.of(p,n,Sort.by(Sort.Direction.DESC,campo));}
 public ContratosTeste.Pagina<Resumo> listar(int p,int n){var r=testes.findByDisponivelPraticaTrue(page(p,n,"actualizadoEm")).map(Teste::resumo);return new ContratosTeste.Pagina<>(r.getContent(),p,n,r.getTotalElements());}
 public Quiz obter(UUID id){var t=testes.findByIdAndDisponivelPraticaTrue(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Quiz indisponível."));return new Quiz(id,t.getVersao(),t.resumo().titulo(),t.detalhe().descricao(),java.util.stream.IntStream.range(0,t.detalhe().perguntas().size()).mapToObj(i->{var q=t.detalhe().perguntas().get(i);return new Pergunta(i+1,q.enunciado(),q.alternativaA(),q.alternativaB(),q.alternativaC(),q.alternativaD(),q.categoria(),q.tipo());}).toList());}
 @Transactional public Resultado submeter(UUID estudante,UUID id,@Valid Submissao pedido){
  var t=testes.findByIdAndDisponivelPraticaTrue(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Quiz indisponível."));
  var qs=t.detalhe().perguntas();if(!t.getVersao().equals(pedido.versao()))throw new ResponseStatusException(HttpStatus.CONFLICT,"O quiz foi actualizado. Abra a versão mais recente.");
  if(qs.isEmpty()||pedido.respostas().size()!=qs.size())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Responda a todas as perguntas.");
  int correctas=0;var feedback=new ArrayList<Feedback>();
  for(int i=0;i<qs.size();i++){var q=qs.get(i);var a=pedido.respostas().get(i);boolean ok=a.numeroPergunta()==i+1&&a.resposta()==q.correcta();if(ok)correctas++;feedback.add(new Feedback(i+1,ok,q.correcta(),q.tipo()));}
  double nota=Math.round(correctas*1000.0/qs.size())/10.0;
  tentativas.save(new TentativaPratica(estudante,id,t.resumo().titulo(),qs.size(),correctas,nota));
  return new Resultado(t.resumo().titulo(),qs.size(),correctas,nota,feedback);
 }
 public ContratosTeste.Pagina<TentativaPratica.TentativaResumo> historico(UUID estudante,int p,int n){var r=tentativas.findByUtilizadorId(estudante,page(p,n,"realizadaEm")).map(TentativaPratica::resumo);return new ContratosTeste.Pagina<>(r.getContent(),p,n,r.getTotalElements());}
 public record Pergunta(int numero,String enunciado,String alternativaA,String alternativaB,String alternativaC,String alternativaD,String categoria,TipoQuestao tipo){}
 public record Quiz(UUID id,long versao,String titulo,String descricao,List<Pergunta> perguntas){}
 public record Resposta(@Min(1) int numeroPergunta,@NotNull Alternativa resposta){}
 public record Submissao(@PositiveOrZero long versao,@NotEmpty @Size(max=50) List<@Valid Resposta> respostas){}
 public record Feedback(int numeroPergunta,boolean correcta,Alternativa respostaCorrecta,TipoQuestao tipo){}
 public record Resultado(String titulo,int totalPerguntas,int correctas,double notaPercentual,List<Feedback> feedback){}
}
