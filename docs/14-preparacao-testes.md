# Demonstração funcional — docente e estudantes

Este guia descreve o fluxo pronto para demonstrar em sala: preparar perguntas, abrir uma sala, partilhar a URL, validar participantes, acompanhar o teste, encerrar e consultar/imprimir as notas. O sistema aceita perguntas de escolha múltipla e verdadeiro/falso. O docente também pode disponibilizar um teste como quiz de treino.

## Preparar e iniciar

1. Inicie a aplicação conforme [instalação e execução](08-instalacao-e-execucao.md) e abra http://localhost:3000.
2. Entre com uma conta de **Docente**. O administrador pode criar contas em **Utilizadores**.
3. Em **Os meus testes**, crie um teste e adicione perguntas.
   - Escolha **Escolha múltipla** para quatro alternativas A–D.
   - Escolha **Verdadeiro ou falso** para as duas opções correspondentes.
   - Reveja a ordem e guarde.
   - Para estudantes praticarem antes da avaliação, active a opção de disponibilizar o teste como quiz.
4. Abra **Salas de teste**, seleccione o teste, dê um nome à sessão e escolha o limite de cada pergunta (10 a 300 segundos; por omissão, 15).
5. Clique **Criar sala e gerar URL**. O monitor mostra o link para copiar e partilhar.

## Percurso do estudante

1. Abra o endereço partilhado, por exemplo `http://localhost:3000/sala/CODIGO`.
2. Escreva o nome **ou** o número de registo académico. O valor serve para identificar e validar a entrada nesta sala.
3. Aguarde a aprovação da docente. A página verifica o estado da entrada automaticamente.
4. Depois de aprovado, responda às perguntas antes do temporizador chegar a zero. O servidor regista a resposta; quando o prazo termina, a pergunta fica errada e o teste avança.
5. No fim, aguarde a docente encerrar a sala. A nota final fica visível ao estudante após o encerramento.

O estudante não precisa de criar sessão nem recebe o gabarito durante o teste. O navegador mantém a participação apenas na sessão desse separador. A página informa que o painel docente mostra o tipo aproximado de dispositivo, o progresso e a posição do cursor/toque na área da pergunta.

## Monitorização e resultados

O monitor actualiza a cada dois segundos. A docente pode:

- ver pedidos pendentes e validar ou recusar cada entrada;
- acompanhar quem está ligado, o dispositivo aproximado, as perguntas respondidas e a posição mais recente do cursor/toque;
- encerrar a sessão, bloqueando novas entradas e respostas;
- consultar a lista de participantes, acertos e nota percentual e usar **Imprimir resultados** para imprimir ou guardar em PDF pelo navegador.

As notas só são reveladas no monitor após encerrar a sala. Pedidos ainda pendentes são recusados no encerramento e não recebem nota. A sala mantém uma cópia do teste e da versão seleccionada quando foi criada, mesmo se o docente editar o teste depois.

## Quiz de preparação

O estudante entra na sua conta, abre **Preparar quiz**, escolhe um quiz disponibilizado por um docente e responde a todas as perguntas. No fim, vê a nota e o feedback, e pode consultar o histórico das tentativas. As respostas correctas não são enviadas antes da submissão.

## Sequência sugerida para a apresentação

1. Como docente, criar um teste curto com uma pergunta de escolha múltipla e uma de verdadeiro/falso.
2. Criar a sala e projectar o link/URL.
3. Abrir o link numa janela privada ou noutro dispositivo e pedir entrada como estudante.
4. Mostrar o pedido pendente no monitor e aprová-lo.
5. Responder a uma pergunta enquanto se observa o progresso/cursor no monitor; opcionalmente deixar outra pergunta expirar.
6. Encerrar a sessão, mostrar as notas finais e abrir a impressão.
7. Opcionalmente, disponibilizar um teste como treino, entrar com uma conta estudante e mostrar o feedback e o histórico.

## Limites da versão académica

- O tipo de dispositivo é estimado a partir do navegador, e a posição representa cursor/toque enviado pela página; não é gravação do ecrã nem prova de identidade.
- A actualização é periódica (dois segundos), não usa WebSocket.
- A identificação escrita pelo estudante é validada pela docente, mas nesta demonstração não é conferida automaticamente com um sistema académico externo.
- A impressão usa o diálogo do navegador. Não há ainda exportação institucional assinada nem integração de notas.
- Os limites de tempo e as perguntas são configuráveis pela sala; a nota desta versão é a percentagem de respostas correctas.

## Componentes principais

No backend, `TesteService` gere os testes e revisões; `ServiceSalas` gere salas, entrada, respostas, monitorização e encerramento; `ServicePratica` disponibiliza quizzes de treino e histórico. `SalaTeste`, `ParticipanteSala`, `RespostaParticipante` e `TentativaPratica` representam os dados persistidos. As migrações `V2`, `V3` e `V4` acrescentam preparação, treino e sessões.

Na interface, `EditorTeste` prepara as perguntas, `SalasDocente` cria sessões e `MonitorSala` acompanha participantes. `SalaPublica` é a entrada e prova do estudante; `QuizPratica` implementa os quizzes para treino.

Para compilar e executar os testes e a interface, veja [testes](07-testes.md) e [instalação](08-instalacao-e-execucao.md). Preserve o volume PostgreSQL existente ao actualizar o projecto.
