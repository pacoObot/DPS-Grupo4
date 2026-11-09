# Limitações e trabalho futuro

## Entregue nesta fase

Contas persistidas, três perfis, gestão administrativa, autenticação JWT e PostgreSQL. A aplicação web também permite ao docente preparar e versionar testes de escolha múltipla/verdadeiro-falso, criar salas com URL, validar participantes, acompanhar o progresso e cursor/toque, encerrar e imprimir resultados. Os estudantes podem fazer quizzes de treino e consultar histórico.

## Ainda fora do âmbito

- Verificação automática do número de registo junto de um sistema académico externo.
- WebSockets/eventos de baixa latência, reconexão de estudante entre dispositivos e reentrada autenticada.
- Banco autónomo de perguntas que possa ser reutilizado entre testes.
- Associação entre conta e jogador TCP; o TCP continua a aceitar nomes para o jogo de treino existente.
- Refresh token, recuperação/troca de palavra-passe, auto-registo e verificação de email.
- Auditoria administrativa detalhada, paginação por curso/turma e gestão institucional.
- Limitação de tentativas de login e mecanismos anti-abuso distribuídos.
- HTTPS, backups automáticos e gestão de segredos para produção.
- Testes de carga e validação simultânea com turmas reais.

A API é uma fundação demonstrável em localhost. A publicação externa exige resolver HTTPS, protecção contra tentativas repetidas de login, backups e revisão de suporte/actualizações das dependências. O segredo JWT deve ser aleatório e partilhado entre instâncias; mudar o segredo invalida tokens existentes.

O frontend guarda tokens em memória, limpa a identidade ao receber 401 e trata 403 sem tentar elevar privilégios. Recarregar a página exige novo login. O logout local elimina o token do navegador, mas não o revoga no servidor; a validade máxima é de 15 minutos. O Nginx encaminha as chamadas à API na mesma origem. A utilização futura de cookies exige rever CSRF e a política de sessão.
