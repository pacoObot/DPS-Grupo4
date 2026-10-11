#!/usr/bin/env bash
#
# run-player.sh — arranca a interface gráfica do JOGADOR (JavaFX).
# Pode abrir várias instâncias (uma por equipa).
#
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/env.sh"

cd "$ROOT"
echo "== Interface do Jogador (JavaFX) =="
exec "$MVN" -o -q javafx:run -Djavafx.mainClass=quiz.interfaces.player.PlayerApplication
