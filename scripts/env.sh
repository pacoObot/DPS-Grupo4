#!/usr/bin/env bash
#
# env.sh — configuração comum a todos os scripts (não executar diretamente).
# Descobre o Maven local e a raiz do projeto.
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Maven: usa o do PATH, senão o instalado localmente, senão o wrapper.
if command -v mvn >/dev/null 2>&1; then
    MVN="$(command -v mvn)"
elif [ -x "$HOME/tools/apache-maven-3.9.9/bin/mvn" ]; then
    MVN="$HOME/tools/apache-maven-3.9.9/bin/mvn"
elif [ -x "$ROOT/mvnw" ]; then
    MVN="$ROOT/mvnw"
else
    echo "ERRO: Maven não encontrado. Instale o Maven ou defina MVN=..." >&2
    exit 1
fi

JAR_SERVER="$ROOT/target/quiz-paralelo-1.0.0-server.jar"
JAR_BOT="$ROOT/target/quiz-paralelo-1.0.0-bot.jar"

DB_NAME="${DB_NAME:-quiz_parallel}"
DB_USER="${DB_USER:-quiz}"
DB_PASSWORD="${DB_PASSWORD:-quiz}"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-5432}"
QUIZ_PORT="${QUIZ_PORT:-5050}"

export DB_NAME DB_USER DB_PASSWORD DB_HOST DB_PORT QUIZ_PORT
