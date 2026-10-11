package quiz.application.service;

import quiz.application.exception.BusinessException;
import quiz.application.port.output.GameRepository;
import quiz.application.port.output.QuestionRepository;
import quiz.application.port.output.TeamRepository;
import quiz.domain.entity.Game;
import quiz.domain.entity.Question;
import quiz.domain.entity.Team;
import quiz.shared.dto.GameInfoDTO;
import quiz.shared.dto.QuestionDTO;

import java.util.List;
import java.util.Optional;

/**
 * GameService — consultas e validações comuns sobre a partida.
 * Centraliza regras usadas por vários use cases (sem dependências de infra).
 */
public class GameService {

    private final GameRepository games;
    private final TeamRepository teams;
    private final QuestionRepository questions;

    public GameService(GameRepository games, TeamRepository teams, QuestionRepository questions) {
        this.games = games;
        this.teams = teams;
        this.questions = questions;
    }

    public Game requireGame(long gameId) {
        return games.findById(gameId)
                .orElseThrow(() -> new BusinessException.NotFound("Partida " + gameId + " não encontrada"));
    }

    public Game requireGameByCode(String code) {
        return games.findByCode(code)
                .orElseThrow(() -> new BusinessException.NotFound("Código de partida inválido: " + code));
    }

    public Question requireQuestion(long questionId) {
        return questions.findById(questionId)
                .orElseThrow(() -> new BusinessException.NotFound("Pergunta " + questionId + " não encontrada"));
    }

    public GameInfoDTO toInfo(Game game) {
        List<Team> all = teams.findByGame(game.getId().value());
        long online = all.stream().filter(Team::isOnline).count();
        return new GameInfoDTO(game.getId().value(), game.getName(), game.getJoinCode(),
                game.getStatus().name(), game.getCurrentRound(), game.getTotalRounds(),
                game.getTimePerQuestion(), (int) online, all.size());
    }

    /** Pergunta da ronda atual como DTO seguro para o cliente (sem resposta). */
    public QuestionDTO currentQuestionFor(Game game) {
        int round = game.getCurrentRound();
        var qid = games.findQuestionIdForRound(game.getId().value(), round)
                .orElseThrow(() -> new BusinessException.NotFound("Ronda " + round + " sem pergunta"));
        Question q = requireQuestion(qid.value());
        return toDTO(q, round, game.getTotalRounds());
    }

    public QuestionDTO toDTO(Question q, int round, int total) {
        String[] opts = q.options();
        return new QuestionDTO(round, total, q.getId().value(), q.getTimeLimit(),
                q.getText(), opts[0], opts[1], opts[2], opts[3]);
    }
}
