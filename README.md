# QuizRace

Projecto académico em Java para um quiz de 2 a 4 jogadores. Inclui a versão sequencial no terminal e uma demonstração de comunicação TCP entre servidor e clientes. Não utiliza frameworks de aplicação; JUnit é usado apenas nos testes.

## Executar

Requisitos: JDK 17 ou superior e Apache Maven 3.9 ou superior disponíveis no PATH.

```bash
cd QuizRace
mvn clean compile
mvn exec:java
```

Para executar os testes:

```bash
mvn test
```

Também pode importar `pom.xml` numa IDE e executar `quizrace.Main`, ou, depois de compilar:

```bash
java -cp target/classes quizrace.Main
```

Registe entre 2 e 4 jogadores com nomes diferentes. Cada jogador responde a todas as perguntas antes de passar a vez ao seguinte. Escreva A, B, C ou D e prima Enter. A contagem começa depois da apresentação das alternativas. Uma entrada inválida permite nova tentativa dentro do mesmo prazo.

## Regras

- Limite de 15 segundos por pergunta; tempo esgotado conta como resposta errada e 15 segundos no tempo do jogador.
- Resposta errada: 0 pontos.
- Resposta correcta: `max(10, 100 - tempoEmSegundos * 5)`; as fracções de segundo são preservadas no cálculo. A apresentação usa duas casas decimais.
- Com o prazo actual, uma resposta correcta vale pelo menos cerca de 25 pontos. O mínimo de 10 está implementado para permitir futuros limites diferentes.
- Ranking por pontuação decrescente; desempate pelo menor tempo total e, depois, pelo nome. As posições são ordinais.
- O fim da entrada interrompe o jogo com uma mensagem.

Uma thread auxiliar daemon lê o terminal e entrega linhas a uma fila. Isto permite aplicar o limite de tempo mesmo quando ninguém prime Enter. A lógica do quiz e os jogadores continuam sequenciais. O terminal não permite identificar a pergunta a que uma linha tardia pertence: após um timeout, a próxima linha será considerada resposta à pergunta que estiver activa. Responda apenas quando solicitado e não antecipe respostas.

## Estrutura e responsabilidades

```text
QuizRace/
├── pom.xml
├── README.md
├── .gitignore
└── src/
    ├── main/
    │   ├── java/quizrace/
    │   │   ├── Main.java
    │   │   ├── model/
    │   │   │   ├── Jogador.java
    │   │   │   ├── Pergunta.java
    │   │   │   └── Resposta.java
    │   │   ├── service/
    │   │   │   ├── QuizSequencial.java
    │   │   │   ├── QuizParalelo.java
    │   │   │   ├── CalculadoraPontuacao.java
    │   │   │   ├── GerenciadorPerguntas.java
    │   │   │   └── RankingService.java
    │   │   ├── server/
    │   │   │   ├── QuizServer.java
    │   │   │   └── ClienteHandler.java
    │   │   ├── client/
    │   │   │   └── QuizClient.java
    │   │   ├── network/
    │   │   │   ├── ConfiguracaoRede.java
    │   │   │   └── ProtocoloMensagem.java
    │   │   ├── parallel/
    │   │   │   └── JogadorTask.java
    │   │   └── util/
    │   │       ├── Cronometro.java
    │   │       ├── MedidorDesempenho.java
    │   │       └── EntradaConsole.java
    │   └── resources/perguntas.txt
    └── test/java/quizrace/
        ├── QuizRaceTest.java
        └── ProtocoloMensagemTest.java
```

| Classe | Responsabilidade |
| --- | --- |
| Main | Registar jogadores, iniciar o jogo e apresentar o ranking. |
| Jogador | Guardar nome, pontuação, contadores e tempos total e da última resposta. |
| Pergunta | Validar quatro alternativas e verificar a resposta e o prazo; dados imutáveis. |
| Resposta | Representar alternativa, tempo e indicação de timeout; dados imutáveis. |
| CalculadoraPontuacao | Aplicar a fórmula de pontos. |
| GerenciadorPerguntas | Carregar e validar as perguntas do recurso UTF-8. |
| QuizSequencial | Processar cada jogador e cada pergunta, medindo e registando resultados. |
| RankingService | Produzir uma lista ordenada sem alterar a lista original. |
| Cronometro | Medir intervalos com o relógio monotónico System.nanoTime. |
| MedidorDesempenho | Medir a duração de uma tarefa. |
| EntradaConsole | Ler o terminal através de uma fila e permitir espera com prazo. |
| QuizParalelo | Classe base vazia, reservada para a segunda etapa. |
| JogadorTask | Classe base vazia, reservada para a futura tarefa de um jogador. |

