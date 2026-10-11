package quiz.application.usecase.player;

import quiz.application.exception.BusinessException;
import quiz.application.port.output.AnswerRepository;
import quiz.application.port.output.GameRepository;
import quiz.application.port.output.QuestionRepository;
import quiz.application.port.output.TeamRepository;
import quiz.application.port.output.UnitOfWork;
import quiz.domain.entity.Answer;
import quiz.domain.entity.Game;
import quiz.domain.entity.Question;
import quiz.domain.entity.Team;
import quiz.domain.enums.GameStatus;
import quiz.domain.entity.Score;
import quiz.shared.dto.AnswerResultDTO;

import java.time.Duration;
import java.time.Instant;

/**
 * =====================================================================
 *  SubmitAnswerUseCase — o coração transacional do sistema
 * =====================================================================
 *
 * EXECUTADO CONCORRENTEMENTE por uma thread por cliente (uma por equipa).
 * Dentro de uma única transação PostgreSQL:
 *
 *   BEGIN
 *     1. SELECT team  ... FOR UPDATE   (trava a linha da equipa)
 *     2. SELECT game   ... FOR UPDATE   (lê deadline da ronda)
 *     3. valida: partida RUNNING, pergunta == ronda atual, tempo válido
 *     4. verifica resposta duplicada    (SELECT + UNIQUE(team_id,question_id))
 *     5. INSERT answers                 (prova persistida)
 *     6. UPDATE teams SET score=...     (atómico, sob a trava da linha)
 *   COMMIT   | ROLLBACK em erro
 *
 * SEGURANÇA: a resposta certa é lida do banco e comparada AQUI, no servidor.
 * O cliente só envia "ANSWER|questionId|B" — nunca pontuação (spec §17).
 */
public class SubmitAnswerUseCase {

    private final TeamRepository teams;
    private final GameRepository games;
    private final QuestionRepository questions;
    private final AnswerRepository answers;
    private final UnitOfWork uow;

    public SubmitAnswerUseCase(TeamRepository teams, GameRepository games,
                               QuestionRepository questions, AnswerRepository answers,
                               UnitOfWork uow) {
        this.teams = teams;
        this.games = games;
        this.questions = questions;
        this.answers = answers;
        this.uow = uow;
    }

    public AnswerResultDTO call(long teamId, long questionId, char option) {
        if (option < 'A' || option > 'D')
            throw new BusinessException.Validation("Opção deve ser A, B, C ou D");

        return uow.call(() -> {
            // 0) localiza a equipa (leitura simples) para saber a partida
            Team teamInfo = teams.findById(teamId)
                    .orElseThrow(() -> new BusinessException.NotFound("Equipa não encontrada"));

            // 1) trava PRIMEIRO a partida (mesma ordem do GameScheduler:
            //    game -> teams). Duas threads concorrentes serializam aqui e
            //    evitam deadlock entre submeter resposta e avançar a ronda.
            Game game = games.lockById(teamInfo.getGameId().value())
                    .orElseThrow(() -> new BusinessException.NotFound("Partida não encontrada"));

            // 1b) depois trava a linha da equipa — serializa escritas
            //     concorrentes desta equipa (double submission / lost update)
            Team team = teams.lockById(teamId)
                    .orElseThrow(() -> new BusinessException.NotFound("Equipa não encontrada"));

            if (game.getStatus() == GameStatus.PAUSED)
                throw new BusinessException.Conflict("Partida pausada — aguarde");
            if (game.getStatus() != GameStatus.RUNNING)
                throw new BusinessException.Conflict("A partida não está a correr");

            // 2) pergunta da ronda ATUAL (o servidor decide, não o cliente)
            long currentQuestionId = games
                    .findQuestionIdForRound(game.getId().value(), game.getCurrentRound())
                    .orElseThrow(() -> new BusinessException.NotFound("Ronda sem pergunta"))
                    .value();

            if (questionId != currentQuestionId) {
                // resposta para outra ronda => ignorada como tardia
                return new AnswerResultDTO(questionId, "LATE", 0, 0, team.getScore().getPoints());
            }

            // 3) double submission
            if (answers.existsByTeamAndQuestion(teamId, currentQuestionId)) {
                return new AnswerResultDTO(questionId, "DUPLICATE", 0, 0, team.getScore().getPoints());
            }

            Question question = questions.findById(currentQuestionId)
                    .orElseThrow(() -> new BusinessException.NotFound("Pergunta não encontrada"));

            // 4) autoridade do tempo é o SERVIDOR (round_started_at + time_per_question)
            Instant deadline = game.currentDeadline();
            Instant now = Instant.now();
            Instant roundStart = game.getRoundStartedAt();
            long totalMs = game.getTimePerQuestion() * 1000L;

            if (deadline == null || now.isAfter(deadline)) {
                team.markTimeout();               // wrong++ e status TIMEOUT
                teams.update(team);
                return new AnswerResultDTO(questionId, "TIMEOUT", 0, 0, team.getScore().getPoints());
            }

            long elapsedMs = roundStart == null ? 0 : Math.max(0, Duration.between(roundStart, now).toMillis());
            long remainingMs = Math.max(0, Duration.between(now, deadline).toMillis());

            boolean correct = question.isCorrect(option);
            int earned = 0;

            if (correct) {
                earned = Score.calculate(question.getPoints(), remainingMs, totalMs);
                team.registerCorrectAnswer(earned);
            } else {
                team.registerWrongAnswer();
            }
            teams.update(team); // persiste score + status sob a trava da linha

            // 5) prova da resposta (UNIQUE(team_id, question_id) é o guarda final)
            answers.save(new Answer(0, game.getId(), team.getId(),
                    question.getId(), option, correct, earned, elapsedMs, Instant.now()));

            return new AnswerResultDTO(questionId,
                    correct ? "CORRECT" : "WRONG",
                    earned, correct ? earned - question.getPoints() : 0,
                    team.getScore().getPoints());
        });
    }
}
