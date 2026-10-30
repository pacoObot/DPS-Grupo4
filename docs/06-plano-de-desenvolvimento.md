# Plano e entrega — 21 de Outubro de 2026

A Fase 0 identificou o jogo TCP existente. A Fase 1 definiu a arquitectura, aprovada pelo utilizador antes da implementação. A Fase 2 entrega a fundação de utilizadores e persistência.

| ID | Entrega | Dependência | Critério e evidência | Estado |
| --- | --- | --- | --- | --- |
| BE-01 | Maven/JDK 21, Spring Boot, build separado | aprovação da arquitectura | compilação limpa e dois JARs; suite TCP preservada | implementado |
| BE-02 | Utilizador, perfis e migração Flyway | BE-01 | persistência, email único e esquema validado | implementado |
| BE-03 | Serviço, DTOs e API de gestão | BE-02 | validação, criação, consulta e alteração testadas | implementado |
| BE-04 | Autenticação e autorização | BE-03 | login, privilégios, revogação e último administrador testados | implementado |
| BE-05 | Compose, PostgreSQL e documentação | BE-01–04 | ensaio HTTP e persistência após reinício, código 0 | implementado e validado |

Responsável de implementação: Codex; validação funcional e apresentação: equipa QuizRace. Prioridade: alta para BE-01 a BE-05. Os testes e as evidências observadas estão em [07-testes.md](07-testes.md); as instruções reproduzíveis em [08-instalacao-e-execucao.md](08-instalacao-e-execucao.md).

Entrega técnica da Fase 2 validada: 36 testes aprovados no host e no build Docker, mais ensaio HTTP/PostgreSQL com recuperação após reinício. Está pronta para servir de base à Fase 3; a configuração da instalação normal requer preencher o ficheiro local `.env`.

## Próxima fase concreta

Fase 3: aplicação React/TypeScript com login, identidade actual, navegação por perfil e gestão de utilizadores para administrador. Consumir o contrato real de [05-api-e-protocolo.md](05-api-e-protocolo.md). Prever tratamento de 401/403/409 e expiração de tokens, sem decidir permissões apenas no frontend.

| Fase | Entrega | Dependência |
| --- | --- | --- |
| 3 | frontend e áreas por perfil | API de utilizadores |
| 4 | preparação, perguntas, quizzes e histórico | identidade persistida |
| 5 | avaliações, salas, código e ciclo de vida docente | autorização e modelo académico |
| 6 | respostas, temporizador e painel em tempo real | salas e contratos de eventos |
| 7 | correcção, estatísticas e exportação | respostas persistidas |
| 8 | integração completa e testes de carga | componentes anteriores |
| 9 | revisão de segurança, entrega e demonstração | validações de todas as fases |

Nesta fase não se entregam avaliações, associação entre conta e jogador TCP, histórico de partidas, frontend ou eventos de sala.
