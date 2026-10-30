# Visão do produto

## Problema e objectivo

O QuizRace evolui de um jogo de perguntas de terminal para uma plataforma académica de preparação e avaliação. Estudantes praticam e consultam a sua evolução; docentes criam e controlam avaliações; administradores mantêm utilizadores e configurações autorizadas.

O valor central é permitir uma avaliação digital controlada pelo servidor, com acompanhamento em tempo real e resultados académicos fiáveis, sem perder o jogo TCP já funcional.

## Utilizadores e limites de permissão

| Perfil | Pode | Não pode |
| --- | --- | --- |
| Administrador | gerir utilizadores, perfis e configurações globais autorizadas | controlar avaliações de outro docente por omissão |
| Docente | gerir as próprias avaliações, salas, resultados e relatórios | alterar contas ou avaliações sem autorização |
| Estudante | praticar, entrar em salas autorizadas, responder e consultar resultados permitidos | iniciar, pausar, encerrar ou alterar avaliações |

O backend aplica estas regras. A interface é apenas uma representação das permissões recebidas.

## Âmbito incremental

1. Fundação: identidade, perfis, persistência e API segura.
2. Preparação: banco de perguntas, quizzes, tentativas e histórico.
3. Avaliação: criação, sala, código de acesso e ciclo de vida.
4. Tempo real: participantes, respostas e painel docente.
5. Resultados: correcção objectiva, estatísticas e exportação definida.

Perguntas abertas, notificações externas, integração institucional e funcionalidades administrativas além da gestão de utilizadores ficam fora da primeira entrega, até terem critérios de avaliação definidos.
