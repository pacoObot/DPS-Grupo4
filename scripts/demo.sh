#!/usr/bin/env bash
#
# demo.sh — demonstração automática ponta-a-ponta (sem JavaFX):
#   1. arranca o servidor
#   2. cria perguntas + partida via cliente admin
#   3. lança um enxame de bots que entram no jogo
#   4. inicia a partida (o scheduler corre as rondas)
#   5. espera pelo GAME_OVER e mostra a classificação
#
# Uso:  ./scripts/demo.sh [Nº_BOTS=20] [Nº_PERGUNTAS=3] [SEG_POR_PERGUNTA=10]
#
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/env.sh"

BOTS="${1:-20}"
QUESTIONS="${2:-3}"
QTIME="${3:-10}"
PORT="${QUIZ_PORT:-5050}"

cd "$ROOT"

if [ ! -f "$JAR_SERVER" ] || [ ! -f "$JAR_BOT" ]; then
    echo "== A compilar (server + bots) =="
    "$MVN" -o -q package -DskipTests
fi

LOG="$ROOT/logs/quiz-demo.log"
: > "$LOG"

echo "== 1/5  A arrancar o servidor (porta $PORT) =="
QUIZ_PORT="$PORT" java -jar "$JAR_SERVER" > "$LOG" 2>&1 &
SERVER_PID=$!
trap 'echo "== A encerrar servidor =="; kill $SERVER_PID 2>/dev/null || true' EXIT

# espera a porta abrir
for _ in $(seq 1 60); do
    if (exec 3<>"/dev/tcp/127.0.0.1/$PORT") 2>/dev/null; then
        exec 3>&- 2>/dev/null || true
        break
    fi
    sleep 0.5
done
sleep 1

echo "== 2/5  A criar perguntas e partida =="
INFO="$(python3 "$ROOT/scripts/admin_cli.py" create --port "$PORT" \
        --questions "$QUESTIONS" --time "$QTIME" --name Demo)"
echo "   $INFO"
GAME_ID="$(echo "$INFO" | sed -n 's/.*GAMEID=\([0-9]*\).*/\1/p')"
CODE="$(echo "$INFO" | sed -n 's/.*CODE=\([A-Z0-9]*\).*/\1/p')"

if [ -z "$CODE" ]; then
    echo "ERRO: não foi possível obter o código da partida." >&2
    tail -30 "$LOG" >&2
    exit 1
fi

echo "== 3/5  A lançar $BOTS bots (código $CODE) =="
java -jar "$JAR_BOT" --code "$CODE" --bots "$BOTS" --delay 3000 \
     --host 127.0.0.1 --port "$PORT" > "$ROOT/logs/quiz-demo-bots.log" 2>&1 &
BOTS_PID=$!
trap 'echo "== A encerrar =="; kill $BOTS_PID $SERVER_PID 2>/dev/null || true' EXIT

sleep 4   # dá tempo aos bots de entrarem

echo "== 4/5  A iniciar a partida =="
python3 "$ROOT/scripts/admin_cli.py" start --port "$PORT" --game "$GAME_ID"

echo "== 5/5  A aguardar o fim (Ctrl+C para interromper) =="
# espera o processo dos bots terminar (eles saem no GAME_OVER)
wait $BOTS_PID 2>/dev/null || true

echo
echo "== Classificação final (do log do servidor) =="
grep -A 40 "CLASSIFICAÇÃO FINAL" "$LOG" | tail -40 || true

echo
echo "Logs: $LOG  |  $ROOT/logs/quiz-demo-bots.log"
