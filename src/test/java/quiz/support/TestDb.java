package quiz.support;

import quiz.infrastructure.Config;

import java.util.HashMap;
import java.util.Map;

/**
 * TestDb — configuração da base de dados usada pelos testes de integração.
 *
 * Por omissão liga-se ao PostgreSQL LOCAL (sem Docker):
 *   jdbc:postgresql://127.0.0.1:5432/quiz_parallel_test   (user/pass: quiz/quiz)
 *
 * Pode ser sobrescrito por propriedades de sistema (-Dquiz.test.db.url=...) ou
 * variáveis de ambiente. Crie a base com: scripts/setup-db.sh.
 */
public final class TestDb {

    private TestDb() {}

    public static Config config() {
        Map<String, String> o = new HashMap<>();
        o.put("DB_URL",      pick("quiz.test.db.url",  "QUIZ_TEST_DB_URL",
                "jdbc:postgresql://127.0.0.1:5432/quiz_parallel_test"));
        o.put("DB_USER",     pick("quiz.test.db.user", "QUIZ_TEST_DB_USER", "quiz"));
        o.put("DB_PASSWORD", pick("quiz.test.db.pass", "QUIZ_TEST_DB_PASS", "quiz"));
        o.put("DB_POOL_SIZE", pick("quiz.test.db.pool", "QUIZ_TEST_DB_POOL", "40"));
        return Config.load(o);
    }

    private static String pick(String sysProp, String env, String def) {
        String v = System.getProperty(sysProp);
        if (v == null || v.isBlank()) v = System.getenv(env);
        return (v == null || v.isBlank()) ? def : v;
    }
}