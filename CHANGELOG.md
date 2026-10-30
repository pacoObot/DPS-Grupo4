# Histórico de alterações

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
