# Interface web — Fase 3

A aplicação React/TypeScript comunica com a API existente. Não substitui o jogo TCP nem apresenta avaliações que ainda não foram implementadas.

## Executar

Com Docker, configurar o `.env` seguindo [a instalação](08-instalacao-e-execucao.md), depois:

```bash
docker compose -f compose.api.yml -f compose.web.yml up --build -d
```

Abrir http://localhost:3000. O Nginx entrega a interface e encaminha `/api/` para a API. A API pode demorar alguns segundos a arrancar; se o login indicar indisponibilidade, aguardar e tentar novamente.

Para desenvolvimento, com Node.js 22.12+ (recomendado 24) e a API em execução:

```bash
cd web
npm ci
npm run dev
```

Vite escuta em localhost:5173, escolhendo outra porta se esta estiver ocupada. O terminal mostra o endereço efectivo. Por padrão, encaminha `/api` para localhost:8080. Outra API pode ser indicada por `QUIZ_API_PROXY=http://127.0.0.1:18087 npm run dev`. A variável configura o proxy no servidor de desenvolvimento; não contém credenciais.

```bash
npm run build
npm run format:check
```

O build verifica TypeScript e produz `web/dist/`. `npm run preview` serve apenas os ficheiros compilados para inspecção visual; para validar login use o desenvolvimento com proxy ou Nginx/Compose. O lockfile fixa as versões instaladas por `npm ci`.

## Responsabilidades

```text
web/
├── src/
│   ├── main.tsx                    # Entrada React
│   ├── App.tsx                     # Sessão, navegação, visão geral e conta
│   ├── api.ts                      # Contratos TypeScript e chamadas HTTP
│   ├── components/
│   │   ├── Logo.tsx                # Identidade visual comum
│   │   ├── Login.tsx               # Formulário de autenticação
│   │   ├── Utilizadores.tsx        # Lista paginada e actualização
│   │   └── Formulario.tsx          # Criação e alteração de conta
│   └── style.css                  # Estilos e adaptação a telemóvel
├── public/favicon.svg
├── vite.config.ts                 # Proxy de desenvolvimento
├── nginx.conf                     # Servidor e proxy da imagem Docker
├── package.json
└── package-lock.json
```

O administrador cria contas e altera perfil/estado. Docentes têm também **Os meus testes**, com editor e histórico descritos no [guia de preparação](14-preparacao-testes.md). Estudantes têm a visão geral e os dados da própria conta. O servidor valida sempre as permissões; ocultar um botão no navegador não concede nem retira privilégios na API.

O JWT fica apenas em memória. Ao sair, recarregar, expirar o prazo ou receber 401, a interface volta ao login. Ao recuperar o foco da janela, consulta a identidade para detectar revogação. Um 403 apresenta erro de permissão; um 409 apresenta o conflito informado pelo servidor, por exemplo email duplicado ou último administrador activo. Alterar a própria conta termina a sessão. O logout é local: o token continua válido no servidor até expirar ou ser revogado por alteração de conta.

## Roteiro de demonstração

1. Entrar com o administrador criado no bootstrap.
2. Abrir **Utilizadores**, criar uma conta de estudante e verificar a lista.
3. Editar essa conta, escolher **Docente** e guardar.
4. Tentar desactivar o último administrador: a aplicação deve apresentar o erro e manter a conta activa.
5. Sair e entrar como docente: consultar **A minha conta**, sem menu de administração.
6. Recarregar a página: deve ser necessário entrar novamente.
7. Repetir com janela estreita e verificar que os controlos continuam acessíveis.

Os resultados observados e os limites da validação estão em [07-testes.md](07-testes.md). Perguntas e testes em rascunho estão disponíveis na Fase 4A. Salas, avaliações em tempo real e resultados ficam para as fases seguintes.
