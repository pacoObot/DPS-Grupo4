package quiz.support;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import quiz.infrastructure.Config;
import quiz.infrastructure.persistence.postgres.Jdbc;
import quiz.infrastructure.persistence.postgres.JdbcUnitOfWork;
import quiz.infrastructure.persistence.postgres.PostgreSQLDataSource;
import quiz.infrastructure.persistence.postgres.SchemaMigrator;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Base dos testes de INTEGRAÇÃO (*IT): fala com um PostgreSQL REAL local.
 *
 * - Se o PostgreSQL não estiver acessível, a classe inteira é SALTADA
 *   (assumption) — os testes unitários continuam a passar sem base de dados.
 * - Antes de cada teste limpa as tabelas e reinicia as sequências.
 *
 * Preparar a base:  scripts/setup-db.sh
 */
public abstract class PostgresIT {

    protected static Config cfg;
    protected static PostgreSQLDataSource ds;
    protected static Jdbc jdbc;

    @BeforeAll
    static void initDatabase() {
        cfg = TestDb.config();
        ds = new PostgreSQLDataSource(cfg);
        assumeTrue(ds.ping(),
                "PostgreSQL de teste inacessível em " + cfg.dbUrl()
                        + " — execute scripts/setup-db.sh (os testes *IT serão saltados).");
        SchemaMigrator.migrate(cfg);
        jdbc = new Jdbc(ds);
    }

    @BeforeEach
    void cleanDatabase() {
        jdbc.update("TRUNCATE answers, teams, game_questions, games, questions RESTART IDENTITY CASCADE");
    }

    protected static JdbcUnitOfWork newUnitOfWork() {
        return new JdbcUnitOfWork(ds);
    }
}