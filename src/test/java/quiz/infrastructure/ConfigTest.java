package quiz.infrastructure;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Resolução de configuração: overrides > variável de ambiente > db.properties.
 * Testamos a camada de overrides (determinista, sem depender do ambiente).
 */
class ConfigTest {

    @Test
    void overridesTemPrioridadeSobreOProperties() {
        Config c = Config.load(Map.of(
                "DB_HOST", "db.exemplo.pt",
                "DB_PORT", "6000",
                "DB_NAME", "meuquiz",
                "DB_USER", "u",
                "DB_PASSWORD", "p"));

        assertEquals("db.exemplo.pt", c.dbHost());
        assertEquals(6000, c.dbPort());
        assertEquals("meuquiz", c.dbName());
        assertEquals("jdbc:postgresql://db.exemplo.pt:6000/meuquiz", c.dbUrl());
    }

    @Test
    void dbUrlExplicitoEhRespeitado() {
        Config c = Config.load(Map.of("DB_URL", "jdbc:postgresql://outro:9999/z"));
        assertEquals("jdbc:postgresql://outro:9999/z", c.dbUrl());
    }

    @Test
    void portaInvalidaUsaDefault() {
        Config c = Config.load(Map.of("QUIZ_PORT", "nao-e-numero"));
        assertEquals(5050, c.serverPort());
    }

    @Test
    void threadPoolConfiguravel() {
        Config c = Config.load(Map.of("QUIZ_THREADS", "120"));
        assertEquals(120, c.threadPoolSize());
    }

    @Test
    void testDbUrlUsaNomeDeTeste() {
        Config c = Config.load(Map.of(
                "DB_HOST", "127.0.0.1",
                "DB_PORT", "5432",
                "DB_TEST_NAME", "quiz_parallel_test"));
        assertEquals("jdbc:postgresql://127.0.0.1:5432/quiz_parallel_test", c.testDbUrl());
    }
}