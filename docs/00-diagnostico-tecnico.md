# Diagnóstico técnico — Fase 0

Data: 21 de Outubro de 2026

> Este documento preserva o diagnóstico anterior à implementação. Maven e JDK 21 foram entretanto instalados e a fundação de utilizadores/API/PostgreSQL implementada. O estado actual está em [arquitectura](03-arquitectura.md), [plano](06-plano-de-desenvolvimento.md) e [testes](07-testes.md).

## Estado real

O QuizRace é uma aplicação Java 21 com Maven e interface de terminal. Já disponibiliza:

- quiz local sequencial para dois a quatro jogadores;
- servidor TCP com sala única, rodadas sincronizadas e até quatro jogadores;
- protocolo de linhas UTF-8 validado, com timeout controlado pelo servidor;
- cálculo de pontuação, ranking determinístico, cliente humano e clientes simulados;
- execução paralela e benchmark reproduzível;
- imagem Docker e Compose para um servidor e quatro bots.

Não existem API HTTP, interface web, autenticação, perfis de utilizador, base de dados ou persistência de perguntas, resultados ou sessões. As perguntas são lidas de `src/main/resources/perguntas.txt`; o estado de `Jogador` vive apenas em memória.

## Componentes inspeccionados

| Área | Implementação actual | Observação para a evolução |
| --- | --- | --- |
| Domínio | `Pergunta`, `Resposta`, `Jogador` | `Jogador` representa participante de uma partida, não uma conta autenticada. Deve ser preservado nesta função. |
| Jogo TCP | `QuizServer`, `ClienteHandler`, `SalaQuiz`, `Rodada` | O coordenador único da sala é uma boa fronteira de concorrência; não deve ser removido. |
| Protocolo | `ProtocoloMensagem` | Mantém-se para clientes TCP existentes. HTTP/WebSocket serão contratos separados. |
| Serviços | carregamento de perguntas, pontuação, ranking, execução sequencial/paralela | São reutilizáveis como regras de treino e correcção objectiva. |
| Infra-estrutura | Maven, Dockerfile multi-stage, Compose | O Compose actual é demonstrativo; será ampliado quando houver base de dados e API. |

## Testes e evidências

O relatório histórico [VALIDACAO.md](VALIDACAO.md) regista `mvn clean compile`, `mvn test`, Docker Compose e **24 testes aprovados**, sem falhas, erros ou ignorados, em 9 de Outubro de 2026.

Nesta auditoria foi tentado `mvn test`, mas o comando não está instalado no ambiente actual (`mvn: command not found`). Há Java 25 disponível; a linha de base declarada do projecto é JDK 21 e Maven 3.9+. Não se afirma que a suite passou nesta sessão. Maven é o bloqueio efectivo para executar a suite; a validação final deve também ser repetida com JDK 21, a versão alvo do projecto.

## TCP e futura aplicação web

TCP continua adequado ao jogo de terminal e aos clientes simulados. Um navegador não consome directamente este protocolo; a aplicação web deverá comunicar com uma API HTTP para comandos e com WebSocket para eventos da sala. A API chama os serviços de domínio e adapta eventos de `SalaQuiz`; não expõe os sockets TCP ao browser. Assim preservam-se os testes e clientes actuais sem introduzir uma ponte frágil TCP–browser.

## Riscos e decisões pendentes

1. **Dependências:** o `pom.xml` actual contém apenas JUnit. A API e persistência exigem dependências, versionamento e testes de integração deliberados.
2. **Identidade:** uma conta (`Utilizador`) não deve reutilizar ou substituir `Jogador`; uma sessão de avaliação deverá referenciar um utilizador autenticado e criar/adaptar um participante de jogo quando necessário.
3. **Segurança:** palavras-passe requerem hash forte; autorização deve ser feita no backend, não por botões ocultos.
4. **Persistência concorrente:** respostas devem ser gravadas de forma idempotente e associadas a avaliação, pergunta e estudante. O estado activo da sala permanece sob controlo do servidor.
5. **Compatibilidade:** a migração não pode alterar sem necessidade `NOME`, `RESPOSTA` e outros formatos TCP existentes.

## Próxima tarefa concreta

Implementar a fundação de identidade e persistência: configurar uma API Java, criar `Utilizador` e `Perfil`, repositório migrado, criação controlada de utilizadores e autenticação, com testes de validação e autorização. A selecção de tecnologia proposta encontra-se em [03-arquitectura.md](03-arquitectura.md).

Ficheiros previstos para a primeira implementação: `pom.xml`, novo código sob `src/main/java/quizrace/{user,api,repository,security}`, migrações em `src/main/resources/db/migration`, testes em `src/test/java/quizrace`, `Dockerfile`, `docker-compose.yml`, `README.md` e os documentos de arquitectura, dados, API, testes e instalação. As classes actuais de TCP só serão alteradas se uma integração posterior exigir uma adaptação comprovada.
