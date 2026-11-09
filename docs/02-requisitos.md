# Requisitos iniciais

## Funcionais — fundação de utilizadores

- RF-01: o sistema deve manter utilizadores com identificador, nome, email único, estado activo, perfil e data de criação.
- RF-02: apenas um administrador pode criar, activar, desactivar ou alterar o perfil de uma conta.
- RF-03: um utilizador activo deve poder autenticar-se com email e palavra-passe; credenciais inválidas não revelam qual o campo incorrecto.
- RF-04: a API deve devolver a identidade e o perfil do utilizador autenticado.
- RF-05: operações de docente usam a identidade autenticada do backend. Na futura entrada por URL, o servidor valida a admissão e emite uma sessão limitada ao participante/sala; nunca aceita um perfil livremente enviado pelo cliente.

## Funcionais — salas e preparação

- RF-06: docentes criam testes com questões de escolha múltipla (A–D) ou verdadeiro/falso, instruções e categoria opcional.
- RF-07: docentes criam salas a partir de um teste guardado e partilham uma URL própria; a sala conserva a versão seleccionada.
- RF-08: estudantes abrem a URL, indicam nome ou número de registo académico e aguardam aprovação da docente antes de ver perguntas.
- RF-09: o servidor apresenta perguntas uma a uma, aplica o limite de tempo, regista respostas e não expõe o gabarito durante o teste.
- RF-10: o monitor docente actualiza a presença, estado e progresso dos estudantes, dispositivo aproximado e cursor/toque comunicado na página.
- RF-11: a docente pode recusar entradas e encerrar a sala; após encerrar, consulta e imprime resultados finais.
- RF-12: docentes podem marcar testes para treino; estudantes autenticados fazem quizzes, recebem feedback e consultam tentativas anteriores.

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

## Preparação docente — implementada

- RF-13: apenas o docente dono cria, lista, consulta e edita os seus testes.
- RF-14: cada teste contém título, instruções opcionais e até 50 perguntas ordenadas; rascunhos vazios podem ser guardados.
- RF-15: cada pergunta tem enunciado, tipo, opções próprias e resposta correcta; categoria é opcional.
- RF-16: cada gravação conserva uma cópia histórica; edição com versão antiga recebe 409.
- RF-17: o docente pode pré-visualizar o teste e consultar versões anteriores sem as alterar.

## Regras e fronteiras da demonstração

- A docente valida cada pedido de entrada; o sistema não consulta um registo académico externo.
- O dispositivo é estimado pelo user agent; cursor/toque é limitado à página do teste e explicitado ao estudante.
- As notas são percentagens de respostas correctas e só ficam visíveis ao docente depois de encerrar a sala.
- O monitor actualiza por consulta periódica; não existe transmissão WebSocket nesta fase.
