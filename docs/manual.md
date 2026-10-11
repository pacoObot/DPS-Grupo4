# Manual de utilização

Três programas: **Servidor** (`server.jar`), **Admin** (JavaFX) e **Jogador** (JavaFX).
Os bots (`bot.jar`) são opcionais, para demonstração de carga.

## 0. Preparação (uma vez)

```bash
./scripts/setup-db.sh     # cria a role `quiz` e as bases quiz_parallel / _test
./scripts/build.sh        # compila e gera os jars
```

## 1. Servidor

```bash
./scripts/run-server.sh            # porta TCP 5050
QUIZ_PORT=6000 ./scripts/run-server.sh
```

Ao arrancar, aplica as migrações Flyway (V1–V8), cria o admin `admin/admin` (se não
existir) e abre o socket. Os logs vão para o ecrã e para `logs/quiz-server.log`,
com o **nome da thread** em cada linha.

## 2. Administrador (JavaFX)

```bash
./scripts/run-admin.sh
```

1. **Login** — `admin` / `admin`.
2. **Perguntas** (`QuestionManagementView`) — criar/editar/eliminar/ativar.
   Campos: texto, categoria, dificuldade, opções A–D, resposta correta, pontos,
   tempo-limite, ativo. Use **Pesquisar** e **Atualizar lista** para filtrar.
3. **Partida** (`GameManagementView`) — **CRIAR PARTIDA** com nome, seleção de perguntas
   e tempo por pergunta. De seguida:
   - **abrir inscrições** → gera um **código** (ex.: `7K3F`) a partilhar com as equipas;
   - **START_GAME** para iniciar; **PAUSE** / **RESUME** a meio (o tempo congela/retoma);
   - **STOP_GAME** para terminar.
   - **Atualizar equipes** e **Atualizar classificação** mostram, respetivamente, as
     equipas (com a thread que as serviu) e a pontuação ao vivo.
4. **Monitor** (`MonitoringView`) — `GET_MONITOR`: mostra thread ↔ equipa ↔ estado,
   provando visualmente o paralelismo do servidor.

## 3. Jogador (JavaFX)

```bash
./scripts/run-player.sh     # pode abrir várias instâncias
```

1. **Login** — indicar o **código** da partida e o **nome da equipa**. **ENTRAR**.
2. **Sala de espera** (`LobbyView`) — mostra `WAITING|nOnline|nTotal` em tempo real.
3. **Pergunta** (`QuizView`) — aparece a pergunta, as opções A–D, um **cronómetro** e
   uma **barra de progresso**. Escolher uma opção para responder.
   - É dada feedback imediato: `CORRECT` (com pontos e bónus de tempo), `WRONG`,
     `TIMEOUT`, `DUPLICATE` (já respondida) ou `LATE` (fora da ronda).
4. **Classificação** — `SCOREBOARD` atualiza após cada ronda.
5. **Fim** (`ResultView`) — `GAME_OVER` com a classificação final.

> Só é permitida **uma resposta por equipa e por pergunta**.

## 4. Bots (opcional)

Depois de o admin abrir a partida e ter o código:

```bash
./scripts/run-bots.sh 7K3F 50         # 50 bots entram e jogam sozinhos
```

Parâmetros: `--code`, `--bots`, `--host`, `--port`, `--delay` (ms máx. de "pensar"),
`--option` (`random` ou `A`–`D`), `--prefix` (nome das equipas).

## 5. Demonstração automática (sem GUI)

```bash
./scripts/demo.sh 20 3 10
```

Faz tudo: arranca o servidor, cria a partida com `admin_cli.py` (cliente TCP em Python),
lança N bots, inicia o jogo e imprime a **classificação final**. Logs em
`logs/quiz-demo.log` e `logs/quiz-demo-bots.log`.

## 6. Perguntas frequentes

| Sintoma | Causa / solução |
|---|---|
| `Address already in use` | porta TCP ocupada → `QUIZ_PORT=5050 ./scripts/run-server.sh` |
| `JOIN_ERROR` código inválido | confirme que as inscrições estão abertas e o código está correto |
| Resposta `LATE` | o cliente respondeu depois de a ronda ter fechado (o servidor manda no tempo) |
| Resposta `DUPLICATE` | a equipa já tinha respondido a essa pergunta |
| Testes `*IT` saltados | o PostgreSQL não está acessível — arranque-o e corra `setup-db.sh` |
| GUI não abre | ambiente sem display (X11/Wayland) — use `demo.sh` para a demo sem GUI |
