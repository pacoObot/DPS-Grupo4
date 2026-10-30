# Evidências de testes — 21 de Outubro de 2026

## Ambiente

Host Pop!_OS/Linux, Maven 3.8.7, JDK 21.0.12.1 seleccionado por `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`. Docker Engine 29.9.1. Build Docker: Maven 3.9.9 e Temurin 21.

## Linha de base

Comando: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -B -ntp test`.

Resultado antes da alteração do backend: **24 testes, zero falhas, zero erros, zero ignorados; BUILD SUCCESS**. Inclui integração TCP em sockets locais.

## Fundação do backend

Comando: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -B -ntp clean verify`.

Resultado: **36 testes, zero falhas, zero erros, zero ignorados; BUILD SUCCESS**.

| Suite | Testes | Âmbito |
| --- | ---: | --- |
| JogoConcorrenteTest | 7 | concorrência, timeout, sala e validação |
| TcpIntegrationTest | 1 | partida real em sockets |
| QuizRaceTest | 7 | domínio, pontuação e modo sequencial |
| ProtocoloMensagemTest | 5 | mensagens TCP |
| BenchmarkTest | 3 | equivalência e medição |
| ClienteSimuladoTest | 1 | configuração do bot |
| UtilizadoresApiTest | 12 | API, persistência H2, autenticação e autorização |

Os testes novos cobrem criação e leitura do hash, email duplicado com maiúsculas, estudante/docente sem privilégios administrativos, tokens ausentes/adulterados/expirados/com emissor incorrecto, desactivação/reactivação, alteração de perfil, último administrador, validação e paginação, resposta de login uniforme e ausência de segredos na representação de DTOs.

Foram gerados `target/quizrace.jar` e `target/quizrace-api.jar`. H2 aplica a migração Flyway e Hibernate valida o esquema. O mesmo build dentro de Docker passou também os **36 testes**. O JAR CLI foi executado com a opção 0: apresentou o menu e terminou com código zero.

## PostgreSQL e HTTP real

Comando: `bash scripts/verificar-api.sh`.

Resultado final: **código de saída 0**. Evidências observadas:

- `PASS: API HTTP, login, autorização e gravação em PostgreSQL.`
- `PASS: dados e login preservados após reinício do PostgreSQL e da API; bootstrap não duplicou contas.`

O ensaio criou administrador e estudante, confirmou duas contas por SQL, reiniciou PostgreSQL e API, voltou a autenticar-se e consultou o mesmo estudante por UUID. Os containers, rede e volume exclusivos do ensaio foram removidos; as imagens permanecem. A instalação normal do utilizador e os outros containers do host não foram alterados.

## Falhas encontradas e tratadas

- Maven inicialmente ausente; instalado pelo utilizador. Java global era 25; comandos seleccionam JDK 21.
- Sandbox impediu dependências e sockets; execução repetida com autorização, sem ignorar testes.
- Uma repetição revelou nomes de parâmetros HTTP ausentes nos metadados de compilação. Corrigido com nomes explícitos nas anotações e configuração de compilação.
- O primeiro ensaio Docker tentou login antes de o bootstrap terminar. O script passou a aguardar login bem-sucedido, com limite de tentativas.
- DTOs de credenciais e token passaram a ocultar dados em `toString`, evitando exposição em diagnóstico normal.

## Limites

Esta validação não é um teste de carga da API nem uma revisão completa de segurança para produção. Avaliações ainda não se aplicam. A validação da interface encontra-se abaixo. As limitações estão em [12-limitacoes-e-trabalho-futuro.md](12-limitacoes-e-trabalho-futuro.md).

## Continuação — 30 de Outubro de 2026

Antes da Fase 3, `mvn clean verify` voltou a passar: **36 testes**, sem falhas, erros ou testes ignorados, usando JDK 21.0.12.1 e Maven 3.9.9 locais. A execução precisou de permissão para sockets e instrumentação Mockito; a tentativa limitada pelo sandbox não constitui falha da aplicação.

Frontend: `npm run build` concluiu a verificação TypeScript e o build Vite. Ensaio Playwright no navegador com Vite e API real em porta 18087, usando H2 efémero separado das contas persistidas:

- Login administrativo, credenciais erradas com mensagem adequada e consulta da identidade.
- Criação de estudante e apresentação na tabela; email duplicado rejeitado.
- Alteração para docente e novo login com navegação sem gestão administrativa.
- Tentativa de desactivar o último administrador rejeitada com mensagem da API.
- Consulta da conta e logout; recarregar a página exige novo login.
- `localStorage` e `sessionStorage` vazios após autenticação.
- Vista de 390 px sem transbordamento horizontal.

Este ensaio de navegador não é uma suite automatizada versionada nem substitui o ensaio PostgreSQL descrito acima. Não foi feita medição de carga nem aguardada a expiração real de 15 minutos nesta sessão.

A imagem `quizrace-web:validacao` também foi construída com sucesso usando `Dockerfile.web`. A configuração Nginx passou `nginx -t` num container temporário. A composição API + web passou `docker compose ... config --quiet`; a stack completa PostgreSQL/API/Nginx não foi iniciada neste ensaio da interface.
