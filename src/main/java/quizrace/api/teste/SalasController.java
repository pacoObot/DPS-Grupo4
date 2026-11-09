package quizrace.api.teste;

import jakarta.validation.Valid;
import java.util.*;
import java.net.URI;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/salas")
public class SalasController {
    private final ServiceSalas service;
    public SalasController(ServiceSalas service){this.service=service;}
    private UUID docente(Authentication a){return UUID.fromString(a.getName());}
    @GetMapping public ResponseEntity<?> listar(Authentication a){return ok(service.listar(docente(a)));}
    @PostMapping public ResponseEntity<?> criar(Authentication a,@Valid @RequestBody ContratoSala.Criar pedido){var s=service.criar(docente(a),pedido);return ResponseEntity.created(URI.create("/api/v1/salas/"+s.codigo())).cacheControl(CacheControl.noStore()).body(s);}
    @GetMapping("/{codigo}") public ResponseEntity<?> obter(Authentication a,@PathVariable("codigo") String c){return ok(service.obter(docente(a),c));}
    @GetMapping("/{codigo}/participantes") public ResponseEntity<?> participantes(Authentication a,@PathVariable("codigo") String c){return ok(service.participantes(docente(a),c));}
    @PatchMapping("/{codigo}/participantes/{id}") public ResponseEntity<?> aprovar(Authentication a,@PathVariable("codigo") String c,@PathVariable("id") UUID id,@Valid @RequestBody ContratoSala.Aprovar b){return ok(service.aprovar(docente(a),c,id,b));}
    @PostMapping("/{codigo}/fechar") public ResponseEntity<?> fechar(Authentication a,@PathVariable("codigo") String c){return ok(service.fechar(docente(a),c));}
    @PostMapping("/entrada") public ResponseEntity<?> entrar(@RequestBody @Valid PedidoEntrada p){return ok(service.entrar(p.codigo(),p.entrada()));}
    @GetMapping("/participacoes/{id}") public ResponseEntity<?> estado(@PathVariable("id") UUID id,@RequestHeader("X-Quiz-Participante") String token){return ok(service.estado(id,token));}
    @PostMapping("/participacoes/{id}/cursor") public ResponseEntity<?> cursor(@PathVariable("id") UUID id,@RequestHeader("X-Quiz-Participante") String token,@RequestBody @Valid ContratoSala.Cursor p){service.cursor(id,token,p);return ok(Map.of("registado",true));}
    @PostMapping("/participacoes/{id}/respostas") public ResponseEntity<?> responder(@PathVariable("id") UUID id,@RequestHeader("X-Quiz-Participante") String token,@RequestBody @Valid ContratoSala.Responder r){return ok(service.responder(id,token,r));}
    public record PedidoEntrada(@jakarta.validation.constraints.NotBlank String codigo,@Valid @jakarta.validation.constraints.NotNull ContratoSala.Entrada entrada){}
    private ResponseEntity<?> ok(Object b){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(b);}
}
