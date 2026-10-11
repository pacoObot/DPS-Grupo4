#!/usr/bin/env bash
#
# build.sh — compila e empacota (jar do servidor + jar dos bots).
#   ./scripts/build.sh          -> build offline + testes unitários
#   ./scripts/build.sh --tests  -> corre também os testes de integração (*IT)
#
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/env.sh"

cd "$ROOT"

if [ "${1:-}" = "--tests" ]; then
    echo "== Build com testes (unitários + integração) =="
    "$MVN" -o clean verify
else
    echo "== Build rápido (offline, sem testes de integração) =="
    "$MVN" -o clean package -DskipTests
fi

echo
echo "Artefactos:"
ls -1 "$ROOT"/target/*.jar
