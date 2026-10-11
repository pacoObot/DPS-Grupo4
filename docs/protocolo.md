# Protocolo de comunicação

Protocolo **de texto**, uma mensagem por linha (`\n`), campos separados por `|`.
Implementado em `shared/protocol/Protocol.java`; o parsing é idêntico nos dois lados,
mas **o servidor nunca confia no cliente**: valida o tipo, o número de campos, os
tamanhos e as faixas numéricas. Mensagem inválida ⇢ `ERROR|mensagem`.

- Limite de linha: `MAX_MESSAGE_LENGTH = 8192` bytes.
- Máximo de campos: `MAX_FIELDS = 40`.
- O tipo tem de casar `[A-Za-z_]+`.

## Mensagens Cliente → Servidor

### Admin
| Mensagem | Descrição |
|---|---|
| `ADMIN_LOGIN\|user\|pass` | autenticar administrador |
| `CREATE_QUESTION\|text\|category\|difficulty\|A\|B\|C\|D\|correct\|points\|timeLimit\|active` | criar pergunta |
| `UPDATE_QUESTION\|id\|…mesmos campos…` | atualizar pergunta |
| `DELETE_QUESTION\|id` | apagar pergunta |
| `LIST_QUESTIONS\|search\|difficulty\|activeFilter` | listar/filtrar |
| `SET_QUESTION_ACTIVE\|id\|true` | ativar/desativar |
| `CREATE_GAME\|name\|q1,q2,q3\|timePerQuestion` | criar partida |
| `OPEN_GAME\|gameId` | abrir inscrições (gera código) |
| `START_GAME\|gameId` | iniciar |
| `PAUSE_GAME\|gameId` / `RESUME_GAME\|gameId` | pausar/retomar |
| `STOP_GAME\|gameId` | encerrar |
| `GET_TEAMS\|gameId` | equipas + estado |
| `GET_SCOREBOARD\|gameId` | classificação |
| `GET_MONITOR` | estado das threads (demonstração) |
| `GET_STATS` | métricas agregadas para o dashboard (servidor, perguntas, partida, top 3) |

### Jogador
| Mensagem | Descrição |
|---|---|
| `JOIN_GAME\|codigo\|NomeEquipa` | entrar na partida |
| `GET_QUESTION` | pedir a pergunta atual |
| `ANSWER\|questionId\|B` | responder — **nunca envia pontuação** |
| `LEAVE_GAME` | sair |
| `PING` | keep-alive |

## Mensagens Servidor → Cliente

| Mensagem | Significado |
|---|---|
| `LOGIN_OK\|admin` / `LOGIN_ERROR\|msg` | resultado do login |
| `QUESTION_CREATED\|id` · `QUESTION_UPDATED\|id` · `QUESTION_DELETED\|id` | confirmações CRUD |
| `QUESTION_ROW\|id\|text\|category\|difficulty\|A\|B\|C\|D\|correct\|points\|time\|active` | linha de listagem |
| `LIST_END\|n` | fim da listagem |
| `GAME_CREATED\|gameId\|code` · `GAME_STATUS\|gameId\|status\|round\|total` | estado da partida |
| `GAME_STARTED\|id` · `GAME_PAUSED\|id` · `GAME_RESUMED\|id` · `GAME_STOPPED\|id` | transições |
| `JOIN_OK\|teamId\|gameId\|gameName` / `JOIN_ERROR\|msg` | resultado da entrada |
| `WAITING\|nOnline\|nTotal` | sala de espera |
| `QUESTION\|round\|total\|questionId\|timeLimit\|text\|A\|B\|C\|D` | nova pergunta |
| `ANSWER_RESULT\|questionId\|CORRECT\|points\|bonus\|total` | resposta certa |
| `ANSWER_RESULT\|questionId\|WRONG\|0\|0\|total` | errada |
| `ANSWER_RESULT\|questionId\|TIMEOUT\|0\|0\|total` | fora de tempo |
| `ANSWER_RESULT\|questionId\|DUPLICATE\|0\|0\|total` | já respondida |
| `ANSWER_RESULT\|questionId\|LATE\|0\|0\|total` | chegou depois do fim da ronda |
| `NEXT_ROUND\|round` | início de ronda |
| `SCOREBOARD\|pos:team:pts:ok:err:status\|…` | classificação ao vivo |
| `TEAMS\|name:pts:status:thread\|…` | equipas e thread que as processa |
| `MONITOR\|thread:team:state\|…` | monitor de concorrência |
| `STATS\|port:…\|pool:…\|threads:…\|conns:…\|qTotal:…\|qActive:…\|cats:…\|gameId:…\|status:…\|name:…\|round:…\|rounds:…\|teams:…\|online:…\|answers:…\|timeouts:…\|top:pos,equipa,pts;…` | métricas do dashboard (dados reais; `gameId:-1` se não há partida) |
| `GAME_OVER\|fim` | fim do jogo |
| `ERROR\|mensagem` | erro genérico |

## Exemplo de sessão (jogador)

```
> JOIN_GAME|7K3F|Equipa Azul
< JOIN_OK|12|3|Quiz de Cultura
< WAITING|4|10
< QUESTION|1|3|42|10|Capital de Portugal?|Lisboa|Porto|Braga|Faro
> ANSWER|42|A
< ANSWER_RESULT|42|CORRECT|15|5|20
< NEXT_ROUND|2
...
< GAME_OVER|fim
```

## Porque é que a pontuação nunca vem do cliente

A spec proíbe que o cliente envie pontos. O cliente envia **apenas** `questionId` e a
**opção** (`A`–`D`). Quem calcula a pontuação é o `ScoreService` no servidor, com base
no tempo restante. Isto elimina a fraude mais óbvia e mantém o servidor como autoridade.

## Depuração rápida

```bash
nc 127.0.0.1 5050
JOIN_GAME|CODIGO|EquipaTeste
GET_QUESTION
```

O `scripts/admin_cli.py` usa exatamente este protocolo (Python) — serve de cliente de
referência e de teste de integração manual.
