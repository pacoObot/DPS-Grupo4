package quizrace.network;

import quizrace.model.Resposta;
import java.util.*;

public final class ProtocoloMensagem {
    public enum Tipo { NOME, MENSAGEM, PERGUNTA, OPCAO, RESPOSTA, RESULTADO, ERRO, FIM }

    public record Mensagem(Tipo tipo, List<String> campos) {
        public Mensagem { campos = List.copyOf(campos); }
        public String campo(int indice) { return campos.get(indice); }
    }

    private ProtocoloMensagem() { }

    public static String construir(Tipo tipo, String... campos) {
        Objects.requireNonNull(tipo, "Tipo obrigatório.");
        for (String campo : campos) {
            if (campo == null || campo.isBlank() || campo.contains("|")
                    || campo.contains("\n") || campo.contains("\r")) {
                throw new IllegalArgumentException("Campo vazio ou com separadores proibidos.");
            }
        }
        String linha = tipo.name() + "|" + String.join("|", campos);
        interpretar(linha);
        return linha;
    }

    public static Mensagem interpretar(String linha) {
        if (linha == null || linha.contains("\n") || linha.contains("\r")) {
            throw new IllegalArgumentException("Mensagem inválida.");
        }
        String[] partes = linha.split("\\|", -1);
        Tipo tipo;
        try { tipo = Tipo.valueOf(partes[0]); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("Tipo de mensagem desconhecido."); }
        int quantidade = tipo == Tipo.OPCAO || tipo == Tipo.RESULTADO ? 2 : 1;
        if (partes.length != quantidade + 1) throw new IllegalArgumentException("Número de campos inválido.");
        List<String> campos = Arrays.asList(partes).subList(1, partes.length);
        if (campos.stream().anyMatch(String::isBlank)) throw new IllegalArgumentException("Campo obrigatório vazio.");
        if ((tipo == Tipo.RESPOSTA || tipo == Tipo.OPCAO) && !campos.get(0).matches("(?i)[ABCD]")) {
            throw new IllegalArgumentException("Alternativa deve ser A, B, C ou D.");
        }
        if (tipo == Tipo.RESULTADO) {
            if (!Set.of("CORRECTO", "ERRADO", "TEMPO_ESGOTADO").contains(campos.get(0))) {
                throw new IllegalArgumentException("Resultado desconhecido.");
            }
            try {
                double pontos = Double.parseDouble(campos.get(1));
                if (!Double.isFinite(pontos) || pontos < 0) throw new NumberFormatException();
            } catch (NumberFormatException e) { throw new IllegalArgumentException("Pontuação inválida."); }
        }
        return new Mensagem(tipo, campos);
    }

    public static Resposta paraResposta(Mensagem mensagem, double tempoSegundos) {
        if (mensagem.tipo() != Tipo.RESPOSTA) throw new IllegalArgumentException("Esperada uma RESPOSTA.");
        return new Resposta(mensagem.campo(0), tempoSegundos,
                tempoSegundos >= quizrace.model.Pergunta.LIMITE_SEGUNDOS);
    }
}
