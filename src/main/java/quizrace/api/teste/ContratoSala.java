package quizrace.api.teste;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ContratoSala {
    private ContratoSala() {}
    public enum Estado { PENDENTE, APROVADO, RECUSADO, FECHADO }
    public enum Dispositivo { COMPUTADOR, TELEMOVEL }
    public record Criar(@NotNull UUID testeId, @NotBlank @Size(max=100) String nome,
                        @Min(10) @Max(300) int segundosPorPergunta) {}
    public record Sala(String codigo, String nome, String tituloTeste, int totalPerguntas,
                       int segundosPorPergunta, boolean aberta, Instant criadaEm, Instant fechadaEm) {}
    public record Entrada(@NotBlank @Size(min=2,max=100) String nome,
                          @NotBlank @Size(min=2,max=40) String identificacao, @NotNull Dispositivo dispositivo) {}
    public record Participante(String id, String nome, String identificacao, Estado estado,
        int respondidas, int totalPerguntas, int correctas, double notaPercentual, Dispositivo dispositivo, boolean ligado,
        Integer cursorX, Integer cursorY, Instant entrouEm, Instant vistoEm) {}
    public record EstadoParticipante(Estado estado, String codigoSala, String tituloTeste,
        int respondidas, int totalPerguntas, PerguntaEstudante pergunta, boolean terminado, Double nota) {}
    public record PerguntaEstudante(int numero, String enunciado, String categoria,
        ContratosTeste.TipoQuestao tipo, List<Opcao> opcoes, int segundosPorPergunta, long segundosRestantes) {}
    public record Opcao(String codigo, String texto) {}
    public record Responder(@Min(1) int numeroPergunta, ContratosTeste.Alternativa resposta) {}
    public record Recebida(boolean registada, String mensagem) {}
    public record Aprovar(@NotNull Boolean aprovada) {}
    public record Cursor(@Min(0) @Max(1000) int x,@Min(0) @Max(1000) int y) {}
}
