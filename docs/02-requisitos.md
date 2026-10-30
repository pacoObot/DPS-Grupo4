# Requisitos iniciais

## Funcionais — fundação de utilizadores

- RF-01: o sistema deve manter utilizadores com identificador, nome, email único, estado activo, perfil e data de criação.
- RF-02: apenas um administrador pode criar, activar, desactivar ou alterar o perfil de uma conta.
- RF-03: um utilizador activo deve poder autenticar-se com email e palavra-passe; credenciais inválidas não revelam qual o campo incorrecto.
- RF-04: a API deve devolver a identidade e o perfil do utilizador autenticado.
- RF-05: operações futuras de docente e estudante devem receber a identidade autenticada do backend, não um perfil enviado livremente pelo cliente.

## Funcionais — evolução posterior

- RF-06: docentes criam avaliações e salas com regras e ciclo de vida explícito.
- RF-07: estudantes autorizados entram por código válido e submetem respostas.
- RF-08: o servidor controla prazos, submissões e correcção de questões objectivas.
- RF-09: o sistema guarda resultados e histórico de preparação.

## Não funcionais

- RNF-01: manter compatibilidade do jogo TCP existente e dos seus testes.
- RNF-02: nenhuma palavra-passe é armazenada em texto simples ou registada em logs.
- RNF-03: persistência local para desenvolvimento e banco relacional em Compose para execução integrada.
- RNF-04: erros de validação têm mensagens seguras e estados HTTP coerentes.
- RNF-05: alterações de esquema são reproduzíveis por migrações versionadas.

## Critérios de aceitação da primeira implementação

1. Há pelo menos os perfis `ADMINISTRADOR`, `DOCENTE` e `ESTUDANTE` no domínio e no armazenamento.
2. A criação valida nome, email, perfil e palavra-passe; emails duplicados são rejeitados.
3. A palavra-passe persistida é um hash BCrypt, nunca o valor recebido.
4. Um endpoint de autenticação devolve credencial de sessão segura e um endpoint protegido identifica o utilizador actual.
5. Um estudante não pode executar operação administrativa, mesmo que invoque o endpoint manualmente.
6. Testes cobrem validação, duplicação, autenticação e autorização; a suite TCP continua sem regressões.
