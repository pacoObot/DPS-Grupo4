package quizrace.api.user;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public final class Contratos {
    private Contratos() {}
    public record Criar(
        @NotBlank @Size(min = 2, max = 100) String nome,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 12, max = 72) String palavraPasse,
        @NotNull Perfil perfil) {
        @Override public String toString() { return "Criar[dados protegidos]"; }
    }
    public record Login(@NotBlank @Email @Size(max = 254) String email,
                        @NotBlank @Size(max = 72) String palavraPasse) {
        @Override public String toString() { return "Login[dados protegidos]"; }
    }
    public record Alterar(Perfil perfil, Boolean activo) {}
    public record Publico(UUID id, String nome, String email, Perfil perfil, boolean activo, Instant criadoEm) {
        public static Publico de(Utilizador u) {
            return new Publico(u.getId(), u.getNome(), u.getEmail(), u.getPerfil(), u.isActivo(), u.getCriadoEm());
        }
    }
    public record Token(String accessToken, String tokenType, long expiresIn, Publico utilizador) {
        @Override public String toString() { return "Token[dados protegidos]"; }
    }
}
