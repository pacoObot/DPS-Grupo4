# QuizRace — DPS Grupo 4

Projecto académico em Java 21: quiz local sequencial, jogo TCP para 2–4 jogadores, execução concorrente e benchmark. Inclui agora uma API Spring Boot com utilizadores, perfis, autenticação JWT e persistência PostgreSQL. Inclui uma interface React/TypeScript com login, área pessoal e gestão administrativa de contas.

> **Backend académico:** siga [instalação e execução](docs/08-instalacao-e-execucao.md), [contrato API](docs/05-api-e-protocolo.md) e [testes](docs/07-testes.md). O [plano](docs/06-plano-de-desenvolvimento.md) identifica o que está entregue e as fases seguintes. Os comandos TCP abaixo continuam válidos.

## Interface web

Com o `.env` configurado conforme o [guia de instalação](docs/08-instalacao-e-execucao.md):

```bash
docker compose -f compose.api.yml -f compose.web.yml up --build -d
```

Abra **http://localhost:3000** e entre com a conta administrativa configurada no primeiro arranque. O administrador cria as contas dos colegas em **Utilizadores**. A interface permite consultar a conta, criar utilizadores e alterar perfil/estado; perguntas e avaliações web serão implementadas nas próximas fases. O jogo TCP continua disponível nos comandos abaixo.

Para desenvolver a interface, use Node.js 22.12+ (recomendado 24), inicie a API e execute:

```bash
cd web
npm ci
npm run dev
```

