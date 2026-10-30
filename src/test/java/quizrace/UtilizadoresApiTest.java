package quizrace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.*;
import quizrace.api.QuizRaceApplication;
import quizrace.api.user.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = QuizRaceApplication.class, properties = {
    "spring.datasource.url=jdbc:h2:mem:users;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "quiz.security.secret=chave-apenas-para-testes-32-bytes-minimo",
    "quiz.bootstrap.email=", "quiz.bootstrap.password="
})
@AutoConfigureMockMvc
class UtilizadoresApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UtilizadorRepository users;
    @Autowired UtilizadorService service;
    @Autowired org.springframework.security.oauth2.jwt.JwtEncoder encoder;
    private static final String PASSWORD = "UmaPalavraPasse123!";
    private String adminToken;
    private Utilizador admin;

    @BeforeEach void preparar() throws Exception {
        users.deleteAll();
        admin = service.criar(new Contratos.Criar("Administrador", "admin@example.org", PASSWORD, Perfil.ADMINISTRADOR));
        adminToken = login("admin@example.org", PASSWORD);
    }
    private String login(String email, String password) throws Exception {
        var result = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("email", email, "palavraPasse", password))))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store")).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }
    private ResultActions criar(String email, String perfil) throws Exception {
        return mvc.perform(post("/api/v1/users").header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                "nome", "Ana Silva", "email", email, "palavraPasse", PASSWORD, "perfil", perfil))));
    }
    @Test void criaPersisteEAutenticaSemExporHash() throws Exception {
        criar("Ana@Example.org", "ESTUDANTE").andExpect(status().isCreated())
            .andExpect(header().exists("Location")).andExpect(jsonPath("$.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.palavraPasse").doesNotExist()).andExpect(jsonPath("$.email").value("ana@example.org"));
        var stored = users.findByEmail("ana@example.org").orElseThrow();
        assertNotEquals(PASSWORD, stored.getPasswordHash());
        assertTrue(stored.getPasswordHash().startsWith("$2a$12$"));
        String token = login("ANA@example.org", PASSWORD);
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.perfil").value("ESTUDANTE"));
    }
    @Test void emailUnicoIgnoraMaiusculas() throws Exception {
        criar("ana@example.org", "ESTUDANTE").andExpect(status().isCreated());
        criar("ANA@example.org", "DOCENTE").andExpect(status().isConflict());
        assertEquals(2, users.count());
    }
    @Test void estudanteEDocenteNaoGeremContas() throws Exception {
        for (Perfil perfil : new Perfil[]{Perfil.ESTUDANTE, Perfil.DOCENTE}) {
            var u = service.criar(new Contratos.Criar("Teste", perfil + "@example.org", PASSWORD, perfil));
            String token = login(u.getEmail(), PASSWORD);
            mvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
            mvc.perform(patch("/api/v1/users/" + u.getId()).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"perfil\":\"ADMINISTRADOR\"}"))
                .andExpect(status().isForbidden());
            mvc.perform(post("/api/v1/users").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        }
    }
    @Test void faltaOuFalsificacaoDeTokenRecusaAcesso() throws Exception {
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/users").header("Authorization", "Bearer falso")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + adminToken + "x"))
            .andExpect(status().isUnauthorized());
    }
    @Test void tokenExpiradoOuEmissorErradoNaoAutentica() throws Exception {
        for (String issuer : new String[]{"quizrace", "outro"}) {
            var now = java.time.Instant.now();
            var claims = org.springframework.security.oauth2.jwt.JwtClaimsSet.builder()
                .issuer(issuer).subject(admin.getId().toString()).issuedAt(now.minusSeconds(7200))
                .expiresAt(issuer.equals("quizrace") ? now.minusSeconds(3600) : now.plusSeconds(900))
                .claim("ver", 0L).build();
            String token = encoder.encode(org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(
                org.springframework.security.oauth2.jwt.JwsHeader.with(
                    org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build(), claims)).getTokenValue();
            mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        }
    }
    @Test void representacaoDeDtosNaoRegistaCredenciais() {
        assertFalse(new Contratos.Login("x@example.org", PASSWORD).toString().contains(PASSWORD));
        assertFalse(new Contratos.Criar("Teste", "x@example.org", PASSWORD, Perfil.DOCENTE).toString().contains(PASSWORD));
        assertFalse(new Contratos.Token("segredo", "Bearer", 900, null).toString().contains("segredo"));
    }
    @Test void contaDesactivadaPerdeAcessoELogin() throws Exception {
        var u = service.criar(new Contratos.Criar("Ana Silva", "ana@example.org", PASSWORD, Perfil.ESTUDANTE));
        String token = login(u.getEmail(), PASSWORD);
        mvc.perform(patch("/api/v1/users/" + u.getId()).header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("email", u.getEmail(), "palavraPasse", PASSWORD))))
            .andExpect(status().isUnauthorized());
        service.alterar(u.getId(), new Contratos.Alterar(null, true));
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }
    @Test void alterarPerfilRevogaTokenAnterior() throws Exception {
        var u = service.criar(new Contratos.Criar("Ana Silva", "ana@example.org", PASSWORD, Perfil.ESTUDANTE));
        String token = login(u.getEmail(), PASSWORD);
        mvc.perform(patch("/api/v1/users/" + u.getId()).header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON).content("{\"perfil\":\"DOCENTE\"}")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + login(u.getEmail(), PASSWORD)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.perfil").value("DOCENTE"));
    }
    @Test void preservaUltimoAdministrador() throws Exception {
        mvc.perform(patch("/api/v1/users/" + admin.getId()).header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}")).andExpect(status().isConflict());
        assertTrue(users.findById(admin.getId()).orElseThrow().isActivo());
    }
    @Test void validaCamposERejeitaCamposDesconhecidos() throws Exception {
        criar("invalido", "ESTUDANTE").andExpect(status().isBadRequest());
        criar("ana@example.org", "SUPERUSER").andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/users").header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON).content("{\"passwordHash\":\"falso\"}")).andExpect(status().isBadRequest());
        mvc.perform(patch("/api/v1/users/" + admin.getId()).header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
        assertThrows(Exception.class, () -> service.criar(new Contratos.Criar("A", "x@example.org", "curta", Perfil.ESTUDANTE)));
        assertThrows(Exception.class, () -> service.criar(new Contratos.Criar("Ana", "x@example.org", "á".repeat(40), Perfil.ESTUDANTE)));
    }
    @Test void errosDeLoginNaoRevelamExistenciaDaConta() throws Exception {
        String primeiro = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"admin@example.org\",\"palavraPasse\":\"invalida\"}"))
            .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String segundo = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"ausente@example.org\",\"palavraPasse\":\"invalida\"}"))
            .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        assertEquals(primeiro, segundo);
    }
    @Test void listaPaginadaEConsultaComErrosControlados() throws Exception {
        mvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist());
        mvc.perform(get("/api/v1/users?size=101").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/users/00000000-0000-0000-0000-000000000000").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/users/invalido").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isBadRequest());
    }
}
