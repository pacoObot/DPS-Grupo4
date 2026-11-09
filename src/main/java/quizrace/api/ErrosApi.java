package quizrace.api;

import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ErrosApi {
    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class,
        HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
        org.springframework.web.bind.MissingRequestHeaderException.class})
    ResponseEntity<?> invalidos(Exception e) { return erro(400, "DADOS_INVALIDOS", "Verifique os dados enviados."); }

    @ExceptionHandler(org.springframework.dao.OptimisticLockingFailureException.class)
    ResponseEntity<?> versaoDesactualizada(Exception e) {
        return erro(409, "VERSAO_DESACTUALIZADA", "Este teste foi alterado noutra janela. Consulte a versão guardada antes de voltar a editar.");
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> duplicado(DataIntegrityViolationException e) {
        return erro(409, "CONFLITO", "Já existe um utilizador com esse email ou os dados violam uma restrição.");
    }
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<?> regra(ResponseStatusException e) {
        return erro(e.getStatusCode().value(), "HTTP_" + e.getStatusCode().value(), e.getReason());
    }
    private ResponseEntity<?> erro(int status, String codigo, String mensagem) {
        return ResponseEntity.status(status).body(Map.of("codigo", codigo, "mensagem", mensagem));
    }
}
