# Concorrência e Paralelismo

Este é o núcleo conceptual do projeto. O objetivo é mostrar, de forma **visível e
verificável**, como o servidor trata N equipas em simultâneo sem perder respostas
nem corromper a pontuação.

## 1. Paralelismo na rede — pool de threads

`infrastructure/concurrency/ClientThreadPool` cria um `Executors.newFixedThreadPool`
de tamanho `QUIZ_THREADS` (default **50**). **Não** se cria `new Thread()` por cliente.

```
accept() ──▶ pool.submit(new ClientHandler(socket))
                    │
                    ├── thread "quiz-pool-1-1"  → Equipa Alpha   (readLine bloqueante)
                    ├── thread "quiz-pool-1-2"  → Equipa Beta
                    └── thread "quiz-pool-1-3"  → Equipa Gamma
```

Como cada `ClientHandler` fica preso ao seu socket (leitura bloqueante), o pool precisa
de uma thread por cliente **conectado**; acima do limite o servidor recusa clientes de
forma controlada. O nome das threads aparece no log (`[quiz-pool-1-3]`) — **prova visual
de paralelismo** durante a apresentação.

## 2. Temporização das rondas — `GameScheduler`

Uma thread dedicada (`game-scheduler`) é a **autoridade do tempo**. Por ronda:

1. grava `round_started_at` e calcula `deadline = round_started_at + time_per_question`;
2. faz broadcast de `QUESTION` a todas as equipas online;
3. espera pelo fim da ronda;
4. calcula pontuações/timeouts, persiste e faz broadcast de `SCOREBOARD`;
5. avança de ronda ou termina com `GAME_OVER`.

O cronómetro do cliente é **apenas visual** — quem decide se uma resposta é válida é
o `deadline` do servidor (comparado no `SubmitAnswerUseCase`).

## 3. "Todos responderam?" — `RoundCoordinator`

| Mecanismo | Para quê |
|---|---|
| `CountDownLatch` | conta as equipas online; cada resposta faz `countDown()` |
| `deadlineNanos` (`System.nanoTime`) | o servidor decide TIMEOUT |
| `Set` concorrente `pending` | evita sinalização dupla; identifica quem falhou o tempo |
| `freeze()` / `unfreeze()` | a pausa congela o deadline; ao retomar, desloca-o |

A ronda fecha quando **todas responderam** *ou* quando o **deadline expira**. O
`GameScheduler` consulta `allAnswered()`/`isExpired()` num ciclo curto (200 ms) para
poder reagir a `PAUSE` a meio da ronda.

## 4. Integridade da pontuação — o problema central

Se 40 equipas responderem no mesmo milissegundo, a sequência
`ler score → somar → gravar score` em código "normal" perde atualizações (*lost update*).

A solução tem **três camadas de defesa**:

- **Transação por resposta** (`JdbcUnitOfWork`): tudo ou nada.
- **`SELECT … FOR UPDATE`** para travar as linhas certas e serializar escritas:
  - `game` **primeiro**, depois `team` — **ordem de locks consistente** com o
    `GameScheduler` (game → teams), evitando **deadlock**.
- **`UNIQUE(team_id, question_id)`** na tabela `answers`: guarda final contra
  submissão dupla, mesmo com uma corrida a passar pelas validações.

```
Thread Alpha            Thread Beta
  │ BEGIN                 │ BEGIN
  │ lock game (FOR UPDATE)│      (espera pelo lock de game)
  │ lock team Alpha       │
  │ score += 15 ; commit  │ lock game (obtém)
                         │ lock team Beta
                         │ score += 10 ; commit   ← sem lost update
```

## 5. Estados e transições

`Game` e `Team` são máquinas de estado (`domain/entity`). Transições inválidas lançam
`BusinessException.Conflict` (ex.: responder numa partida `PAUSED`). Isto impede
corridas entre "iniciar/pausar/encerrar" e "responder".

## 6. Prova nos testes

`src/test/java/.../ConcurrencyIT.java` (integração, PostgreSQL real):

- **24 equipas respondem em simultâneo** ao mesmo tempo → verifica-se que o total de
  respostas persistidas é exatamente 24 e que **nenhum** ponto se perdeu.
- **Corrida de dupla submissão**: duas tentativas da mesma equipa/pergunta → apenas
  **uma** vence; a outra recebe `DUPLICATE`.

Correr:

```bash
mvn -o test -Dtest=ConcurrencyIT
```

## 7. Demonstração ao vivo

```bash
./scripts/demo.sh 50 3 10     # 50 bots, 3 perguntas, 10s
```

Enquanto corre, o terminal do servidor mostra as threads `quiz-pool-1-*` a processar
equipas diferentes em paralelo, e o monitor (`GET_MONITOR`) mostra a distribuição
thread ↔ equipa ↔ estado.
