package quizrace.api.teste;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ContratosTeste {
    private ContratosTeste() {}
    public enum Alternativa { A, B, C, D }
    public record Questao(
        @NotBlank @Size(max = 2000) String enunciado,
        @NotBlank @Size(max = 500) String alternativaA,
        @NotBlank @Size(max = 500) String alternativaB,
        @NotBlank @Size(max = 500) String alternativaC,
        @NotBlank @Size(max = 500) String alternativaD,
        @NotNull Alternativa correcta,
        @Size(max = 100) String categoria) {}
    public record Guardar(
        @NotBlank @Size(max = 150) String titulo,
        @Size(max = 2000) String descricao,
        @PositiveOrZero Long versao,
        @NotNull @Size(max = 50) List<@NotNull @Valid Questao> perguntas) {}
    public record Detalhe(UUID id, String titulo, String descricao, long versao,
                          Instant criadoEm, Instant actualizadoEm, List<Questao> perguntas) {}
    public record Resumo(UUID id, String titulo, int totalPerguntas, long versao, Instant actualizadoEm) {}
    public record Revisao(long versao, Instant guardadoEm) {}
    public record Pagina<T>(List<T> content, int page, int size, long totalElements) {}
}
