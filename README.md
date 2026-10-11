# Quiz Multiplayer Paralelo

Sistema **cliente-servidor em rede** de quiz para várias equipas em simultâneo.
Escrito em **Java 21**, comunicação por **TCP Socket**, persistência em **PostgreSQL**,
interfaces gráficas em **JavaFX** (programáticas + CSS) e arquitetura limpa em camadas.

O foco do projeto é **programação concorrente e em rede**: cada equipa é servida numa
thread própria, o servidor é a autoridade do tempo, e a pontuação nunca se perde mesmo
com todas as equipas a responder no mesmo instante (transações + `SELECT ... FOR UPDATE`).

```
        ┌────────────┐   TCP   ┌──────────────────────────────┐   JDBC   ┌────────────┐
        │  JavaFX    │◀───────▶│   SERVIDOR (thread pool)      │◀────────▶│ PostgreSQL │
        │ Admin /    │  texto  │  accept → ClientHandler       │          │ quiz_parallel
        │ Jogador    │  `|`    │  GameScheduler (rondas)       │          └────────────┘
        └────────────┘         └──────────────────────────────┘
        ┌────────────┐              ▲
        │ Enxame de  │──────────────┘   (bots de carga para a demonstração)
        │ bot.jar    │
        └────────────┘
```

---

## Funcionalidades

- **Admin (JavaFX):** consola com **navegação lateral** e **dashboard de indicadores
  em tempo real** (threads, ligações, perguntas, partida, respostas, top 3),
  CRUD de perguntas com filtros e validação, criar/configurar partida,
  abrir inscrições, iniciar/pausar/retomar/encerrar, classificação e **monitor de threads**.
- **Jogador (JavaFX):** entrar com código da partida, sala de espera ao vivo,
  ecrã de pergunta com **cronómetro e barra de progresso**, feedback imediato,
  **banner de pausa**, classificação ao vivo e resultado final com **pódio**.
- **Servidor:** pool de threads, `GameScheduler` que conduz as rondas, transações
  atómicas por resposta, validação estrita de todo o protocolo.
- **Bots:** enxame de jogadores automáticos (`bot.jar`) para testar carga e concorrência.
- **Persistência:** migrações versionadas com **Flyway** (V1–V8).

---

## Requisitos

| Ferramenta | Versão |
|---|---|
| Java (JDK) | 21 ou superior |
| Maven | 3.9+ |
| PostgreSQL | 14+ (local, **sem Docker**) |

> O `pom.xml` usa `maven.compiler.release=21`, pelo que funciona em JDKs recentes
> (testado em Java 26).

---

## Arranque rápido

```bash
# 1) Preparar o PostgreSQL local (cria role `quiz` e as bases)
./scripts/setup-db.sh

# 2) Compilar (jars do servidor e dos bots)
./scripts/build.sh

# 3) Arrancar o servidor  (aplica migrações e cria o admin/admin)
./scripts/run-server.sh              # porta TCP 5050

# 4) Noutros terminais:
./scripts/run-admin.sh               # interface do administrador
./scripts/run-player.sh              # interface do jogador (abrir várias vezes)
```

No admin: **admin / admin** → crie perguntas → crie a partida → copie o código →
abra inscrições → inicie. No jogador, use esse código e um nome de equipa.

### Cliente web (browser / telemóvel)

Além do cliente JavaFX, existe um **gateway web** (módulo separado
`quiz-paralelo-web`, Spring Boot) que serve uma página *mobile-first* e faz
ponte por WebSocket para o servidor TCP do quiz. É assim que os jogadores
entram **pela web** — inclusive pelo telemóvel, na mesma Wi‑Fi.

```bash
# com o servidor do quiz a correr (porta 5050):
./scripts/run-web.sh                 # arranca o gateway na porta 8090
```

- No PC: `http://localhost:8090`
- No telemóvel (mesma rede Wi‑Fi): `http://<IP-desta-máquina>:8090`
  (o servidor do quiz escuta em `0.0.0.0:5050` e o gateway em `0.0.0.0:8090`).

Cada separador/aba do browser é um jogador: abre uma ligação TCP própria ao
servidor do quiz e reencaminha `JOIN_GAME`, `QUESTION`, `ANSWER`… em tempo real.

### Enxame de bots (demonstração de concorrência)

```bash
./scripts/run-bots.sh <CODIGO> 50        # 50 bots entram e jogam sozinhos
```

### Demonstração automática ponta-a-ponta (sem JavaFX)

```bash
./scripts/demo.sh 20 3 10                # 20 bots, 3 perguntas, 10s cada
```

Arranca o servidor, cria a partida, lança os bots, inicia o jogo e imprime a
classificação final no terminal.

---

## Configuração

Lida por esta ordem: **variável de ambiente → `db.properties`** (produção nunca
deve ter segredos no ficheiro).

| Variável | Default | Descrição |
|---|---|---|
| `DB_HOST` | `127.0.0.1` | host do PostgreSQL |
| `DB_PORT` | `5432` | porta do PostgreSQL |
| `DB_NAME` | `quiz_parallel` | base de dados |
| `DB_USER` / `DB_PASSWORD` | `quiz` / `quiz` | credenciais |
| `QUIZ_PORT` | `5050` | porta TCP do servidor |
| `QUIZ_THREADS` | `50` | tamanho do pool de threads |
| `DB_POOL_SIZE` | `10` | conexões JDBC |

Exemplo: `QUIZ_PORT=6000 ./scripts/run-server.sh`

---

## Testes

```bash
mvn -o test        # unitários (*Test) + integração (*IT) — base local necessária para os *IT
./scripts/build.sh --tests
```

- **Unitários (`*Test`)** — domínio, protocolo, config, use cases com mocks. Não usam BD.
- **Integração (`*IT`)** — PostgreSQL **real**: repositórios, fluxo transacional de
  resposta e **prova de concorrência** (sem *lost updates*).

> Se o PostgreSQL não estiver acessível, os testes `*IT` são **saltados**
> automaticamente (assumption) e os unitários continuam a passar.

---

## Estrutura (Clean Architecture)

```
quiz-paralelo/
├── src/main/java/quiz/
│   ├── domain/            # entidades e regras puras (sem tecnologia)
│   ├── application/       # portas (interfaces) + casos de uso + serviços
│   ├── infrastructure/    # JDBC, Flyway, rede, concorrência, bots, segurança
│   ├── interfaces/        # JavaFX (admin, jogador) + cliente TCP + tema
│   └── shared/            # protocolo e DTOs
├── src/main/resources/db/migration/   # Flyway V1..V8
├── scripts/               # setup-db, build, run-*, demo, admin_cli.py
├── docs/                  # arquitetura, protocolo, concorrência, manual
└── pom.xml
```

A regra de dependência aponta **sempre para dentro**: `interfaces` e `infrastructure`
dependem de `application`, que depende de `domain`. O `domain` não conhece ninguém.

---

## Documentação

- [`docs/arquitetura.md`](docs/arquitetura.md) — camadas, padrões, decisões.
- [`docs/protocolo.md`](docs/protocolo.md) — todas as mensagens TCP.
- [`docs/concorrencia.md`](docs/concorrencia.md) — threads, locks, transações.
- [`docs/manual.md`](docs/manual.md) — manual de utilização (admin e jogador).
