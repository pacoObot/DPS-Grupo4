package quiz.infrastructure;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

/**
 * Configuração do sistema.
 * ORDEM DE LEITURA: overrides (testes) -> variável de ambiente (produção) ->
 * db.properties (desenvolvimento local). Nunca hardcode em código de negócio.
 *
 *   DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD, QUIZ_PORT, QUIZ_THREADS
 */
public final class Config {

    private final Properties props = new Properties();
    private final Properties overrides = new Properties();

    private Config() {
        try (InputStream in = Config.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) props.load(in);
        } catch (IOException ignored) {
            // sem ficheiro: só variáveis de ambiente
        }
    }

    public static Config load() {
        return new Config();
    }

    /**
     * Config para testes/variantes: permite injetar valores que têm prioridade
     * sobre variáveis de ambiente e db.properties (ex.: URL do Testcontainers).
     */
    public static Config load(Map<String, String> overrides) {
        Config c = new Config();
        if (overrides != null) c.overrides.putAll(overrides);
        return c;
    }

    public String get(String key, String def) {
        String o = overrides.getProperty(key);
        if (o != null) return o;
        String env = System.getenv(key);
        if (env != null && !env.isBlank()) return env;
        return props.getProperty(key, def);
    }

    public int getInt(String key, int def) {
        try {
            return Integer.parseInt(get(key, String.valueOf(def)));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    // ---- banco de dados --------------------------------------------------
    public String dbHost()      { return get("DB_HOST", "127.0.0.1"); }
    public int    dbPort()      { return getInt("DB_PORT", 5437); }
    public String dbName()      { return get("DB_NAME", "quiz_parallel"); }
    public String dbUser()      { return get("DB_USER", "quiz"); }
    public String dbPassword()  { return get("DB_PASSWORD", "quiz"); }
    public String dbUrl() {
        return get("DB_URL", "jdbc:postgresql://" + dbHost() + ":" + dbPort() + "/" + dbName());
    }

    /** Base de dados usada pelos testes de integração. */
    public String testDbName() { return get("DB_TEST_NAME", "quiz_parallel_test"); }
    public String testDbUrl() {
        return "jdbc:postgresql://" + dbHost() + ":" + dbPort() + "/" + testDbName();
    }

    // ---- servidor TCP ----------------------------------------------------
    public int serverPort()   { return getInt("QUIZ_PORT", 5050); }
    public int threadPoolSize(){ return getInt("QUIZ_THREADS", 50); }

    /** Tamanho máximo do pool de conexões JDBC. */
    public int dbPoolSize()   { return getInt("DB_POOL_SIZE", 10); }
}