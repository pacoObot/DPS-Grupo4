package quizrace.api.security;

import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import quizrace.api.user.*;

@Component
public class AdministradorInicial implements ApplicationRunner {
    private final UtilizadorRepository users;
    private final UtilizadorService service;
    private final EntityManager entityManager;
    private final String email;
    private final String password;
    public AdministradorInicial(UtilizadorRepository users, UtilizadorService service, EntityManager entityManager,
        @Value("${quiz.bootstrap.email}") String email, @Value("${quiz.bootstrap.password}") String password) {
        this.users = users;
        this.service = service;
        this.entityManager = entityManager;
        this.email = email;
        this.password = password;
    }
    @Override @Transactional
    public void run(ApplicationArguments args) {
        if (email.isBlank() && password.isBlank()) return;
        entityManager.createNativeQuery("SELECT id FROM bloqueio_administracao WHERE id = 1 FOR UPDATE").getSingleResult();
        if (users.count() != 0) return; // Não redefine palavras-passe nem promove contas existentes.
        service.criar(new Contratos.Criar("Administrador", email, password, Perfil.ADMINISTRADOR));
    }
}
