package quizrace.api.teste;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.Base64;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly=true)
public class ServiceSalas {
    private static final SecureRandom RANDOM=new SecureRandom();
    private final SalaRepository salas;
    private final ParticipanteRepository participantes;
    private final TesteRepository testes;
    private final ObjectMapper json;
    public ServiceSalas(SalaRepository salas,ParticipanteRepository participantes,TesteRepository testes,ObjectMapper json){this.salas=salas;this.participantes=participantes;this.testes=testes;this.json=json;}
    private SalaTeste sala(String codigo){return salas.findById(codigo).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Sala inexistente."));}
    private SalaTeste salaDocente(String codigo,UUID docente){var s=sala(codigo);if(!s.pertence(docente))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Sala inexistente.");return s;}
    private ParticipanteSala participante(UUID id,String token){
        var p=participantes.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Participação inexistente."));
        if(token==null||!MessageDigest.isEqual(p.tokenHash().getBytes(StandardCharsets.US_ASCII),hash(token).getBytes(StandardCharsets.US_ASCII)))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Sessão de participante inválida.");
        return p;
    }
    private static String hash(String token){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    private static String token(){byte[] b=new byte[32];RANDOM.nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
    private static String codigo(){byte[] b=new byte[9];RANDOM.nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
    private static double nota(int correctas,int total){return total==0?0:Math.round(correctas*1000.0/total)/10.0;}
    private ContratosTeste.Detalhe snapshot(SalaTeste s){try{return json.readValue(s.snapshot(),ContratosTeste.Detalhe.class);}catch(JsonProcessingException e){throw new IllegalStateException("Cópia do teste inválida.",e);}}
    private ContratoSala.PerguntaEstudante question(ContratosTeste.Questao q,int i,int segundos,long restante){
        var letras=q.tipo()==ContratosTeste.TipoQuestao.VERDADEIRO_FALSO?List.of("A","B"):List.of("A","B","C","D");
        return new ContratoSala.PerguntaEstudante(i+1,q.enunciado(),q.categoria(),q.tipo(),letras.stream().map(l->new ContratoSala.Opcao(l,switch(l){case "A"->q.alternativaA();case "B"->q.alternativaB();case "C"->q.alternativaC();default->q.alternativaD();})).toList(),segundos,restante);
    }
    @Transactional public ContratoSala.Sala criar(UUID docente,@Valid ContratoSala.Criar pedido){
        var t=testes.findByIdAndDocenteId(pedido.testeId(),docente).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Teste inexistente."));
        var detalhe=t.detalhe();if(detalhe.perguntas().isEmpty())throw new ResponseStatusException(HttpStatus.CONFLICT,"Adicione perguntas antes de criar a sala.");
        try{for(int i=0;i<3;i++){String code=codigo();if(!salas.existsById(code))return salas.saveAndFlush(new SalaTeste(docente,pedido,detalhe,json.writeValueAsString(detalhe),code)).publico();}}
        catch(JsonProcessingException e){throw new IllegalStateException("Não foi possível guardar a cópia do teste.",e);}
        throw new IllegalStateException("Não foi possível gerar um link único para a sala.");
    }
    public List<ContratoSala.Sala> listar(UUID docente){return salas.findByDocenteIdOrderByCriadaEmDesc(docente).stream().map(SalaTeste::publico).toList();}
    public ContratoSala.Sala obter(UUID docente,String codigo){return salaDocente(codigo,docente).publico();}
    @Transactional public ContratoSala.Sala fechar(UUID docente,String codigo){var s=salaDocente(codigo,docente);s.fechar();participantes.findByCodigoSalaOrderByEntrouEmAsc(codigo).forEach(ParticipanteSala::fechar);return s.publico();}
    @Transactional public ContratoSala.Participante aprovar(UUID docente,String codigo,UUID id,@Valid ContratoSala.Aprovar pedido){
        var s=salaDocente(codigo,docente);if(!s.aberta())throw new ResponseStatusException(HttpStatus.CONFLICT,"A sala já foi encerrada.");
        var p=participantes.findByIdAndCodigoSala(id,codigo).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Participante inexistente."));
        if(p.estado()!=ContratoSala.Estado.PENDENTE)throw new ResponseStatusException(HttpStatus.CONFLICT,"Esta entrada já foi analisada.");
        p.aprovar(pedido.aprovada());return p.publico(s.getTotalPerguntas(),s.aberta());
    }
    public List<ContratoSala.Participante> participantes(UUID docente,String codigo){var s=salaDocente(codigo,docente);return participantes.findByCodigoSalaOrderByEntrouEmAsc(codigo).stream().map(p->p.publico(s.getTotalPerguntas(),s.aberta())).toList();}
    @Transactional public Map<String,Object> entrar(String codigo,@Valid ContratoSala.Entrada pedido){
        var s=sala(codigo);if(!s.aberta())throw new ResponseStatusException(HttpStatus.CONFLICT,"Esta sala já foi encerrada.");
        String chave=pedido.identificacao().strip().toLowerCase(Locale.ROOT);
        if(participantes.existsByCodigoSalaAndIdentificacaoChave(codigo,chave))throw new ResponseStatusException(HttpStatus.CONFLICT,"Esta identificação já foi usada nesta sala.");
        String token=token();var p=participantes.saveAndFlush(new ParticipanteSala(codigo,pedido,hash(token)));
        return Map.of("id",p.getId().toString(),"token",token,"estado",p.estado().name(),"mensagem","A docente vai validar a sua entrada.");
    }
    @Transactional public ContratoSala.EstadoParticipante estado(UUID id,String token){
        var p=participante(id,token);var s=sala(p.codigoSala());p.ver();
        ContratosTeste.Detalhe t=snapshot(s);
        if(!s.aberta())return new ContratoSala.EstadoParticipante(p.estado(),s.getCodigo(),s.publico().nome(),p.respondidas(),s.getTotalPerguntas(),null,p.estado()==ContratoSala.Estado.FECHADO,p.estado()==ContratoSala.Estado.FECHADO?nota(p.correctas(),s.getTotalPerguntas()):null);
        ContratoSala.PerguntaEstudante pergunta=null;
        if(p.aprovada()&&p.respondidas()<t.perguntas().size()){
            p.iniciarPergunta();var iniciado=p.perguntaIniciadaEm();long restantes=Math.max(0,s.getSegundosPorPergunta()-Duration.between(iniciado,Instant.now()).toSeconds());
            pergunta=question(t.perguntas().get(p.respondidas()),p.respondidas(),s.getSegundosPorPergunta(),restantes);
        }
        return new ContratoSala.EstadoParticipante(p.estado(),s.getCodigo(),s.publico().nome(),p.respondidas(),s.getTotalPerguntas(),pergunta,p.respondidas()>=s.getTotalPerguntas(),null);
    }
    @Transactional public void cursor(UUID id,String token,@Valid ContratoSala.Cursor c){var p=participante(id,token);var s=sala(p.codigoSala());if(!s.aberta()||!p.aprovada())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"A sala não recebe posições nesta sessão.");p.mover(c.x(),c.y());}
    @Transactional public ContratoSala.Recebida responder(UUID id,String token,@Valid ContratoSala.Responder resposta){
        var p=participante(id,token);var s=sala(p.codigoSala());
        if(!s.aberta()||!p.aprovada())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"A sala não aceita respostas nesta sessão.");
        var t=snapshot(s);if(p.respondidas()>=t.perguntas().size())throw new ResponseStatusException(HttpStatus.CONFLICT,"Já respondeu a todas as perguntas.");
        if(resposta.numeroPergunta()!=p.respondidas()+1)throw new ResponseStatusException(HttpStatus.CONFLICT,"A pergunta actual mudou. Actualize a sala.");
        if(p.perguntaIniciadaEm()==null)throw new ResponseStatusException(HttpStatus.CONFLICT,"Abra a pergunta antes de responder.");
        var q=t.perguntas().get(p.respondidas());boolean expirou=Duration.between(p.perguntaIniciadaEm(),Instant.now()).toMillis()>s.getSegundosPorPergunta()*1000L;
        String valor=resposta.resposta()==null?"":resposta.resposta().name();boolean certa=!expirou&&!valor.isEmpty()&&q.correcta().name().equals(valor);
        p.resposta(expirou?"":valor,certa);
        return new ContratoSala.Recebida(true,expirou?"O tempo terminou. A pergunta foi registada como errada.":"Resposta registada.");
    }
}
