#!/usr/bin/env bash
#
# run-web.sh — arranca o gateway web (Spring Boot) que serve o cliente
# do jogador no browser (telemóvel/PC) e faz ponte para o servidor TCP.
#
#   Uso:   ./scripts/run-web.sh [PORTA_WEB]
#
# Requer: o servidor do quiz (quiz-server) a correr na porta 5050.
#
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WEB_DIR="$HERE/../quiz-paralelo-web"
[ -d "$WEB_DIR" ] || WEB_DIR="$HERE/web"

MVN="${MVN:-$HOME/tools/apache-maven-3.9.9/bin/mvn}"
WEB_PORT="${1:-8090}"

[ -d "$WEB_DIR" ] || { echo "Diretório do módulo web não encontrado (quiz-paralelo-web)"; exit 1; }

JAR="$WEB_DIR/target/quiz-paralelo-web-1.0.0.jar"
if [ ! -f "$JAR" ]; then
  echo "==> A compilar o módulo web (primeira vez pode descarregar dependências)..."
  ( cd "$WEB_DIR" && "$MVN" -q -DskipTests package )
fi

echo "==> Cliente web em:  http://0.0.0.0:${WEB_PORT}"
echo "==> No telemóvel (mesma Wi-Fi):  http://<IP-desta-máquina>:${WEB_PORT}"
( cd "$WEB_DIR" && exec java -jar "$JAR" --server.port="$WEB_PORT" )