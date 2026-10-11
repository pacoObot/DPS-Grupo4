#!/usr/bin/env bash
#
# run-admin.sh — arranca a interface gráfica do ADMINISTRADOR (JavaFX).
# Pré-requisito: o servidor deve estar a correr (scripts/run-server.sh).
#
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/env.sh"

cd "$ROOT"
echo "== Interface do Administrador (JavaFX) =="
exec "$MVN" -o -q javafx:run -Djavafx.mainClass=quiz.interfaces.admin.AdminApplication
