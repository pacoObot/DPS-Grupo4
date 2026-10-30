package quizrace.api;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import quizrace.api.user.*;
import quizrace.api.security.AutenticacaoService;

@RestController
@RequestMapping("/api/v1")
public class UtilizadoresController {
    private final UtilizadorService service;
    private final UtilizadorRepository users;
    private final AutenticacaoService auth;
    public UtilizadoresController(UtilizadorService service, UtilizadorRepository users, AutenticacaoService auth) {
        this.service = service;
        this.users = users;
        this.auth = auth;
    }
    @PostMapping("/users")
    public ResponseEntity<Contratos.Publico> criar(@Valid @RequestBody Contratos.Criar pedido) {
        var user = service.criar(pedido);
        return ResponseEntity.created(URI.create("/api/v1/users/" + user.getId())).body(Contratos.Publico.de(user));
    }
    @GetMapping("/users")
    public java.util.Map<String, Object> listar(@RequestParam(name = "page", defaultValue = "0") int page,
                                                @RequestParam(name = "size", defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paginação inválida.");
        var pagina = users.findAll(PageRequest.of(page, size, Sort.by("email"))).map(Contratos.Publico::de);
        return java.util.Map.of("content", pagina.getContent(), "page", page, "size", size,
                               "totalElements", pagina.getTotalElements());
    }
    @GetMapping("/users/{id}")
    public Contratos.Publico obter(@PathVariable("id") UUID id) {
        return Contratos.Publico.de(users.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilizador inexistente.")));
    }
    @PatchMapping("/users/{id}")
    public Contratos.Publico alterar(@PathVariable("id") UUID id, @RequestBody Contratos.Alterar pedido) {
        return Contratos.Publico.de(service.alterar(id, pedido));
    }
    @PostMapping("/auth/login")
    public ResponseEntity<Contratos.Token> login(@Valid @RequestBody Contratos.Login pedido) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(auth.login(pedido));
    }
    @GetMapping("/auth/me")
    public ResponseEntity<Contratos.Publico> me(Authentication principal) {
        var u = users.findById(UUID.fromString(principal.getName())).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas."));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Contratos.Publico.de(u));
    }
}