`Pergunta` e `Resposta` usam records, disponíveis em Java 17. Para adicionar perguntas, edite `src/main/resources/perguntas.txt` e volte a compilar. Formato de cada linha:

```text
enunciado|alternativa A|alternativa B|alternativa C|alternativa D|letra correcta|categoria
```

A categoria pode ficar vazia, mantendo o último separador. Não utilize `|` dentro dos textos. Linhas vazias e linhas começadas por `#` são ignoradas.

## Segunda etapa e comparação

`QuizParalelo` e `JogadorTask` ainda não executam tarefas. A segunda etapa deverá usar ExecutorService, uma tarefa por jogador e aguardar a conclusão antes de ler os resultados. Cada tarefa poderá ser dona exclusiva do seu Jogador, evitando sincronização desnecessária. Actualmente Jogador não permite utilização concorrente segura por várias tarefas.

Um único terminal partilhado não oferece entrada humana simultânea independente. Para demonstrar concorrência e comparar tempos de forma justa, a segunda etapa deve usar respostas simuladas reproduzíveis, com as mesmas perguntas, respostas e atrasos nas duas versões, ou canais independentes por jogador.

O tempo sequencial apresentado inclui a espera pelas respostas e a saída das perguntas, mas exclui registo, carregamento e impressão do ranking. Ainda não é um benchmark de processamento nem existe medição da versão paralela.

## Validação

Os testes verificam a fórmula, os dados de Jogador e Pergunta, o prazo de leitura, o carregamento do recurso, a ordenação e uma partida sequencial com entrada em memória. Não dependem de introdução manual.

### Ambiente usado nesta entrega

O ambiente original tinha Java para execução, mas não tinha Maven nem JDK completo. A validação utilizou Maven 3.9.9 e JDK 21 temporários em `/tmp`, sem alterar o sistema. Enquanto essas pastas existirem, pode executar a partir de `QuizRace/`:

```bash
export JAVA_HOME=/tmp/quizrace-jdk
export PATH="/tmp/apache-maven-3.9.9/bin:$JAVA_HOME/bin:$PATH"
mvn -Dmaven.repo.local=/tmp/quizrace-m2 clean compile
mvn -Dmaven.repo.local=/tmp/quizrace-m2 test
mvn -Dmaven.repo.local=/tmp/quizrace-m2 exec:java
```

Para utilização permanente, instale um JDK completo e Maven e use os comandos normais indicados no início.

## Comunicação TCP — fase actual

TCP é usado para comunicação entre cliente e servidor. Fornece um fluxo de bytes fiável e ordenado; neste projecto, cada mensagem é uma linha UTF-8 terminada por uma mudança de linha. A aplicação interpreta essas linhas, pois TCP não delimita mensagens automaticamente.

`QuizServer` abre um ServerSocket na porta **5000**, aceita ligações e fecha os sockets ao terminar com Ctrl+C. `ClienteHandler` atende um cliente, regista o seu nome, devolve uma mensagem de teste e apresenta a primeira pergunta de `perguntas.txt`. Reutiliza Pergunta, Jogador, Resposta, CalculadoraPontuacao e GerenciadorPerguntas. `QuizClient` lê o terminal e mostra as mensagens recebidas. `ProtocoloMensagem` centraliza construção, parsing e validação. `ConfiguracaoRede` concentra hostname padrão (`localhost`), porta e tempos de espera.

Há uma thread simples por ligação para que um cliente à espera de entrada não bloqueie os outros. Cada handler possui o seu Jogador; apenas o conjunto de sockets tem um bloqueio curto para coordenar o encerramento. O cliente mantém a recepção activa enquanto aguarda entrada do terminal, permitindo observar timeouts e desligamentos. Isto ainda **não é a versão paralela académica final**.

### Testar em três terminais

Em cada terminal, abra a pasta que contém o `pom.xml`. Compile primeiro:

```bash
mvn clean compile
mvn test
```

Terminal 1 — servidor:

```bash
mvn exec:java -Dexec.mainClass="quizrace.server.QuizServer"
```

Terminal 2 — primeiro cliente:

```bash
mvn exec:java -Dexec.mainClass="quizrace.client.QuizClient"
```

