# Limitações e trabalho futuro

## Entregue nesta fase

Contas persistidas, três perfis, criação e consulta administrativas, mudança de perfil/estado, autenticação Bearer JWT, hash BCrypt, migração Flyway e execução API/PostgreSQL em Docker.

## Ainda fora do âmbito

- Interface web, perguntas persistidas, histórico, avaliações e salas académicas.
- Associação entre conta e jogador TCP; o TCP continua a aceitar nomes para o jogo de treino existente.
- Refresh token, recuperação/troca de palavra-passe, auto-registo e verificação de email.
- Auditoria administrativa detalhada, paginação por curso/turma e gestão institucional.
- Limitação de tentativas de login e mecanismos anti-abuso distribuídos.
- HTTPS/reverse proxy, backups automáticos e gestão de segredos para produção.
- Validação de carga da API e das futuras submissões concorrentes.

A API é uma fundação demonstrável em localhost. A publicação externa exige resolver HTTPS, protecção contra tentativas repetidas de login, backups e revisão de suporte/actualizações das dependências. O segredo JWT deve ser aleatório e partilhado entre instâncias; mudar o segredo invalida tokens existentes.

O frontend da Fase 3 deve guardar tokens em memória, limpar a identidade ao receber 401 e tratar 403 sem tentar elevar privilégios. A utilização futura de cookies exige rever CSRF e a política de sessão.
