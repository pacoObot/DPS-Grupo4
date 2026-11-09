# Instalação e execução — passo a passo

## 1. Ferramentas

Em Ubuntu/Pop!_OS:

```bash
sudo apt update
sudo apt install -y maven openjdk-21-jdk
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
mvn -version
java -version
javac -version
```

Os três comandos devem indicar Java 21. Estas exportações afectam apenas o terminal actual e não alteram a configuração global. Nesta máquina Maven 3.8.7 funciona; Docker usa Maven 3.9.9. A instalação local anterior em `.tools/` é opcional e ignorada pelo Git.

Entrar na pasta do projecto antes dos comandos seguintes.

## 2. Construir e testar

```bash
mvn clean verify
```

Produz `target/quizrace.jar` (terminal/TCP) e `target/quizrace-api.jar` (HTTP). A suite Maven usa H2 em memória para integração da API; não é preciso iniciar PostgreSQL para estes testes.

## 3. Configurar API e PostgreSQL

```bash
cp .env.example .env
openssl rand -hex 32
```

Preencher `.env` com uma palavra-passe forte para a base de dados, o segredo aleatório gerado e o email/palavra-passe do administrador inicial. A palavra-passe de conta tem pelo menos 12 caracteres e no máximo 72 bytes UTF-8. O ficheiro é ignorado por Git e Docker.

Não existe conta predefinida. As variáveis de administrador são utilizadas apenas quando a tabela está vazia; depois do primeiro arranque podem ser removidas de `.env`. Não funcionam como recuperação ou alteração de palavra-passe.

## 4. Iniciar

```bash
docker compose -f compose.api.yml up --build -d
docker compose -f compose.api.yml logs -f api
```

A API fica em `http://localhost:8080/api/v1`. Pode alterar `QUIZ_API_PORT` no `.env` se a porta estiver ocupada. O PostgreSQL não publica uma porta no host; comunica com a API pela rede Compose.

Para encerrar mantendo as contas:

```bash
docker compose -f compose.api.yml down
```

Para reiniciar e confirmar persistência:

```bash
docker compose -f compose.api.yml restart
```

O volume nomeado mantém os dados entre reinícios. Não usar `down -v` na instalação normal: elimina a base de dados.

## 5. Demonstração HTTP

Com `curl` e `jq`, recolher credenciais sem escrever a palavra-passe no histórico:

```bash
read -r -p "Email: " QR_EMAIL
read -r -s -p "Palavra-passe: " QR_PASSWORD
QR_TOKEN=$(jq -nc --arg email "$QR_EMAIL" --arg password "$QR_PASSWORD" \
  '{email:$email,palavraPasse:$password}' |
  curl -fsS http://localhost:8080/api/v1/auth/login \
    -H 'Content-Type: application/json' --data-binary @- | jq -er '.accessToken')
unset QR_PASSWORD
curl -fsS http://localhost:8080/api/v1/auth/me -H "Authorization: Bearer $QR_TOKEN"
curl -fsS http://localhost:8080/api/v1/users -H "Authorization: Bearer $QR_TOKEN"
```

Criar um estudante com o token do administrador:

```bash
read -r -s -p "Palavra-passe do estudante: " QR_PASSWORD
jq -nc --arg password "$QR_PASSWORD" \
  '{nome:"Ana Silva",email:"ana@example.org",palavraPasse:$password,perfil:"ESTUDANTE"}' |
  curl -fsS http://localhost:8080/api/v1/users \
    -H "Authorization: Bearer $QR_TOKEN" -H 'Content-Type: application/json' --data-binary @-
unset QR_PASSWORD
```

Repetir o login com a conta do estudante: `/auth/me` responde 200, mas `/users` responde 403. O contrato completo está em [05-api-e-protocolo.md](05-api-e-protocolo.md).

## 6. Ensaio automatizado da entrega

```bash
bash scripts/verificar-api.sh
```

Requer Docker Compose, curl, jq e openssl. Cria um projecto Docker temporário, usa credenciais aleatórias, executa o build com testes, verifica login, gravação real e recuperação após reinício. Usa a porta 19087 (alterável por `QUIZ_TEST_PORT`). No fim remove apenas containers, rede e volume desse ensaio; a imagem construída permanece. Não usa a base de dados da instalação normal.

## Execução Java fora do Docker

Com um PostgreSQL acessível e as variáveis `QUIZ_DB_URL`, `QUIZ_DB_USER`, `QUIZ_DB_PASSWORD`, `QUIZ_JWT_SECRET` e, no primeiro arranque, `QUIZ_ADMIN_EMAIL`/`QUIZ_ADMIN_PASSWORD` exportadas:

```bash
java -jar target/quizrace-api.jar
```

O ficheiro `.env` é lido pelo Compose, não automaticamente pelo Java. Para o jogo de terminal, continuam válidos `mvn exec:java` e os comandos TCP do README.

## Interface web e acesso dos estudantes

Com o mesmo `.env`, iniciar os três serviços:

```bash
docker compose -f compose.api.yml -f compose.web.yml up --build -d
```

Abra http://localhost:3000. `QUIZ_WEB_PORT` altera esta porta. Entre com a conta docente já criada (ou use a conta administrativa para criar docentes/estudantes). O docente cria testes em **Os meus testes**, abre uma sala e partilha o link; o estudante abre o endereço, informa nome ou número académico e aguarda aprovação. O monitor mostra presença e progresso e permite encerrar, ver notas e imprimir a lista. Estudantes autenticados também podem praticar com os quizzes disponibilizados.

Para encerrar mantendo os dados:

```bash
docker compose -f compose.api.yml -f compose.web.yml down
```

Por omissão, a interface fica acessível na rede local. Descubra o IP do computador docente e confirme que o firewall permite a porta 3000 na rede privada:

```bash
docker compose -f compose.api.yml -f compose.web.yml up --build -d
hostname -I
```

Abra `http://IP-LOCAL:3000` no computador docente e nos dispositivos estudantes; crie a sala a partir do endereço com IP para o link partilhado ser acessível. Exemplo: `http://192.168.1.20:3000/sala/CODIGO`. Para restringir novamente a localhost, configure `QUIZ_WEB_BIND=127.0.0.1` no ambiente do Compose. A exposição destina-se à rede privada local; não encaminhe a porta para a Internet. Consulte [13-interface-web.md](13-interface-web.md) e o [roteiro da demonstração](14-preparacao-testes.md).
