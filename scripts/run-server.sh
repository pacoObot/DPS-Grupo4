#!/usr/bin/env bash
#
# run-server.sh — arranca o servidor TCP (aplica migrações Flyway e cria o admin).
# Porta TCP padrão: 5050  |  base de dados: PostgreSQL local (quiz_parallel).
#
# Variáveis opcionais: QUIZ_PORT, QUIZ_THREADS, DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
#
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/env.sh"

cd "$ROOT"

if [ ! -f "$JAR_SERVER" ]; then
    echo "Jar do servidor em falta — a compilar..."
    "$MVN" -o -q package -DskipTests
fi

echo "== Servidor Quiz Paralelo =="
echo "   TCP  : ${QUIZ_PORT:-5050}   threads: ${QUIZ_THREADS:-50}"
echo "   BDs  : ${DB_USER}@${DB_HOST}:${DB_PORT}/${DB_NAME}"
echo "   admin: admin / admin"
echo

exec java -jar "$JAR_SERVER"
