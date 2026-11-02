package quizrace.api.teste;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@Service
@Validated
@Transactional(readOnly = true)
public class TesteService {
    private final TesteRepository testes;
    private final TesteRevisaoRepository revisoes;
    private final ObjectMapper json;

    public TesteService(TesteRepository testes, TesteRevisaoRepository revisoes, ObjectMapper json) {
        this.testes = testes;
        this.revisoes = revisoes;
        this.json = json;
    }
    private Teste doDocente(UUID docente, UUID id) {
        // Consultar pelo dono impede que conhecer um UUID dê acesso ao teste ou ao gabarito.
        return testes.findByIdAndDocenteId(id, docente).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Teste inexistente."));
    }
    private PageRequest pagina(int page, int size, String campo) {
        if (page < 0 || size < 1 || size > 50)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paginação inválida.");
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, campo).and(Sort.by("id")));
    }
    public ContratosTeste.Pagina<ContratosTeste.Resumo> listar(UUID docente, int page, int size) {
        var p = testes.findByDocenteId(docente, pagina(page, size, "actualizadoEm")).map(Teste::resumo);
        return new ContratosTeste.Pagina<>(p.getContent(), page, size, p.getTotalElements());
    }
    public ContratosTeste.Detalhe obter(UUID docente, UUID id) { return doDocente(docente, id).detalhe(); }

    @Transactional
    public ContratosTeste.Detalhe criar(UUID docente, @Valid ContratosTeste.Guardar pedido) {
        if (pedido.versao() != null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Um teste novo não tem versão anterior.");
        return guardar(new Teste(docente, pedido));
    }
    @Transactional
    public ContratosTeste.Detalhe alterar(UUID docente, UUID id, @Valid ContratosTeste.Guardar pedido) {
        var teste = doDocente(docente, id);
        if (pedido.versao() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indique a versão que está a editar.");
        if (!pedido.versao().equals(teste.getVersao()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este teste foi alterado noutra janela. Consulte a versão guardada antes de voltar a editar.");
        teste.actualizar(pedido);
        return guardar(teste);
    }
    private ContratosTeste.Detalhe guardar(Teste teste) {
        // O flush confirma a versão optimista antes de guardar a cópia histórica na mesma transacção.
        var salvo = testes.saveAndFlush(teste);
        var detalhe = salvo.detalhe();
        try {
            revisoes.saveAndFlush(new TesteRevisao(salvo, json.writeValueAsString(detalhe)));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Não foi possível guardar o histórico do teste.", e);
        }
        return detalhe;
    }
    public ContratosTeste.Pagina<ContratosTeste.Revisao> historico(UUID docente, UUID id, int page, int size) {
        doDocente(docente, id);
        var p = revisoes.findByTesteId(id, pagina(page, size, "versao")).map(TesteRevisao::resumo);
        return new ContratosTeste.Pagina<>(p.getContent(), page, size, p.getTotalElements());
    }
    public ContratosTeste.Detalhe revisao(UUID docente, UUID id, long versao) {
        doDocente(docente, id);
        var r = revisoes.findByTesteIdAndVersao(id, versao).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Versão inexistente."));
        try { return json.readValue(r.getConteudo(), ContratosTeste.Detalhe.class); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Histórico inválido.", e); }
    }
}
