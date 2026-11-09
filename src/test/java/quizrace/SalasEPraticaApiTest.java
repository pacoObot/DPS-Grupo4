package quizrace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.web.servlet.*;
import quizrace.api.QuizRaceApplication;
import quizrace.api.teste.*;
import quizrace.api.user.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes=QuizRaceApplication.class,properties={
 "spring.datasource.url=jdbc:h2:mem:salas;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
 "spring.datasource.username=sa","spring.datasource.password=",
 "quiz.security.secret=chave-apenas-para-testes-32-bytes-minimo","quiz.bootstrap.email=","quiz.bootstrap.password="})
@AutoConfigureMockMvc
class SalasEPraticaApiTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired UtilizadorRepository users;
 @Autowired TesteRepository testes; @Autowired TesteRevisaoRepository revisoes;
 @Autowired SalaRepository salas; @Autowired ParticipanteRepository participantes; @Autowired TentativaRepository tentativas; @Autowired JwtEncoder encoder;
 String docente, outro, estudante, admin; JsonNode teste; String codigo;
 @BeforeEach void preparar() throws Exception {
  tentativas.deleteAll();participantes.deleteAll();salas.deleteAll();revisoes.deleteAll();testes.deleteAll();users.deleteAll();
  docente=conta("docente",Perfil.DOCENTE);outro=conta("outro",Perfil.DOCENTE);estudante=conta("estudante",Perfil.ESTUDANTE);admin=conta("admin",Perfil.ADMINISTRADOR);
  var body=Map.of("titulo","Quiz demonstração","descricao","Escolha uma resposta.","disponivelPratica",true,"perguntas",List.of(
   Map.of("enunciado","Java tem JVM?","alternativaA","Sim","alternativaB","Não","alternativaC","Talvez","alternativaD","Não sei","correcta","A","categoria","Java","tipo","MULTIPLA_ESCOLHA"),
   Map.of("enunciado","O céu é verde?","alternativaA","Verdadeiro","alternativaB","Falso","alternativaC","","alternativaD","","correcta","B","categoria","Ciência","tipo","VERDADEIRO_FALSO")));
  teste=json.readTree(mvc.perform(post("/api/v1/testes").header("Authorization","Bearer "+docente).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
  codigo=json.readTree(mvc.perform(post("/api/v1/salas").header("Authorization","Bearer "+docente).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(Map.of("testeId",teste.get("id").asText(),"nome","Defesa de apresentação","segundosPorPergunta",60)))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("codigo").asText();
 }
 String conta(String nome,Perfil perfil){var u=users.saveAndFlush(new Utilizador(nome,nome+"@example.org","hash-nao-usado",perfil));var now=Instant.now();return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),JwtClaimsSet.builder().issuer("quizrace").subject(u.getId().toString()).issuedAt(now).expiresAt(now.plusSeconds(900)).claim("ver",u.getVersaoAuth()).build())).getTokenValue();}
 ResultActions req(String metodo,String url,String token)throws Exception {return mvc.perform(get(url).header("Authorization","Bearer "+token));}
 JsonNode entrar(String ident,String nome)throws Exception {var b=Map.of("codigo",codigo,"entrada",Map.of("nome",nome,"identificacao",ident,"dispositivo","COMPUTADOR"));return json.readTree(mvc.perform(post("/api/v1/salas/entrada").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(b))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());}
 ResultActions estado(JsonNode p)throws Exception{return mvc.perform(get("/api/v1/salas/participacoes/"+p.get("id").asText()).header("X-Quiz-Participante",p.get("token").asText()));}
 ResultActions aprovar(JsonNode p,boolean ok,String token)throws Exception{return mvc.perform(patch("/api/v1/salas/"+codigo+"/participantes/"+p.get("id").asText()).header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content("{\"aprovada\":"+ok+"}"));}
 @Test void linkAdmiteEstudanteAposAprovacaoEDocenteAcompanhaProgressoEEncerra()throws Exception {
  var p=entrar("20260077","Ana Silva");
  estado(p).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("PENDENTE")).andExpect(jsonPath("$.pergunta").doesNotExist());
  mvc.perform(get("/api/v1/salas/"+codigo+"/participantes").header("Authorization","Bearer "+docente)).andExpect(status().isOk()).andExpect(jsonPath("$[0].identificacao").value("20260077"));
  aprovar(p,true,docente).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("APROVADO"));
  var primeira=estado(p).andExpect(status().isOk()).andExpect(jsonPath("$.pergunta.enunciado").value("Java tem JVM?"))
    .andExpect(jsonPath("$.pergunta.opcoes.length()").value(4)).andReturn();
  String q1=json.readTree(primeira.getResponse().getContentAsString()).get("pergunta").get("numero").asText();assertEquals("1",q1);
  estado(p).andExpect(jsonPath("$.pergunta.correcta").doesNotExist());
  var resposta=Map.of("numeroPergunta",1,"resposta","A");
  mvc.perform(post("/api/v1/salas/participacoes/"+p.get("id").asText()+"/respostas").header("X-Quiz-Participante",p.get("token").asText()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(resposta))).andExpect(status().isOk());
  estado(p).andExpect(jsonPath("$.respondidas").value(1)).andExpect(jsonPath("$.pergunta.numero").value(2)).andExpect(jsonPath("$.pergunta.tipo").value("VERDADEIRO_FALSO")).andExpect(jsonPath("$.pergunta.opcoes.length()").value(2));
  mvc.perform(post("/api/v1/salas/participacoes/"+p.get("id").asText()+"/respostas").header("X-Quiz-Participante",p.get("token").asText()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("numeroPergunta",2,"resposta","B")))).andExpect(status().isOk());
  mvc.perform(get("/api/v1/salas/"+codigo+"/participantes").header("Authorization","Bearer "+docente)).andExpect(jsonPath("$[0].respondidas").value(2)).andExpect(jsonPath("$[0].notaPercentual").value(0));
  mvc.perform(post("/api/v1/salas/"+codigo+"/fechar").header("Authorization","Bearer "+docente)).andExpect(status().isOk()).andExpect(jsonPath("$.aberta").value(false));
  mvc.perform(get("/api/v1/salas/"+codigo+"/participantes").header("Authorization","Bearer "+docente)).andExpect(jsonPath("$[0].correctas").value(2)).andExpect(jsonPath("$[0].notaPercentual").value(100));
  estado(p).andExpect(jsonPath("$.estado").value("FECHADO")).andExpect(jsonPath("$.nota").value(100));
 }
 @Test void aprovaRestritaAoDonoIdentificacaoDuplicadaERecusaRespostasInvalidas()throws Exception {
  var p=entrar("20260077","Ana Silva");var c=json.readTree(mvc.perform(post("/api/v1/salas/entrada").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("codigo",codigo,"entrada",Map.of("nome","Outra","identificacao"," 20260077 ","dispositivo","COMPUTADOR"))))).andExpect(status().isConflict()).andReturn().getResponse().getContentAsString());
  assertTrue(c.get("mensagem").asText().contains("já foi usada"));
  aprovar(p,true,outro).andExpect(status().isNotFound());aprovar(p,false,docente).andExpect(status().isOk());
  estado(p).andExpect(jsonPath("$.estado").value("RECUSADO"));
  var outroP=entrar("20260078","Bia");aprovar(outroP,true,docente).andExpect(status().isOk());estado(outroP).andExpect(status().isOk());
  mvc.perform(post("/api/v1/salas/participacoes/"+outroP.get("id").asText()+"/respostas").header("X-Quiz-Participante","token-falso").contentType(MediaType.APPLICATION_JSON).content("{\"numeroPergunta\":1,\"resposta\":\"A\"}")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/v1/salas/participacoes/"+outroP.get("id").asText()+"/respostas").header("X-Quiz-Participante",outroP.get("token").asText()).contentType(MediaType.APPLICATION_JSON).content("{\"numeroPergunta\":2,\"resposta\":\"A\"}")).andExpect(status().isConflict());
 }
 @Test void estudanteVeQuizzesDisponiveisSemGabaritoEPodeTreinarEConsultarHistorico()throws Exception {
  mvc.perform(get("/api/v1/quizzes/pratica").header("Authorization","Bearer "+estudante)).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
  mvc.perform(get("/api/v1/quizzes/pratica/"+teste.get("id").asText()).header("Authorization","Bearer "+estudante)).andExpect(status().isOk()).andExpect(jsonPath("$.perguntas[0].correcta").doesNotExist()).andExpect(jsonPath("$.perguntas[1].tipo").value("VERDADEIRO_FALSO"));
  mvc.perform(post("/api/v1/quizzes/pratica/"+teste.get("id").asText()+"/respostas").header("Authorization","Bearer "+estudante).contentType(MediaType.APPLICATION_JSON).content("{\"versao\":0,\"respostas\":[{\"numeroPergunta\":1,\"resposta\":\"A\"},{\"numeroPergunta\":2,\"resposta\":\"B\"}]}")).andExpect(status().isOk()).andExpect(jsonPath("$.notaPercentual").value(100)).andExpect(jsonPath("$.feedback[1].respostaCorrecta").value("B"));
  mvc.perform(get("/api/v1/quizzes/pratica/historico").header("Authorization","Bearer "+estudante)).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].correctas").value(2));
  mvc.perform(get("/api/v1/quizzes/pratica").header("Authorization","Bearer "+docente)).andExpect(status().isOk());
  mvc.perform(get("/api/v1/quizzes/pratica").header("Authorization","Bearer "+admin)).andExpect(status().isForbidden());
 }
 @Test void naoExpõeQuestoesNemAceitaEntradasEmSalaFechada()throws Exception {
  var p=entrar("20260077","Ana");mvc.perform(post("/api/v1/salas/"+codigo+"/fechar").header("Authorization","Bearer "+docente)).andExpect(status().isOk());
  estado(p).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("RECUSADO")).andExpect(jsonPath("$.pergunta").doesNotExist()).andExpect(jsonPath("$.nota").doesNotExist());
  mvc.perform(post("/api/v1/salas/entrada").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("codigo",codigo,"entrada",Map.of("nome","Bia","identificacao","20260078","dispositivo","TELEMOVEL"))))).andExpect(status().isConflict());
 }
}
