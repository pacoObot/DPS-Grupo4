package quizrace.api.teste;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalaRepository extends JpaRepository<SalaTeste,String> {
    List<SalaTeste> findByDocenteIdOrderByCriadaEmDesc(UUID docenteId);
    List<SalaTeste> findByTesteIdAndDocenteIdOrderByCriadaEmDesc(UUID testeId, UUID docenteId);
}
