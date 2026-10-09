package quizrace.network;

import quizrace.model.Resposta;
import java.io.*;
import java.util.*;

public final class ProtocoloMensagem {
    public enum Tipo { NOME, MENSAGEM, PERGUNTA, OPCAO, RESPOSTA, RESULTADO, ERRO, FIM,
        ENTROU, JOGADORES, AGUARDAR, INICIO, TEMPO, PONTUACAO, RANKING, VENCEDOR }

    public record Mensagem(Tipo tipo, List<String> campos) {
        public Mensagem { campos = List.copyOf(campos); }
        public String campo(int indice) { return campos.get(indice); }
    }
    private ProtocoloMensagem() { }

    public static String construir(Tipo tipo, String... campos) {
        Objects.requireNonNull(tipo, "Tipo obrigatório.");
        for (String campo : campos) {
            if (campo == null || campo.isBlank() || campo.contains("|") || campo.chars().anyMatch(Character::isISOControl))
                throw new IllegalArgumentException("Campo vazio ou com separadores proibidos.");
        }
        String linha = tipo.name() + "|" + String.join("|", campos);
        interpretar(linha);
        return linha;
    }

    public static Mensagem interpretar(String linha) {
        if (linha == null || linha.length() > ConfiguracaoRede.MAX_MENSAGEM
                || linha.chars().anyMatch(Character::isISOControl)) throw new IllegalArgumentException("Mensagem inválida ou demasiado longa.");
        String[] partes = linha.split("\\|", -1);
        Tipo tipo;
        try { tipo = Tipo.valueOf(partes[0]); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("Tipo de mensagem desconhecido."); }
        List<String> campos = Arrays.asList(partes).subList(1, partes.length);
        int n = campos.size();
        boolean quantidadeValida = switch (tipo) {
            case OPCAO, RESULTADO, JOGADORES, VENCEDOR -> n == 2;
            case PERGUNTA, RESPOSTA -> n == 1 || n == 2;
            case RANKING -> n == 3 || n == 7;
            default -> n == 1;
        };
        if (!quantidadeValida || campos.stream().anyMatch(String::isBlank)) throw new IllegalArgumentException("Número de campos inválido ou campo vazio.");
        if (tipo == Tipo.NOME && campos.get(0).trim().length() > 40) throw new IllegalArgumentException("Nome demasiado longo.");
        if (tipo == Tipo.RESPOSTA || tipo == Tipo.OPCAO) {
            String alternativa = campos.get(tipo == Tipo.RESPOSTA ? n - 1 : 0);
            if (!alternativa.matches("(?i)[ABCD]")) throw new IllegalArgumentException("Alternativa deve ser A, B, C ou D.");
        }
        if ((tipo == Tipo.PERGUNTA || tipo == Tipo.RESPOSTA) && n == 2) inteiro(campos.get(0), 1, Integer.MAX_VALUE);
        switch (tipo) {
            case JOGADORES -> { inteiro(campos.get(0), 0, 4); inteiro(campos.get(1), 2, 4); }
            case TEMPO -> inteiro(campos.get(0), 1, 15);
            case INICIO -> { if (!campos.get(0).equals("GO")) inteiro(campos.get(0), 1, 3); }
            case RESULTADO -> {
                if (!Set.of("CORRECTO", "ERRADO", "TIMEOUT", "TEMPO_ESGOTADO").contains(campos.get(0)))
                    throw new IllegalArgumentException("Resultado desconhecido.");
                decimal(campos.get(1), 100);
            }
            case PONTUACAO -> decimal(campos.get(0), Double.MAX_VALUE);
            case VENCEDOR -> decimal(campos.get(1), Double.MAX_VALUE);
            case RANKING -> {
                inteiro(campos.get(0), 1, 4); decimal(campos.get(2), Double.MAX_VALUE);
                if (n == 7) {
                    for (int i = 3; i < 6; i++) inteiro(campos.get(i), 0, Integer.MAX_VALUE);
                    decimal(campos.get(6), Double.MAX_VALUE);
                }
            }
            default -> { }
        }
        return new Mensagem(tipo, campos);
    }

    private static void inteiro(String valor, int minimo, int maximo) {
        try { int numero = Integer.parseInt(valor); if (numero < minimo || numero > maximo) throw new NumberFormatException(); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Número inteiro inválido."); }
    }
    private static void decimal(String valor, double maximo) {
        try { double numero = Double.parseDouble(valor); if (!Double.isFinite(numero) || numero < 0 || numero > maximo) throw new NumberFormatException(); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Número decimal inválido."); }
    }

    public static Resposta paraResposta(Mensagem mensagem, double tempoSegundos) {
        if (mensagem.tipo() != Tipo.RESPOSTA) throw new IllegalArgumentException("Esperada uma RESPOSTA.");
        return new Resposta(mensagem.campo(mensagem.campos().size() - 1), tempoSegundos,
                tempoSegundos >= quizrace.model.Pergunta.LIMITE_SEGUNDOS);
    }

    // Limita a memória antes de construir a linha; readLine() sozinho não impõe limite.
    public static String lerLinha(Reader entrada) throws IOException {
        StringBuilder linha = new StringBuilder();
        int c;
        while ((c = entrada.read()) != -1) {
            if (c == '\n') {
                if (!linha.isEmpty() && linha.charAt(linha.length() - 1) == '\r') linha.setLength(linha.length() - 1);
                return linha.toString();
            }
            if (linha.length() >= ConfiguracaoRede.MAX_MENSAGEM) throw new IOException("Mensagem demasiado longa.");
            linha.append((char) c);
        }
        if (!linha.isEmpty()) throw new EOFException("Mensagem incompleta.");
        return null;
    }
}
