package quizrace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
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

@SpringBootTest(classes = QuizRaceApplication.class, properties = {
    "spring.datasource.url=jdbc:h2:mem:testes;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "quiz.security.secret=chave-apenas-para-testes-32-bytes-minimo",
    "quiz.bootstrap.email=", "quiz.bootstrap.password="
})
@AutoConfigureMockMvc
class TestesApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UtilizadorRepository users;
    @Autowired TesteRepository testes;
    @Autowired TesteRevisaoRepository revisoes;
    @Autowired JwtEncoder encoder;
    String docente, outro, estudante, admin;

    @BeforeEach void preparar() {
        revisoes.deleteAll();
        testes.deleteAll();
        users.deleteAll();
        docente = conta("docente", Perfil.DOCENTE);
        outro = conta("outro", Perfil.DOCENTE);
        estudante = conta("estudante", Perfil.ESTUDANTE);
        admin = conta("admin", Perfil.ADMINISTRADOR);
    }
    String conta(String nome, Perfil perfil) {
        var u = users.saveAndFlush(new Utilizador(nome, nome + "@example.org", "hash-nao-usado-neste-teste", perfil));
        var now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer("quizrace").subject(u.getId().toString())
                .issuedAt(now).expiresAt(now.plusSeconds(900)).claim("ver", u.getVersaoAuth()).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
    Map<String, Object> pergunta(String enunciado) {
        return Map.of("enunciado", enunciado, "alternativaA", "Java", "alternativaB", "Python",
                "alternativaC", "C", "alternativaD", "Rust", "correcta", "A", "categoria", "DPS");
    }
    ObjectNode pedido() {
        return json.valueToTree(Map.of("titulo", "Teste DPS", "descricao", "Leia com atenção.",
                "perguntas", List.of(pergunta("Qual linguagem usamos?"), pergunta("Qual linguagem tem JVM?"))));
    }
    ResultActions criar(String token, ObjectNode body) throws Exception {
        return mvc.perform(post("/api/v1/testes").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body)));
    }
    JsonNode criar() throws Exception {
        return json.readTree(criar(docente, pedido()).andExpect(status().isCreated())
                .andExpect(header().exists("Location")).andExpect(jsonPath("$.versao").value(0))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andReturn().getResponse().getContentAsString());
    }
    ResultActions ler(String token, String rota) throws Exception {
        return mvc.perform(get(rota).header("Authorization", "Bearer " + token));
    }
    ResultActions alterar(String token, String id, ObjectNode body) throws Exception {
        return mvc.perform(put("/api/v1/testes/" + id).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body)));
    }
    @Test void criaConsultaEListaApenasOsPropriosTestes() throws Exception {
        var criado = criar();
        String rota = "/api/v1/testes/" + criado.get("id").asText();
        ler(docente, rota).andExpect(status().isOk()).andExpect(jsonPath("$.perguntas[0].correcta").value("A"));
        ler(docente, "/api/v1/testes").andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].totalPerguntas").value(2))
                .andExpect(jsonPath("$.content[0].perguntas").doesNotExist());
        ler(outro, "/api/v1/testes").andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        assertEquals(1, testes.count());
        assertEquals(1, revisoes.count());
    }
    @Test void outroDocenteNaoLeNemAlteraTesteOuHistorico() throws Exception {
        String id = criar().get("id").asText();
        for (String sufixo : List.of("", "/historico", "/historico/0"))
            ler(outro, "/api/v1/testes/" + id + sufixo).andExpect(status().isNotFound());
        alterar(outro, id, pedido().put("versao", 0)).andExpect(status().isNotFound());
        assertEquals(1, revisoes.count());
    }
    @Test void soDocentesPodemPrepararOuConsultarGabaritos() throws Exception {
        String id = criar().get("id").asText();
        mvc.perform(get("/api/v1/testes")).andExpect(status().isUnauthorized());
        for (String token : List.of(estudante, admin)) {
            criar(token, pedido()).andExpect(status().isForbidden());
            for (String sufixo : List.of("", "/" + id, "/" + id + "/historico", "/" + id + "/historico/0"))
                ler(token, "/api/v1/testes" + sufixo).andExpect(status().isForbidden());
            alterar(token, id, pedido().put("versao", 0)).andExpect(status().isForbidden());
        }
    }
    @Test void guardaOrdemRemovePerguntasEPreservaCopiaHistorica() throws Exception {
        String id = criar().get("id").asText();
        var body = pedido().put("versao", 0).put("titulo", "Nova versão");
        body.set("perguntas", json.valueToTree(List.of(pergunta("Qual linguagem tem JVM?"), pergunta("Qual linguagem usamos?"))));
        var novo = json.readTree(alterar(docente, id, body).andExpect(status().isOk())
                .andExpect(jsonPath("$.perguntas[0].enunciado").value("Qual linguagem tem JVM?"))
                .andReturn().getResponse().getContentAsString());
        body.put("versao", novo.get("versao").asLong());
        body.set("perguntas", json.valueToTree(List.of(pergunta("Pergunta final"))));
        alterar(docente, id, body).andExpect(status().isOk()).andExpect(jsonPath("$.perguntas.length()").value(1));
        ler(docente, "/api/v1/testes/" + id).andExpect(jsonPath("$.perguntas[0].enunciado").value("Pergunta final"));
        ler(docente, "/api/v1/testes/" + id + "/historico/0").andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Teste DPS"))
                .andExpect(jsonPath("$.perguntas.length()").value(2));
        ler(docente, "/api/v1/testes/" + id + "/historico?size=1").andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3)).andExpect(jsonPath("$.content.length()").value(1));
    }
    @Test void versaoAntigaNaoSobrescreveEdicaoNova() throws Exception {
        String id = criar().get("id").asText();
        var body = pedido().put("versao", 0).put("titulo", "Guardado primeiro");
        alterar(docente, id, body).andExpect(status().isOk());
        alterar(docente, id, body.put("titulo", "Edicao antiga")).andExpect(status().isConflict());
        ler(docente, "/api/v1/testes/" + id).andExpect(jsonPath("$.titulo").value("Guardado primeiro"));
        assertEquals(2, revisoes.count());
    }
    @Test void duasGravacoesConcorrentesNaoPerdemActualizacoes() throws Exception {
        String id = criar().get("id").asText();
        var inicio = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<Integer> gravar = () -> {
                assertTrue(inicio.await(5, TimeUnit.SECONDS));
                return alterar(docente, id, pedido().put("versao", 0)).andReturn().getResponse().getStatus();
            };
            var a = pool.submit(gravar); var b = pool.submit(gravar);
            inicio.countDown();
            var estados = List.of(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
            assertTrue(estados.contains(200), estados.toString());
            assertTrue(estados.contains(409), estados.toString());
            assertEquals(2, revisoes.count());
        }
    }
    @Test void validaPerguntasAninhadasLimitesEIdentidade() throws Exception {
        criar(docente, pedido().put("titulo", "   ")).andExpect(status().isBadRequest());
        var body = pedido();
        ((ObjectNode) body.get("perguntas").get(0)).put("alternativaA", " ");
        criar(docente, body).andExpect(status().isBadRequest());
        body = pedido(); ((ObjectNode) body.get("perguntas").get(0)).put("correcta", "E");
        criar(docente, body).andExpect(status().isBadRequest());
        body = pedido(); body.set("perguntas", json.createArrayNode().addNull());
        criar(docente, body).andExpect(status().isBadRequest());
        body = pedido(); body.set("perguntas", json.valueToTree(java.util.Collections.nCopies(51, pergunta("Pergunta"))));
        criar(docente, body).andExpect(status().isBadRequest());
        criar(docente, pedido().put("docenteId", users.findAll().getFirst().getId().toString()))
                .andExpect(status().isBadRequest());
        criar(docente, pedido().put("versao", 1)).andExpect(status().isBadRequest());
        assertEquals(0, testes.count());
        assertEquals(0, revisoes.count());
    }
    @Test void rascunhoVazioPermitidoEParametrosInvalidosControlados() throws Exception {
        var body = pedido(); body.set("perguntas", json.createArrayNode());
        var criado = json.readTree(criar(docente, body).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        String id = criado.get("id").asText();
        alterar(docente, id, pedido()).andExpect(status().isBadRequest());
        ler(docente, "/api/v1/testes?size=51").andExpect(status().isBadRequest());
        ler(docente, "/api/v1/testes?page=-1").andExpect(status().isBadRequest());
        ler(docente, "/api/v1/testes/invalido").andExpect(status().isBadRequest());
        ler(docente, "/api/v1/testes/" + id + "/historico/999").andExpect(status().isNotFound());
    }
}