Terminal 3 — segundo cliente:

```bash
mvn exec:java -Dexec.mainClass="quizrace.client.QuizClient"
```

Introduza `Ana` no primeiro cliente e `Bruno` no segundo. Deixe ambos ligados antes de enviar a mensagem de teste. Cada um deverá receber `MENSAGEM|Bem-vindo, <nome>!`. Escreva, por exemplo, `Olá servidor`. O servidor regista o texto e responde `MENSAGEM|Recebido: Olá servidor`. Em seguida envia a primeira pergunta do ficheiro (actualmente sobre a palavra-chave que cria uma classe Java). Responda `A` para acertar ou `B` para testar uma resposta errada. Prima Enter após cada entrada.

O cliente acrescenta automaticamente NOME, MENSAGEM e RESPOSTA: não escreva os prefixos no terminal. Aguarde as instruções antes de responder. O servidor calcula o resultado, envia FIM e fecha essa ligação, continuando disponível para outros clientes. `/sair` desliga o cliente em qualquer fase. Ctrl+C encerra o servidor e as ligações activas.

Para outro computador na mesma rede, indique o IP ou hostname do servidor:

```bash
mvn exec:java -Dexec.mainClass="quizrace.client.QuizClient" -Dexec.args="192.168.1.10"
```

O servidor escuta nas interfaces de rede disponíveis; a porta TCP 5000 deve estar acessível. Se estiver ocupada, liberte-a ou altere a constante PORTA e recompile servidor e clientes. Se utilizar as ferramentas temporárias desta entrega, aplique as variáveis de ambiente descritas acima e acrescente `-Dmaven.repo.local=/tmp/quizrace-m2` aos comandos Maven.

### Protocolo

| Direcção | Mensagem | Significado |
| --- | --- | --- |
| Cliente → servidor | `NOME|Ana` | Identificação inicial |
| Cliente → servidor | `MENSAGEM|Olá servidor` | Texto de teste, após o nome |
| Cliente → servidor | `RESPOSTA|A` | Resposta à pergunta |
| Servidor → cliente | `MENSAGEM|Bem-vindo, Ana!` | Informação ou confirmação |
| Servidor → cliente | `PERGUNTA|Enunciado` | Pergunta |
| Servidor → cliente | `OPCAO|A|Texto` | Uma das quatro alternativas |
| Servidor → cliente | `RESULTADO|CORRECTO|85.00` | Resultado e pontos |
| Servidor → cliente | `ERRO|Descrição` | Resposta inválida; pode tentar novamente |
| Servidor → cliente | `FIM|Descrição` | Fim normal da sessão |

RESULTADO admite CORRECTO, ERRADO ou TEMPO_ESGOTADO. Os pontos usam ponto decimal, independentemente do idioma do sistema. Campos vazios, quebras de linha e `|` dentro de campos são rejeitados; não existe escape nem JSON. O nome deve chegar primeiro, seguido de uma mensagem de teste e de uma resposta. Um erro na identificação ou na mensagem inicial encerra apenas essa ligação e fica registado no servidor. Durante a pergunta, entradas inválidas recebem ERRO e não reiniciam o prazo.

### Limitações actuais

- Uma pergunta por ligação, sem sala partilhada, ranking em rede ou gestão global de 2–4 jogadores. A versão sequencial continua disponível sem alterações.
- Prazo de 15 segundos medido no servidor após enviar a pergunta e instruções; inclui a latência de rede. Timeout vale zero pontos. A identificação e a mensagem de teste têm espera de leitura de 60 segundos cada.
- Nomes não são únicos entre ligações. Sem autenticação, TLS, reconexão, persistência ou limites de clientes/tamanho de linhas; destina-se a demonstração numa rede controlada.
- A leitura do terminal reutiliza EntradaConsole, cuja thread daemon termina com o processo. Sockets e streams de rede são fechados no fim da sessão; o cliente deixa de enviar quando a ligação fecha.
- Docker será introduzido posteriormente para facilitar a execução e simulação de múltiplos clientes.
- ExecutorService será introduzido posteriormente para implementar e avaliar a versão paralela. Não existem ainda pool, benchmark ou testes de speedup.

Os testes de ProtocoloMensagem verificam construção, parsing, mensagens inválidas, conversão entre texto e campos e validação de respostas com as regras existentes. Não requerem portas de rede; os sete testes anteriores são preservados.
