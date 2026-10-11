#!/usr/bin/env bash
#
# run-bots.sh — arranca um enxame de jogadores automáticos (stress/concorrência).
#
# Uso:
#   ./scripts/run-bots.sh <CODIGO_DA_PARTIDA> [Nº_BOTS] [OPÇÃO]
#
# Exemplos:
#   ./scripts/run-bots.sh ABC123            # 20 bots, respostas aleatórias
#   ./scripts/run-bots.sh ABC123 100        # 100 bots
#   ./scripts/run-bots.sh ABC123 60 A       # 60 bots, todos respondem (A)
#
# Antes: criar e ABRIR a partida na consola de admin, e copiar o código.
#
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/env.sh"

CODE="${1:-}"
BOTS="${2:-20}"
OPTION="${3:-auto}"

if [ -z "$CODE" ]; then
    echo "Uso: $0 <CODIGO_DA_PARTIDA> [Nº_BOTS=20] [OPÇÃO=auto|A|B|C|D]" >&2
    exit 2
fi

cd "$ROOT"
if [ ! -f "$JAR_BOT" ]; then
    echo "Jar dos bots em falta — a compilar..."
    "$MVN" -o -q package -DskipTests
fi

echo "== Enxame de ${BOTS} bots -> ${DB_HOST}:${QUIZ_PORT:-5050}  código=${CODE} =="
exec java -jar "$JAR_BOT" --code "$CODE" --bots "$BOTS" --option "$OPTION" \
    --host 127.0.0.1 --port "${QUIZ_PORT:-5050}"
