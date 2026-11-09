# Histórico de alterações

## 2026-11-09 — Demonstração com 20 perguntas

- Quiz de apresentação actualizado para 20 perguntas de escolha múltipla e verdadeiro/falso, disponível também para prática.
- Seis contas sintéticas de estudante preparadas para a apresentação.
- Na sala de avaliação, tocar ou clicar numa alternativa envia a resposta imediatamente e abre a pergunta seguinte.
- Roteiro de apresentação actualizado com as contas e o fluxo de resposta automática.

## 2026-11-09 — Salas de avaliação e quizzes de preparação

- Testes docentes suportam escolha múltipla e verdadeiro/falso e podem ser disponibilizados como quizzes de treino.
- Salas com URL própria, pedido de entrada pelo estudante e validação da docente.
- Perguntas apresentadas por etapas com temporizador controlado no servidor e respostas sem exposição do gabarito.
- Monitor periódico com presença, dispositivo aproximado, progresso e cursor/toque; encerramento, notas e impressão.
- Histórico de tentativas de treino; documentação do percurso e roteiro de apresentação actualizados.
- Suite completa com 48 testes; build da interface verificado.

## 2026-11-02 — Preparação de testes pelo docente (Fase 4A)

- Rascunhos com título, instruções e até 50 perguntas A–D, resposta correcta e categoria.
- Acesso aos testes e gabaritos limitado ao docente dono, aplicado na API.
- Gravação transaccional, histórico de versões e conflitos de edição protegidos por versão optimista.
- Editor com ordenação, remoção, pré-visualização e consulta do histórico.
- Plano actualizado para sala por URL, identificação académica, acompanhamento e impressão de notas.

## 2026-10-30 — Interface académica (Fase 3)

- React/TypeScript com login, área pessoal e navegação por perfil.
- Lista paginada, criação e alteração de perfil/estado de utilizadores.
- Sessão apenas em memória, expiração e mensagens de erro da API.
- Layout responsivo, proxy Vite e imagem Nginx com Compose complementar.
- Build verificado e ensaio funcional no navegador com API real e H2 temporário.

## 2026-10-21 — Fundação de identidade e persistência

- API Spring Boot 3.5.16 e JAR independente, mantendo o executável TCP.
- Utilizadores persistidos com os perfis administrador, docente e estudante.
- Migração Flyway, PostgreSQL, email único e protecção do último administrador.
- Autenticação JWT de 15 minutos, BCrypt e revogação após alteração de estado/perfil.
- Bootstrap administrativo por configuração, sem conta fixa.
- Compose específico da API, testes de integração e ensaio de reinício do banco.
- Documentação de arquitectura, dados, API, execução e limitações.

O histórico anterior está em `git log` e `docs/VALIDACAO.md`; não foi reconstruído artificialmente.