Abra o endereço mostrado pelo Vite (normalmente http://localhost:5173). O proxy encaminha `/api` para http://127.0.0.1:8080. Se a API usar outra porta: `QUIZ_API_PROXY=http://127.0.0.1:18087 npm run dev`. Consulte o [guia da interface](docs/13-interface-web.md).

## Arquitectura

```text
QuizClient / ClienteSimulado (2–4 jogadores)
                  |
                  | TCP — linhas UTF-8
                  v
             QuizServer
                  |
           ExecutorService
                  |
           ClienteHandler
                  |
               SalaQuiz ---- coordenador único
                  |
        Rodada + CountDownLatch
                  |
 Pergunta / Resposta / Jogador / CalculadoraPontuacao
                  |
            RankingService
```

- **Sequencial local:** cada jogador responde a todas as perguntas antes do seguinte. A thread auxiliar de EntradaConsole permite aplicar o timeout ao teclado; não processa jogadores em paralelo.
- **Jogo em rede:** handlers recebem mensagens em simultâneo, enquanto SalaQuiz coordena perguntas comuns. O servidor decide tempo, correcção, pontuação e ranking.
- **Benchmark:** QuizSequencial e QuizParalelo executam exactamente a mesma JogadorTask com dados imutáveis reproduzíveis. Não usa TCP, teclado ou atrasos humanos.

TCP fornece um fluxo de bytes fiável e ordenado, mas não delimita mensagens. O protocolo usa uma mensagem por linha e `|` entre campos. Cada leitor impõe um máximo de 2048 caracteres antes de construir a mensagem completa.

## Requisitos e compilação

- JDK **21** completo (incluindo `javac`).
- Maven 3.8.7 ou superior (3.9.9 na imagem Docker).
- Docker Engine e Docker Compose para a demonstração em containers.

Abra a pasta que contém `pom.xml`:

```bash
mvn clean compile
mvn test
mvn package
```

São gerados `target/quizrace.jar` (CLI/TCP) e `target/quizrace-api.jar` (API executável com dependências). O jogo TCP continua a executar sem PostgreSQL; a API precisa da configuração descrita no guia. Os testes de integração usam portas livres (`ServerSocket(0)`); é necessário permitir sockets locais no ambiente de testes.

## Menu e versão local

```bash
mvn exec:java
```

```text
1 - Jogar versão local sequencial
2 - Iniciar servidor
3 - Iniciar cliente
4 - Executar benchmark
0 - Sair
```

O menu inicia o modo escolhido e termina quando esse modo acaba. Para entrar directamente na versão local:

```bash
mvn exec:java -Dexec.args="--local"
```

São aceites 2–4 nomes diferentes. Responda A, B, C ou D e prima Enter. Entradas inválidas não reiniciam o prazo. No terminal local, uma linha introduzida depois de um timeout pode ser lida pela pergunta seguinte; responda apenas quando solicitado.

## Servidor e dois clientes humanos

Execute os comandos em terminais separados, todos na pasta do projecto.

Terminal 1:

```bash
mvn exec:java -Dexec.mainClass="quizrace.server.QuizServer"
```

Terminais 2 e 3:

```bash
mvn exec:java -Dexec.mainClass="quizrace.client.QuizClient"
```

Introduza um nome diferente em cada cliente. Com dois jogadores, a sala inicia uma contagem de 3, 2, 1 e GO. Até ao GO podem entrar mais dois. Se alguém sair e restar menos de dois durante a contagem, a sala volta a aguardar.

Todos recebem as mesmas **10 perguntas**. Aguarde as quatro alternativas e o anúncio de tempo; escreva apenas a letra, sem prefixos de protocolo. A recepção de mensagens continua activa enquanto o cliente espera pelo teclado. Use `/sair` para desligar ou Ctrl+C para encerrar o processo.

Cada rodada termina quando todos responderem/desistirem ou quando os 15 segundos se esgotarem. Os resultados individuais e a pontuação acumulada são enviados no fecho da rodada, antes da pergunta seguinte. No fim, todos recebem ranking e vencedor; os sockets dos jogadores fecham. O servidor continua a aceitar ligações, mas informa que a sala está finalizada. Nesta versão, reinicie o servidor para jogar outra partida.

Outro hostname/porta:

```bash
mvn exec:java -Dexec.mainClass="quizrace.server.QuizServer" -Dexec.args="6000"
mvn exec:java -Dexec.mainClass="quizrace.client.QuizClient" -Dexec.args="192.168.1.10 6000"
```

Ou execute directamente o JAR:

```bash
java -cp target/quizrace.jar quizrace.server.QuizServer
java -cp target/quizrace.jar quizrace.client.QuizClient localhost 5000
```

ConfiguracaoRede resolve **argumento > variável de ambiente > padrão**. Variáveis: `QUIZ_HOST` e `QUIZ_PORT`; padrões: `localhost` e `5000`. O servidor recebe apenas a porta por argumento e escuta nas interfaces disponíveis. A porta deve estar acessível na rede. Porta ocupada gera uma mensagem de erro.

## Regras e modelo

- Resposta correcta: `max(10, 100 - tempoEmSegundos * 5)`.
- Resposta errada ou timeout: zero. Com 15 segundos, uma resposta correcta vale aproximadamente 25–100 pontos.
- Fracções de segundo são preservadas; a apresentação arredonda para duas casas. Valores visualmente iguais podem desempatar pelos valores internos.
- Jogador mantém pontuação, correctas, erradas, timeouts, tempo total/última resposta e ligação activa. **Timeouts são um subconjunto das erradas**, preservando o comportamento anterior.
- Desligar durante a partida regista timeout nas perguntas pendentes e seguintes; o jogador permanece no ranking. Desligar antes do jogo remove-o da sala.
- Ranking: maior pontuação, menor tempo total, mais correctas e, finalmente, nome para desempate determinístico. O primeiro é declarado vencedor, incluindo jogadores que tenham saído.
- Tempo é medido pelo servidor com System.nanoTime, após enviar enunciado/opções e ao anunciar o prazo. Inclui latência de rede e escalonamento; não depende do relógio do cliente.

As perguntas são carregadas de `src/main/resources/perguntas.txt`. O identificador positivo é atribuído pela ordem no ficheiro. Formato:

```text
enunciado|alternativa A|alternativa B|alternativa C|alternativa D|letra correcta|categoria
```

Categoria opcional, mantendo o último separador. Linhas vazias e começadas por `#` são ignoradas. Recompile após editar o ficheiro.

## ExecutorService e sincronização

QuizServer utiliza um pool limitado a oito atendimentos (quatro jogadores e ligações em identificação), sem fila ilimitada de sockets. Não cria manualmente uma thread por cliente. Ligações excedentes são fechadas; nomes ainda não enviados têm timeout de leitura de 60 segundos.

SalaQuiz utiliza um executor agendado com um único coordenador para a contagem e as rodadas. A entrada/saída e o estado da sala são protegidos por secções críticas curtas. Cada Rodada tem um **CountDownLatch**, inicializado com o número de jogadores. A inserção da resposta e a verificação de duplicação são atómicas num método synchronized. Cada jogador reduz a contagem uma única vez. O coordenador espera só até ao prazo restante, encerra a rodada e transforma ausências em timeout.

Os handlers **não alteram pontuação**. Apenas o coordenador aplica os resultados, e só depois produz o ranking. No benchmark, cada tarefa cria e possui o seu próprio Jogador; Future.get estabelece a publicação segura dos resultados. Não se aplica synchronized a todos os getters/setters.

O shutdown hook fecha o ServerSocket e os sockets dos clientes (desbloqueando leituras), interrompe os executores e aguarda a sua terminação. Não se usa Thread.sleep para sincronizar rodadas. A única espera desse tipo está nas tentativas de ligação do cliente simulado.

## Protocolo

| Mensagem | Utilização |
| --- | --- |
| `NOME|Ana` | Primeira mensagem do cliente; nome de 1–40 caracteres, sem controlos ou separadores |
| `ENTROU|Ana` | Entrada aceite |
| `JOGADORES|2|4` | Ocupação da sala |
| `AGUARDAR|Texto` | Sala de espera |
| `INICIO|3`, `INICIO|2`, `INICIO|1`, `INICIO|GO` | Contagem |
| `PERGUNTA|1|Enunciado` | Pergunta com identificador |
| `OPCAO|A|Texto` | Alternativa |
| `TEMPO|15` | Abrir o prazo da resposta |
| `RESPOSTA|1|B` | Resposta identificada, enviada pelos clientes actuais |
| `RESULTADO|CORRECTO|95.00` | Também admite ERRADO e TIMEOUT |
| `PONTUACAO|195.00` | Total do jogador |
| `RANKING|1|Ana|450.00|5|5|1|42.50` | Posição, nome, pontos, correctas, erradas, timeouts, tempo total |
| `VENCEDOR|Ana|450.00` | Vencedor |
| `ERRO|Texto` | Falha de validação |
| `FIM|Partida terminada` | Encerramento normal |
| `MENSAGEM|Texto` | Informação; o servidor mantém a confirmação de mensagens de teste |

O parser preserva os formatos antigos PERGUNTA sem ID, RESULTADO com TEMPO_ESGOTADO e RANKING reduzido. **RESPOSTA sem ID é aceite apenas na primeira pergunta da sala.** A partir daí exige-se ID para impedir que mensagens atrasadas ou duplicadas afectem outra rodada. Os clientes actuais enviam sempre o ID.

Separadores `|` e controlos não são permitidos dentro de campos. Não há JSON nem escape. Mensagens inválidas recebem ERRO sem derrubar o servidor; falhas de identificação, linhas excessivas ou desconexões encerram apenas o cliente afectado. Nomes duplicados, sala cheia e entrada após GO são rejeitados.

## Benchmark

```bash
mvn exec:java -Dexec.mainClass="quizrace.benchmark.BenchmarkRunner"
```

Parâmetros opcionais: threads, medições, iterações SHA-256 por pergunta:

```bash
mvn exec:java -Dexec.mainClass="quizrace.benchmark.BenchmarkRunner" -Dexec.args="4 5 1000"
```

São medidos 10, 100, 500 e 1000 jogadores, cada um com 10 perguntas. Uma semente fixa gera as mesmas respostas e tempos simulados para ambas as versões. A carga de CPU artificial repete SHA-256; os checksums são consumidos e comparados, além de pontuação, tempos e contadores.

Há um aquecimento de cada versão e cinco medições por padrão. A ordem alterna entre sequencial/paralela. O pool é reutilizado e aquecido; criação inicial do pool, geração dos dados, leitura de recursos e impressão não entram nas medições. Criação das tarefas, execução e recolha dos resultados entram. Cada execução cria novos Jogador, evitando acumular pontuações entre medições.

```text
speedup = média sequencial / média paralela
eficiência = speedup / número de threads
```

Os tempos medidos são de processamento, não os tempos fictícios de resposta usados para calcular pontos. Não se medem jogadores humanos nem latência TCP. Trata-se de uma demonstração académica, não de um microbenchmark JMH: JIT, GC, frequência de CPU e outros processos influenciam os resultados. Não há garantia de aceleração para cargas pequenas.

Resultados realmente obtidos estão em [docs/VALIDACAO.md](docs/VALIDACAO.md).

## Clientes simulados

O bot liga por TCP, regista o nome, lê as perguntas e agenda respostas sem bloquear a recepção. Usa o banco de perguntas local para acertar com a probabilidade configurada; perguntas desconhecidas recebem uma alternativa aleatória. Nunca envia pontuação nem decide o resultado oficial.

```bash
mvn exec:java -Dexec.mainClass="quizrace.client.ClienteSimulado" -Dexec.args="localhost 5000 BOT-1 100 500 0.75"
```

Ordem: host, porta, nome, atraso mínimo em ms, atraso máximo em ms, probabilidade de acerto (0–1). Variáveis equivalentes: `QUIZ_HOST`, `QUIZ_PORT`, `QUIZ_NOME`, `QUIZ_ATRASO_MIN_MS`, `QUIZ_ATRASO_MAX_MS`, `QUIZ_ACERTO`. Os argumentos têm prioridade. Defaults: BOT-1, 100–500 ms, 0.75. Use nomes diferentes. A probabilidade 1 acerta sempre no banco conhecido; 0 escolhe sempre uma alternativa errada. Atrasos superiores a 15 segundos demonstram timeout.

## Docker e Compose

```bash
docker compose build
docker compose up
```

O Dockerfile usa duas etapas: Maven/JDK 21 compila **e executa os testes**; a imagem final contém apenas o JRE 21 e o JAR, executados por um utilizador sem privilégios. Compose cria uma rede comum, servidor (`server:5000`) e quatro bots BOT-1…BOT-4. Os bots tentam novamente se o servidor ainda não estiver pronto.

No fim, os quatro bots terminam com código zero e o servidor continua activo com a sala finalizada. Para encerrar e repetir:

```bash
docker compose down
docker compose up
```

Execução em segundo plano e consulta de resultados:

```bash
docker compose up -d
docker compose logs -f
docker compose ps -a
docker compose down
```

Só servidor em Docker, com clientes humanos fora dele:

```bash
docker compose up server
# Noutro terminal:
mvn exec:java -Dexec.mainClass="quizrace.client.QuizClient"
```

Se a porta 5000 do computador estiver ocupada:

```bash
QUIZ_PUBLISH_PORT=6000 docker compose up
```

Os bots continuam a usar `server:5000`; clientes no computador usam `localhost 6000`. O nome `server` só é resolvido dentro da rede Compose. Não use `--abort-on-container-exit` para observar todos os rankings, pois o primeiro bot a terminar pode parar os restantes.

## Estrutura e responsabilidades

```text
QuizRace/
├── pom.xml
├── README.md
├── Dockerfile
├── docker-compose.yml
├── .dockerignore
├── .gitignore
├── docs/VALIDACAO.md
└── src/
    ├── main/
    │   ├── java/quizrace/
    │   │   ├── Main.java
    │   │   ├── model/{Jogador,Pergunta,Resposta}.java
    │   │   ├── service/
    │   │   │   ├── QuizSequencial.java
    │   │   │   ├── QuizParalelo.java
    │   │   │   ├── CalculadoraPontuacao.java
    │   │   │   ├── GerenciadorPerguntas.java
    │   │   │   └── RankingService.java
    │   │   ├── server/
    │   │   │   ├── QuizServer.java
    │   │   │   ├── ClienteHandler.java
    │   │   │   ├── SalaQuiz.java
    │   │   │   ├── Rodada.java
    │   │   │   ├── EstadoSala.java
    │   │   │   └── CanalJogador.java
    │   │   ├── client/{QuizClient,ClienteSimulado}.java
    │   │   ├── network/{ConfiguracaoRede,ProtocoloMensagem}.java
    │   │   ├── parallel/JogadorTask.java
    │   │   ├── benchmark/{BenchmarkRunner,JogadorSimulado,ResultadoBenchmark}.java
    │   │   └── util/{Cronometro,MedidorDesempenho,EntradaConsole}.java
    │   └── resources/perguntas.txt
    └── test/java/quizrace/
        ├── QuizRaceTest.java
        ├── ProtocoloMensagemTest.java
        ├── JogoConcorrenteTest.java
        ├── TcpIntegrationTest.java
        ├── BenchmarkTest.java
        └── ClienteSimuladoTest.java
```

Main apresenta o menu. Models representam os dados. Services carregam perguntas, executam os modos e calculam os resultados. SalaQuiz coordena a partida, Rodada protege as respostas, ClienteHandler trata a ligação e CanalJogador permite testar a sala sem sockets. Network valida configuração e protocolo. BenchmarkRunner mede; JogadorSimulado fornece a carga; JogadorTask executa-a; ResultadoBenchmark calcula os indicadores. Os utilitários tratam tempo e entrada do terminal.

## Testes e evolução

Os testes originais foram preservados; apenas a quantidade esperada de perguntas passou de 3 para 10. A suite acrescenta modelo, desempate, sala cheia, nomes duplicados, regresso à espera, prazo com relógio injectado, 100 tentativas concorrentes de resposta duplicada, protocolo/limites, equivalência de benchmark e integração TCP em porta livre.

A implementação foi guardada em commits incrementais: base TCP, jogo sincronizado, benchmark e execução em containers. Consulte `git log --oneline` e [a validação](docs/VALIDACAO.md).

## Limitações do jogo TCP e próximas melhorias

- Uma sala e uma partida por processo de servidor; sem reconexão, múltiplas salas ou persistência. Reinicie para nova partida.
- Sem TLS/autenticação; apropriado para ambiente académico controlado. O limite de linhas e clientes não substitui protecção contra abuso. Escritas TCP são bloqueantes; um cliente que nunca leia pode atrasar a distribuição.
- Clientes antigos de uma só pergunta não completam o novo jogo; devem usar o cliente actual com identificadores.
- A contagem começa ao atingir dois jogadores; os restantes precisam de entrar antes do GO.
- O tempo de rede inclui latência; não representa apenas o tempo de raciocínio humano.
- O benchmark é de carga CPU simulada, separado do desempenho do jogo interactivo.
- Futuro: novas salas, recuperação de ligação, saída de rede com prazo, autenticação e medições mais rigorosas com JMH.

### Ferramentas instaladas

Maven e JDK 21 foram instalados no sistema em 21 de Outubro de 2026. Seleccione Java 21 no terminal:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
mvn clean verify
```

As instruções da API e os resultados desta entrega estão em [execução](docs/08-instalacao-e-execucao.md) e [testes](docs/07-testes.md). Docker não depende do Maven instalado no host.
