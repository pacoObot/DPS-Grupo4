package quizrace.api.teste;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ParticipanteRepository extends JpaRepository<ParticipanteSala,UUID> {
    List<ParticipanteSala> findByCodigoSalaOrderByEntrouEmAsc(String codigo);
    Optional<ParticipanteSala> findByIdAndCodigoSala(UUID id,String codigo);
    boolean existsByCodigoSalaAndIdentificacaoChave(String codigo,String identificacao);
}
