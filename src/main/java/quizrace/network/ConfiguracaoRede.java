package quizrace.network;

import java.util.Map;

public final class ConfiguracaoRede {
    public static final String HOST = "localhost";
    public static final int PORTA = 5000;
    public static final int ESPERA_INICIAL_MS = 60_000;
    public static final int LIGACAO_MS = 5_000;
    public static final int MAX_MENSAGEM = 2048;
    private ConfiguracaoRede() { }

    public static String host(String argumento) { return host(argumento, System.getenv()); }
    public static String host(String argumento, Map<String, String> ambiente) {
        return argumento != null && !argumento.isBlank() ? argumento : ambiente.getOrDefault("QUIZ_HOST", HOST);
    }
    public static int porta(String argumento) { return porta(argumento, System.getenv()); }
    public static int porta(String argumento, Map<String, String> ambiente) {
        String valor = argumento != null ? argumento : ambiente.getOrDefault("QUIZ_PORT", String.valueOf(PORTA));
        try {
            int porta = Integer.parseInt(valor);
            if (porta < 1 || porta > 65535) throw new NumberFormatException();
            return porta;
        } catch (NumberFormatException e) { throw new IllegalArgumentException("Porta deve estar entre 1 e 65535."); }
    }
}
