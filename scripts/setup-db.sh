#!/usr/bin/env bash
#
# setup-db.sh — prepara o PostgreSQL LOCAL (sem Docker) para o projeto.
#
#   - cria o role  quiz (password quiz)
#   - cria as bases quiz_parallel e quiz_parallel_test
#
# Requer acesso administrativo ao PostgreSQL (sudo para o utilizador postgres,
# ou PGPASSWORD do superutilizador). É idempotente: pode correr várias vezes.
#
set -euo pipefail

DB_USER="${DB_USER:-quiz}"
DB_PASSWORD="${DB_PASSWORD:-quiz}"
DB_NAME="${DB_NAME:-quiz_parallel}"
DB_TEST_NAME="${DB_TEST_NAME:-quiz_parallel_test}"

echo "== Quiz: preparação do PostgreSQL local =="

# Como executar comandos como o superutilizador postgres?
if [ -n "${PGSUPER:-}" ]; then
    run_sql() { PGPASSWORD="$PGSUPER" psql -h "${DB_HOST:-127.0.0.1}" -p "${DB_PORT:-5432}" -U postgres -tAc "$1"; }
elif sudo -n -u postgres true 2>/dev/null; then
    run_sql() { sudo -u postgres psql -tAc "$1"; }
else
    echo "Sem sudo não-interativo. Será pedida a password do superutilizador postgres."
    echo "Dica: export PGPASSWORD=... e defina PGSUPER=1 (ou execute como root/postgres)."
    run_sql() { PGPASSWORD="${PGPASSWORD_SUPER:?defina PGPASSWORD_SUPER}" psql -h "${DB_HOST:-127.0.0.1}" -p "${DB_PORT:-5432}" -U postgres -tAc "$1"; }
fi

role_exists="$(run_sql "SELECT 1 FROM pg_roles WHERE rolname='${DB_USER}'" || true)"
if [ "$role_exists" != "1" ]; then
    run_sql "CREATE ROLE ${DB_USER} LOGIN PASSWORD '${DB_PASSWORD}';"
    echo "  + role ${DB_USER} criado"
else
    echo "  = role ${DB_USER} já existe"
fi

for db in "$DB_NAME" "$DB_TEST_NAME"; do
    exists="$(run_sql "SELECT 1 FROM pg_database WHERE datname='${db}'" || true)"
    if [ "$exists" != "1" ]; then
        run_sql "CREATE DATABASE ${db} OWNER ${DB_USER};"
        echo "  + base ${db} criada"
    else
        echo "  = base ${db} já existe"
    fi
done

echo
echo "Concluído. Teste a ligação com:"
echo "  PGPASSWORD=${DB_PASSWORD} psql -h ${DB_HOST:-127.0.0.1} -p ${DB_PORT:-5432} -U ${DB_USER} -d ${DB_NAME} -c 'SELECT 1'"
echo
echo "As migrações (Flyway) são aplicadas automaticamente no arranque do servidor."
