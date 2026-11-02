package quizrace.api.teste;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/testes")
public class TestesController {
    private final TesteService service;
    public TestesController(TesteService service) { this.service = service; }
    private UUID docente(Authentication auth) { return UUID.fromString(auth.getName()); }
    @GetMapping
    public ResponseEntity<?> listar(Authentication auth,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return resposta(service.listar(docente(auth), page, size));
    }
    @PostMapping
    public ResponseEntity<?> criar(Authentication auth, @Valid @RequestBody ContratosTeste.Guardar pedido) {
        var criado = service.criar(docente(auth), pedido);
        return ResponseEntity.created(URI.create("/api/v1/testes/" + criado.id()))
                .cacheControl(CacheControl.noStore()).body(criado);
    }
    @GetMapping("/{id}")
    public ResponseEntity<?> obter(Authentication auth, @PathVariable("id") UUID id) {
        return resposta(service.obter(docente(auth), id));
    }
    @PutMapping("/{id}")
    public ResponseEntity<?> alterar(Authentication auth, @PathVariable("id") UUID id,
                                     @Valid @RequestBody ContratosTeste.Guardar pedido) {
        return resposta(service.alterar(docente(auth), id, pedido));
    }
    @GetMapping("/{id}/historico")
    public ResponseEntity<?> historico(Authentication auth, @PathVariable("id") UUID id,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return resposta(service.historico(docente(auth), id, page, size));
    }
    @GetMapping("/{id}/historico/{versao}")
    public ResponseEntity<?> revisao(Authentication auth, @PathVariable("id") UUID id,
                                    @PathVariable("versao") long versao) {
        return resposta(service.revisao(docente(auth), id, versao));
    }
    private ResponseEntity<?> resposta(Object corpo) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(corpo);
    }
}
