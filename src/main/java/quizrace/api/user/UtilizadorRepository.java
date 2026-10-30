package quizrace.api.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UtilizadorRepository extends JpaRepository<Utilizador, UUID> {
    Optional<Utilizador> findByEmail(String email);
    long countByPerfilAndActivoTrue(Perfil perfil);
}
