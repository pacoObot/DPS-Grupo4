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

## Preparação de testes — Fase 4A

Todas as rotas abaixo exigem `DOCENTE`, filtram pelo UUID autenticado e devolvem `Cache-Control: no-store`. Outro docente recebe 404; estudante/administrador recebe 403. Nenhum endpoint público entrega perguntas ou gabarito nesta fase.

| Método / rota | Resultado |
| --- | --- |
| GET /testes?page=0&size=10 | Resumos dos testes do docente, mais recentes primeiro; size 1–50 |
| POST /testes | 201, detalhe e Location; sem campo versao |
| GET /testes/{id} | Detalhe completo do rascunho |
| PUT /testes/{id} | Substitui conteúdo/ordem, exige a versao recebida na leitura |
| GET /testes/{id}/historico?page=0&size=10 | Versões e datas, da mais recente para a mais antiga |
| GET /testes/{id}/historico/{versao} | Cópia histórica completa, apenas de leitura |

Exemplo de criação:

```json
{
  "titulo": "Teste 1 — Java",
  "descricao": "Seleccione uma alternativa por pergunta.",
  "perguntas": [{
    "enunciado": "Qual linguagem usamos no projecto?",
    "alternativaA": "Java", "alternativaB": "Python",
    "alternativaC": "C", "alternativaD": "Rust",
    "correcta": "A", "categoria": "DPS"
  }]
}
```

Título: 1–150 caracteres não vazios; descrição opcional até 2000. De 0 a 50 perguntas, enunciado não vazio até 2000, alternativas não vazias até 500, categoria opcional até 100. `correcta` aceita A/B/C/D. Valores textuais são aparados nas extremidades. O dono é sempre obtido do token; campos desconhecidos são rejeitados.

O detalhe acrescenta `id`, `versao`, `criadoEm` e `actualizadoEm`. A versão de API começa em 0; a interface apresenta-a a partir de 1. No PUT enviar apenas título, descrição, perguntas e versão, não o DTO inteiro. Versão ausente é 400; versão desactualizada ou conflito concorrente é 409. A gravação devolve a nova versão. As páginas seguem `{content,page,size,totalElements}`. As cópias históricas conservam também as respostas correctas e pertencem ao mesmo docente.
