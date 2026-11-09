package quizrace.api.teste;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface TentativaRepository extends JpaRepository<TentativaPratica,UUID>{
 Page<TentativaPratica> findByUtilizadorId(UUID id,Pageable p);
}
