package quizrace.api.teste;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TesteRepository extends JpaRepository<Teste, UUID> {
    Page<Teste> findByDocenteId(UUID docenteId, Pageable pagina);
    Optional<Teste> findByIdAndDocenteId(UUID id, UUID docenteId);
}
