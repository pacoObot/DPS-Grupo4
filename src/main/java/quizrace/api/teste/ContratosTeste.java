package quizrace.api.teste;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ContratosTeste {
    private ContratosTeste() {}
    public enum Alternativa { A, B, C, D }
    public enum TipoQuestao { MULTIPLA_ESCOLHA, VERDADEIRO_FALSO }
    public record Questao(
        @NotBlank @Size(max = 2000) String enunciado,
        @NotBlank @Size(max = 500) String alternativaA,
        @NotBlank @Size(max = 500) String alternativaB,
        @Size(max = 500) String alternativaC,
        @Size(max = 500) String alternativaD,
        @NotNull Alternativa correcta,
        @Size(max = 100) String categoria,
        @NotNull TipoQuestao tipo) {
        @JsonIgnore @AssertTrue public boolean opcoesValidas() {
            if (tipo == null || alternativaA == null || alternativaB == null || correcta == null) return false;
            if (tipo == TipoQuestao.VERDADEIRO_FALSO)
                return (correcta == Alternativa.A || correcta == Alternativa.B)
                    && alternativaA.strip().equalsIgnoreCase("Verdadeiro")
                    && alternativaB.strip().equalsIgnoreCase("Falso")
                    && (alternativaC == null || alternativaC.isBlank())
                    && (alternativaD == null || alternativaD.isBlank());
            return alternativaC != null && !alternativaC.isBlank()
                && alternativaD != null && !alternativaD.isBlank();
        }
    }
    public record Guardar(
        @NotBlank @Size(max = 150) String titulo,
        @Size(max = 2000) String descricao,
        @PositiveOrZero Long versao,
        @NotNull @Size(max = 50) List<@NotNull @Valid Questao> perguntas,
        Boolean disponivelPratica) {}
    public record Detalhe(UUID id, String titulo, String descricao, long versao, boolean disponivelPratica,
                          Instant criadoEm, Instant actualizadoEm, List<Questao> perguntas) {}
    public record Resumo(UUID id, String titulo, int totalPerguntas, long versao, boolean disponivelPratica, Instant actualizadoEm) {}
    public record Revisao(long versao, Instant guardadoEm) {}
    public record Pagina<T>(List<T> content, int page, int size, long totalElements) {}
}
