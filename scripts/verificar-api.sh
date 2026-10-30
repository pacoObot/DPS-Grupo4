#!/usr/bin/env bash
# Ensaio isolado: cria e remove apenas os containers/volume deste projecto temporário.
set -euo pipefail
cd "$(dirname "$0")/.."
export QUIZ_DB_PASSWORD="$(openssl rand -hex 24)"
export QUIZ_JWT_SECRET="$(openssl rand -hex 32)"
export QUIZ_ADMIN_EMAIL="admin@example.org"
export QUIZ_ADMIN_PASSWORD="$(openssl rand -hex 16)"
export QUIZ_API_PORT="${QUIZ_TEST_PORT:-19087}"
project="quizrace-check-$$"
compose=(docker compose -p "$project" -f compose.api.yml)
cleanup() {
  result=$?
  if [ "$result" -ne 0 ]; then "${compose[@]}" logs --tail 80 api; fi
  "${compose[@]}" down -v --remove-orphans
}
trap cleanup EXIT
"${compose[@]}" up -d --build
base="http://127.0.0.1:$QUIZ_API_PORT/api/v1"
esperar() {
  for tentativa in {1..90}; do
    if [ "$(curl -s -o /dev/null -w '%{http_code}' "$base/auth/me" || true)" = 401 ]; then return; fi
    sleep 1
  done
  "${compose[@]}" logs api
  return 1
}
login() {
  # O servidor HTTP pode aceitar pedidos antes de o ApplicationRunner concluir o bootstrap.
  for tentativa in {1..30}; do
    if token_login="$(jq -nc --arg email "$QUIZ_ADMIN_EMAIL" --arg password "$QUIZ_ADMIN_PASSWORD" \
      '{email:$email,palavraPasse:$password}' |
      curl -fsS "$base/auth/login" -H 'Content-Type: application/json' --data-binary @- 2>/dev/null |
      jq -er '.accessToken')"; then
      printf '%s' "$token_login"
      return
    fi
    sleep 1
  done
  return 1
}
esperar
token="$(login)"
resposta="$(jq -nc --arg password "$QUIZ_ADMIN_PASSWORD" \
  '{nome:"Estudante de teste",email:"student@example.org",palavraPasse:$password,perfil:"ESTUDANTE"}' |
  curl -fsS "$base/users" -H "Authorization: Bearer $token" -H 'Content-Type: application/json' --data-binary @-)"
id="$(jq -er '.id' <<< "$resposta")"
test "$(jq -r '.perfil' <<< "$resposta")" = ESTUDANTE
test "$(curl -s -o /dev/null -w '%{http_code}' "$base/users")" = 401
contagem="$("${compose[@]}" exec -T db psql -U quizrace -d quizrace -Atc 'SELECT count(*) FROM utilizador')"
test "$contagem" = 2
printf 'PASS: API HTTP, login, autorização e gravação em PostgreSQL.\n'
"${compose[@]}" restart db
# O healthcheck confirma a recuperação do PostgreSQL antes de reiniciar a API.
for tentativa in {1..60}; do
  if "${compose[@]}" exec -T db pg_isready -U quizrace -d quizrace >/dev/null 2>&1; then break; fi
  sleep 1
done
"${compose[@]}" restart api
esperar
token="$(login)"
curl -fsS "$base/users/$id" -H "Authorization: Bearer $token" | jq -e '.email == "student@example.org"' >/dev/null
test "$("${compose[@]}" exec -T db psql -U quizrace -d quizrace -Atc 'SELECT count(*) FROM utilizador')" = 2
printf 'PASS: dados e login preservados após reinício do PostgreSQL e da API; bootstrap não duplicou contas.\n'
