package quizrace.api.user;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@Service
@Validated
public class UtilizadorService {
    private final UtilizadorRepository repository;
    private final PasswordEncoder passwords;
    private final EntityManager entityManager;

    public UtilizadorService(UtilizadorRepository repository, PasswordEncoder passwords, EntityManager entityManager) {
        this.repository = repository;
        this.passwords = passwords;
        this.entityManager = entityManager;
    }
    public static String normalizarEmail(String email) { return email.strip().toLowerCase(Locale.ROOT); }

    @Transactional
    public Utilizador criar(@Valid Contratos.Criar pedido) {
        String nome = pedido.nome().strip();
        if (nome.length() < 2 || nome.chars().anyMatch(Character::isISOControl)
                || pedido.palavraPasse().getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome ou palavra-passe inválidos.");
        return repository.saveAndFlush(new Utilizador(nome, normalizarEmail(pedido.email()),
                passwords.encode(pedido.palavraPasse()), pedido.perfil()));
    }

    @Transactional
    public Utilizador alterar(UUID id, Contratos.Alterar pedido) {
        if (pedido.perfil() == null && pedido.activo() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indique perfil ou activo.");
        // Uma linha partilhada serializa alterações administrativas também entre várias instâncias da API.
        entityManager.createNativeQuery("SELECT id FROM bloqueio_administracao WHERE id = 1 FOR UPDATE").getSingleResult();
        Utilizador u = repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilizador inexistente."));
        boolean retiraAdministrador = u.isActivo() && u.getPerfil() == Perfil.ADMINISTRADOR
                && (Boolean.FALSE.equals(pedido.activo())
                    || pedido.perfil() != null && pedido.perfil() != Perfil.ADMINISTRADOR);
        if (retiraAdministrador && repository.countByPerfilAndActivoTrue(Perfil.ADMINISTRADOR) <= 1)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "É necessário manter um administrador activo.");
        u.alterar(pedido.perfil(), pedido.activo());
        return repository.saveAndFlush(u);
    }
}
