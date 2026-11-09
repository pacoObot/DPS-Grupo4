package quizrace.api.teste;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/quizzes/pratica")
public class PraticaController {
 private final ServicePratica service;
 public PraticaController(ServicePratica service){this.service=service;}
 private UUID user(Authentication a){return UUID.fromString(a.getName());}
 @GetMapping public ResponseEntity<?> listar(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size){return ok(service.listar(page,size));}
 @GetMapping("/{id}") public ResponseEntity<?> obter(@PathVariable UUID id){return ok(service.obter(id));}
 @PostMapping("/{id}/respostas") public ResponseEntity<?> submeter(Authentication a,@PathVariable UUID id,@Valid @RequestBody ServicePratica.Submissao b){return ok(service.submeter(user(a),id,b));}
 @GetMapping("/historico") public ResponseEntity<?> historico(Authentication a,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size){return ok(service.historico(user(a),page,size));}
 private ResponseEntity<?> ok(Object o){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(o);}
}
