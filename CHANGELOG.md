# Histórico de alterações

## 2026-10-21 — Fundação de identidade e persistência

- API Spring Boot 3.5.16 e JAR independente, mantendo o executável TCP.
- Utilizadores persistidos com os perfis administrador, docente e estudante.
- Migração Flyway, PostgreSQL, email único e protecção do último administrador.
- Autenticação JWT de 15 minutos, BCrypt e revogação após alteração de estado/perfil.
- Bootstrap administrativo por configuração, sem conta fixa.
- Compose específico da API, testes de integração e ensaio de reinício do banco.
- Documentação de arquitectura, dados, API, execução e limitações.

O histórico anterior está em `git log` e `docs/VALIDACAO.md`; não foi reconstruído artificialmente.
