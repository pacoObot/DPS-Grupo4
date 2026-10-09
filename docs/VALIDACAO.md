# Validação — 9 de Outubro de 2026

## Ambiente

JDK 21.0.12.1, Maven 3.9.9, quatro processadores disponíveis à JVM. Docker Engine e Compose disponíveis. JDK/Maven foram instalados temporariamente em `/tmp`, pois as ferramentas da sessão anterior já tinham sido removidas.

## Testes e execução local

- Base TCP anterior: 12 testes aprovados antes do primeiro commit desta continuação.
- Sala/rodadas: 20 testes aprovados, incluindo TCP numa porta atribuída pelo sistema.
- Benchmark: 23 testes aprovados.
- Cliente simulado: 24 testes aprovados.
- Jogo local executado pelo menu Maven: 10 perguntas para cada um de dois jogadores e ranking apresentado.
- Servidor e dois QuizClient reais iniciados via Maven: 10 perguntas comuns, pontuações, resposta errada, timeout real de 15 segundos e ranking idêntico nos dois clientes.

Resultado observado na partida CLI controlada (a pontuação varia com o tempo):

| Jogador | Pontos | Correctas | Erradas | Timeouts | Tempo total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Ana | 997.50 | 10 | 0 | 0 | 0.50 s |
| Bruno | 99.73 | 1 | 9 | 1 | 15.43 s |

## Benchmark realmente medido

Comando: `mvn exec:java -Dexec.mainClass=quizrace.benchmark.BenchmarkRunner`.

Quatro threads; dez perguntas; 1000 operações SHA-256 por pergunta; semente 42; um warm-up por versão e cinco medições por cenário. Tempos médios em milissegundos:

| Jogadores | Sequencial | Paralelo | Speedup | Eficiência |
| ---: | ---: | ---: | ---: | ---: |
| 10 | 40.845 | 26.833 | 1.522x | 0.381 |
| 100 | 357.121 | 230.132 | 1.552x | 0.388 |
| 500 | 1772.196 | 1108.084 | 1.599x | 0.400 |
| 1000 | 3427.618 | 2344.970 | 1.462x | 0.365 |

Todas as execuções compararam também checksums, nomes, pontuações, contadores e tempos simulados. São observações deste ambiente, não valores esperados em qualquer máquina nem medições de latência do jogo TCP.

## Problemas encontrados e tratados

- Disco inicialmente sem espaço: trabalho retomado depois de o utilizador libertar espaço.
- Maven e JDK temporários ausentes: ferramentas restauradas, sem alterar a instalação do sistema.
- Isolamento bloqueou downloads e sockets: validação repetida com acesso autorizado; os testes de rede não foram ignorados.
- Respostas sem identificador poderiam atingir a rodada seguinte: clientes actuais enviam ID; formato antigo restrito à primeira pergunta.
- Fechar apenas executores não desbloqueia necessariamente leituras de socket: o shutdown fecha os sockets primeiro.
- Aumento do banco de perguntas exigiu actualizar uma asserção de 3 para 10, preservando todos os testes anteriores.

## Docker realmente executado

- `docker compose config --quiet`: configuração válida.
- `docker compose build`: imagem construída; `mvn verify` dentro do build passou os **24 testes**, sem falhas nem testes ignorados.
- `docker compose up -d`: servidor e quatro clientes na mesma rede.
- `docker compose wait player1 player2 player3 player4`: todos terminaram com **código 0**.
- Conferência automática dos logs: dez mensagens PERGUNTA iguais em todos os clientes, quatro linhas RANKING iguais e FIM em cada um.
- `docker compose down`: containers e rede de teste encerrados; a imagem permanece disponível para repetir a demonstração.

Ranking observado nos quatro containers:

| Posição | Jogador | Pontos | Correctas | Erradas | Timeouts | Tempo |
| ---: | --- | ---: | ---: | ---: | ---: | ---: |
| 1 | BOT-1 | 886.98 | 9 | 1 | 0 | 2.73 s |
| 2 | BOT-2 | 882.77 | 9 | 1 | 0 | 3.75 s |
| 3 | BOT-3 | 591.78 | 6 | 4 | 0 | 2.70 s |
| 4 | BOT-4 | 590.71 | 6 | 4 | 0 | 2.98 s |

## Verificação final

`mvn clean compile` e `mvn test` foram executados separadamente: **BUILD SUCCESS**, 24 testes, zero falhas, zero erros e zero ignorados. O JAR também foi gerado com `mvn -DskipTests package` após a suite completa; o build Docker executou `verify` sem saltar testes.
