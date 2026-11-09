# Plano e entrega — actualizado em 9 de Novembro de 2026

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

## Fase 3 entregue

Fase 3 implementada: aplicação React/TypeScript com login, identidade actual, navegação por perfil e gestão de utilizadores para administrador. Consome o contrato real de [05-api-e-protocolo.md](05-api-e-protocolo.md). Inclui tratamento de 401/403/409 e expiração de tokens, sem decidir permissões apenas no frontend.

## Continuação faseada aprovada em 2 de Novembro

| Fase | Entrega | Estado / critérios |
| --- | --- | --- |
| 4A | Preparação de testes pelo docente | Implementada: título, instruções, até 50 perguntas A–D, gabarito, categoria, ordenação, pré-visualização e histórico; acesso limitado ao dono |
| 4B | Banco reutilizável de perguntas e treino autónomo | Adiado; não é requisito para publicar a primeira sala |
| 5 | Personalizar sala e disponibilizar URL | Implementada: teste versionado, sala nomeada, limite por pergunta e link próprio; participante pede entrada com nome ou número académico |
| 6 | Resolver o teste e acompanhar em tempo real | Implementada: escolha múltipla e verdadeiro/falso, temporizador no servidor, validação docente, progresso, presença, dispositivo aproximado e cursor/toque; actualização periódica a cada 2 segundos |
| 7 | Encerramento, notas e impressão | Implementada: docente encerra; resultados finais e acertos no monitor; impressão pelo navegador |
| 8 | Integração e carga | Em evolução: suite cobre autorização, respostas, duplicados, expiração, persistência e erros; reconexão e carga distribuída ficam por validar |
| 9 | Revisão e entrega | Demonstração funcional preparada; executar percurso manual na apresentação e registar observações |

### Decisões para as fases seguintes

- A entrada pela URL não deve exigir a conta académica com email/palavra-passe usada pelo docente. O servidor emitirá uma sessão de participante restrita à sala após validação.
- Nome ou número académico identifica o participante, mas não comprova sozinho a identidade. Na fase 5 será definida a regra de admissão (lista autorizada, aprovação pelo docente ou outra regra acordada), duplicação e reconexão. Não assumir uma integração institucional já existente.
- Uma sala usará uma cópia/versionamento do teste publicado: editar o rascunho depois não deve alterar perguntas de uma sessão em curso nem as suas notas.
- O docente controla o encerramento da sessão. A progressão das perguntas (individual ou comandada pelo docente), os limites de tempo e a escala de notas serão definidos antes de implementar as fases 6–7. As regras do jogo TCP não são automaticamente a política de avaliação académica.
- O acompanhamento é visível para os estudantes e limitado à aplicação. Cursor significa posição dentro da página do teste, não acesso ao ambiente de trabalho. Telemóveis enviam eventos de toque; a classificação celular/PC é aproximada e não serve como prova de identidade ou fraude.
- O painel recebe eventos autorizados por sala. Não transmite gabaritos aos participantes; limita a frequência de cursor/toque para não sobrecarregar a rede.

As fases 5–7 estão implementadas na aplicação web; a documentação e o roteiro de apresentação estão em [14-preparacao-testes.md](14-preparacao-testes.md). A fase 4B (banco reutilizável autónomo de perguntas) continua adiada; o treino usa testes que o docente marcou como disponíveis.
