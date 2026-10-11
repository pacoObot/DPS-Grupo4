# Arquitetura

## Visão geral

O projeto segue **Clean Architecture** (Ports & Adapters). As dependências apontam
para o centro; o domínio não conhece nenhuma tecnologia.

```
┌──────────────────────────────────────────────────────────────────┐
│  interfaces/            (adaptadores de ENTRADA: JavaFX, bot)      │
│    admin/  player/  common/                                        │
├──────────────────────────────────────────────────────────────────┤
│  infrastructure/network (adaptador de ENTRADA: TCP)                │
│    TcpServer, ClientHandler, ProtocolHandler, ClientRegistry       │
│                                                                    │
│  infrastructure/persistence (adaptador de SAÍDA: PostgreSQL/JDBC)  │
│    Jdbc, JdbcUnitOfWork, *RepositoryImpl, SchemaMigrator           │
│                                                                    │
│  infrastructure/concurrency  GameScheduler, RoundCoordinator,      │
│                              ClientThreadPool                      │
│  infrastructure/security     PBKDF2PasswordHasher                  │
├──────────────────────────────────────────────────────────────────┤
│  application/                                                      │
│    port/output  QuestionRepository, TeamRepository, GameRepository,│
│                 AnswerRepository, AdminRepository, UnitOfWork,     │
│                 PasswordHasher            (interfaces)             │
│    usecase      admin/  game/  player/    (casos de uso)           │
│    service      GameService, ScoreService, ConcurrencyService      │
│    exception    BusinessException                                  │
├──────────────────────────────────────────────────────────────────┤
│  domain/                                                           │
│    entity  Game, Team, Question, Answer, Score, Player, Admin...   │
│    enums   GameStatus, TeamStatus, Difficulty, UserRole            │
│    valueobject  GameId, TeamId, QuestionId, PlayerId               │
└──────────────────────────────────────────────────────────────────┘
                     shared/  Protocol, DTOs (sem lógica)
```

## Padrões usados

| Padrão | Onde | Objetivo |
|---|---|---|
| **Ports & Adapters** | `application/port` + `infrastructure` | trocar BD/rede sem tocar no negócio |
| **Use Case** | `application/usecase/**` | 1 classe = 1 ação do sistema |
| **Repository** | `*RepositoryImpl` | acesso a dados atrás de interface |
| **Unit of Work** | `UnitOfWork` / `JdbcUnitOfWork` | transação atómica por operação |
| **Composition Root** | `AppContext` | única classe que conhece implementações |
| **Value Object** | `GameId`, `TeamId`, ... | ids tipo-seguros |
| **State** | `Game` / `Team` | máquinas de estado com transições válidas |
| **Thread Pool** | `ClientThreadPool` | 1 thread por cliente, limitado |

## Onde procurar o quê

| Preciso de… | Ficheiro |
|---|---|
| ponto de entrada do servidor | `infrastructure/network/ServerMain.java` |
| montagem de todas as dependências | `infrastructure/network/AppContext.java` |
| loop `accept()` e pool de threads | `infrastructure/network/TcpServer.java` |
| tradução protocolo → casos de uso | `.../AdminClientHandler.java`, `.../PlayerClientHandler.java` |
| ciclo das rondas (temporização) | `infrastructure/concurrency/GameScheduler.java` |
| contagem "todos responderam" | `infrastructure/concurrency/RoundCoordinator.java` |
| núcleo transacional da resposta | `application/usecase/player/SubmitAnswerUseCase.java` |
| regras de transição de estado | `domain/entity/Game.java`, `domain/entity/Team.java` |
| tema visual JavaFX | `interfaces/common/Theme.java`, `interfaces/common/HeaderBar.java` |

## Fluxo de uma resposta (resumo)

```
JavaFX Jogador ──"ANSWER|42|B"──▶ TCP ──▶ ClientHandler (thread do pool)
   │
   ▼
SubmitAnswerUseCase  (UMA transação PostgreSQL)
   BEGIN
     SELECT game  ... FOR UPDATE     (deadline da ronda)
     SELECT team  ... FOR UPDATE     (serializa a equipa)
     valida estado / ronda / tempo
     INSERT answers   (UNIQUE(team,question) = anti-duplicação)
     UPDATE teams SET score = ...
   COMMIT
   │
   ▼
RoundCoordinator.signal(team)  →  quando TODAS responderam, o GameScheduler fecha a ronda
```

Detalhes completos em [`concorrencia.md`](concorrencia.md).

## Decisões de desenho

- **O servidor é a autoridade do tempo**, não o cliente. O deadline é
  `round_started_at + time_per_question`, gravado na partida. O cliente só desenha.
- **A resposta correta nunca sai do servidor** no ecrã de jogo (`QuestionDTO` não a tem).
- **Protocolo de texto `|`** — simples de depurar (`nc`, logs) e suficiente para o âmbito.
- **Sem ORM** — um mini-helpers JDBC (`Jdbc`) mantém as camadas explícitas e as
  transações controláveis para a demonstração de concorrência.
- **JavaFX programático + CSS** — o tema é um ficheiro CSS gerado em runtime
  (`Theme`), permitindo um visual cuidado sem depender de FXML.
