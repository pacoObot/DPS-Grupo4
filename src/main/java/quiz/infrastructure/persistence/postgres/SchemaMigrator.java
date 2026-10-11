package quiz.infrastructure.persistence.postgres;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quiz.infrastructure.Config;

/**
 * Migrações Flyway — aplica V1..V8 na base de dados.
 * Executada no arranque do servidor (e nos testes de integração).
 */
public final class SchemaMigrator {

    private static final Logger log = LoggerFactory.getLogger(SchemaMigrator.class);

    private SchemaMigrator() {}

    /** Migra a base de dados principal. */
    public static void migrate(Config cfg) {
        migrate(cfg.dbUrl(), cfg.dbUser(), cfg.dbPassword());
    }

    /** Migra uma base de dados específica (usado pelos testes). */
    public static void migrate(String url, String user, String password) {
        Flyway flyway = Flyway.configure()
                .dataSource(url, user, password)
                .locations("classpath:db/migration")
                .load();
        var result = flyway.migrate();
        log.info("Flyway: {} migrações aplicadas em {}", result.migrationsExecuted, url);
    }
}
