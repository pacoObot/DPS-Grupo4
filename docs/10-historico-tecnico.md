# Histórico técnico — 21 de Outubro de 2026

1. Inspecção do repositório e separação entre jogo TCP e futura plataforma académica.
2. Utilizador aprovou a arquitectura e pediu instalação de Maven e implementação da fundação.
3. A instalação automática via sudo exigiu palavra-passe; as ferramentas foram descarregadas localmente. Posteriormente o utilizador instalou Maven 3.8.7 e JDK 21 pelo apt.
4. O Java global continuava na versão 25. Os comandos de validação seleccionaram JDK 21 através de JAVA_HOME, sem mudar a configuração global.
5. A suite inicial passou os 24 testes, incluindo sockets TCP.
6. Implementados API, migração, contas, perfis, autenticação, autorização e bootstrap.
7. Detectada em verificação uma dependência dos nomes de parâmetros reflectidos pelo compilador. As anotações HTTP passaram a declarar nomes explicitamente.
8. DTOs de credenciais e token receberam representação de diagnóstico sem segredos.
9. Acrescentados Compose da API, ensaio com PostgreSQL e documentação reproduzível.

Os resultados finais, incluindo quaisquer limitações de validação, são registados em [07-testes.md](07-testes.md). Não foram criados commits automaticamente.
