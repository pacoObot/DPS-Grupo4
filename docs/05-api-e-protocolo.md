# API implementada — v1

Base: `http://localhost:8080/api/v1`. JSON UTF-8. Rotas protegidas recebem `Authorization: Bearer <accessToken>`. O protocolo TCP do README mantém-se separado e não autentica contas académicas.

| Método / rota | Permissão | Resultado |
| --- | --- | --- |
| POST /auth/login | público | 200, token e identidade |
| GET /auth/me | conta activa autenticada | 200, identidade actual |
| POST /users | administrador | 201 e Location |
| GET /users?page=0&size=20 | administrador | 200, página (size 1–100) |
| GET /users/{id} | administrador | 200, utilizador |
| PATCH /users/{id} | administrador | 200, estado/perfil actualizados |

## Login

```json
{"email":"admin@example.org","palavraPasse":"a-palavra-passe-configurada"}
```

Resposta:
```json
{
  "accessToken": "<token>",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "utilizador": {
    "id": "<uuid>",
    "nome": "Administrador",
    "email": "admin@example.org",
    "perfil": "ADMINISTRADOR",
    "activo": true,
    "criadoEm": "<data ISO-8601>"
  }
}
```

Login e identidade usam `Cache-Control: no-store`. Não há refresh token; após expirar, repetir login.

## Criar utilizador

```json
{"nome":"Ana Silva","email":"ana@example.org","palavraPasse":"exemplo-com-12-ou-mais","perfil":"ESTUDANTE"}
```

Nome: 2–100 caracteres úteis; email válido até 254 caracteres e normalizado; palavra-passe de 12–72 caracteres, limitada também a 72 bytes UTF-8; perfil obrigatório. Não usar os valores de exemplo como credenciais reais. Resposta contém os mesmos campos públicos de `utilizador` no login, sem token.

## Alterar perfil ou estado

```json
{"perfil":"DOCENTE","activo":true}
```

Pelo menos um dos campos é obrigatório. Campos desconhecidos são rejeitados. Alterar invalida os tokens anteriores; o utilizador deve autenticar-se novamente. Não é permitido desactivar ou despromover o último administrador activo.

## Lista

```json
{"content":[],"page":0,"size":20,"totalElements":0}
```

Ordenação por email. Estudantes e docentes não podem consultar outras contas através destas rotas.

## Erros

Formato: `{"codigo":"DADOS_INVALIDOS","mensagem":"Verifique os dados enviados."}`.

| Estado | Situação |
| --- | --- |
| 400 | dados, UUID, perfil, paginação ou JSON inválidos |
| 401 | login inválido, conta inactiva, token inválido/expirado/revogado ou ausente |
| 403 | perfil sem permissão |
| 404 | utilizador inexistente |
| 409 | email duplicado ou tentativa de remover último administrador |

O login não distingue email inexistente de palavra-passe errada. Não se devolvem detalhes SQL ou stack traces. Endpoints não previstos são recusados por defeito.
