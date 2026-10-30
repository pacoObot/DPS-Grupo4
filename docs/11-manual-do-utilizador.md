# Manual da fundação de utilizadores

Esta entrega disponibiliza uma API; a interface web será criada na fase seguinte.

## Administrador

O responsável da instalação configura a primeira conta através de variáveis de ambiente. Autentique-se em `POST /api/v1/auth/login`, use o token devolvido e crie contas em `POST /api/v1/users`. Consulte a lista com `GET /api/v1/users` e altere perfil/estado através de `PATCH /api/v1/users/{id}`.

Uma conta desactivada deixa de conseguir entrar. Uma alteração administrativa exige novo login da conta afectada. O sistema impede remover o último administrador activo.

## Docente e estudante

Recebem a conta criada pelo administrador e podem autenticar-se e consultar os próprios dados em `GET /api/v1/auth/me`. Ainda não existe criação de avaliações ou área de preparação ligada à conta.

## Sessão

O token dura 15 minutos. Ao receber 401, volte a autenticar-se. Uma resposta 403 significa que a conta não tem permissão para a operação. Não partilhe o token nem a palavra-passe.

Exemplos executáveis estão no [guia de instalação](08-instalacao-e-execucao.md) e o formato dos pedidos no [contrato API](05-api-e-protocolo.md).
