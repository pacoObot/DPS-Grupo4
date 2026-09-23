# QuizRace

Projecto académico em Java para um quiz de 2 a 4 jogadores. Esta primeira etapa implementa a versão sequencial no terminal. Não utiliza frameworks de aplicação; JUnit é usado apenas nos testes.

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
    │   │   ├── parallel/
    │   │   │   └── JogadorTask.java
    │   │   └── util/
    │   │       ├── Cronometro.java
    │   │       ├── MedidorDesempenho.java
    │   │       └── EntradaConsole.java
    │   └── resources/perguntas.txt
    └── test/java/quizrace/QuizRaceTest.java
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
