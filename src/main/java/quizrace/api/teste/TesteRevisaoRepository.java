package quizrace.api.teste;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TesteRevisaoRepository extends JpaRepository<TesteRevisao, UUID> {
    Page<TesteRevisao> findByTesteId(UUID testeId, Pageable pagina);
    Optional<TesteRevisao> findByTesteIdAndVersao(UUID testeId, long versao);
}
