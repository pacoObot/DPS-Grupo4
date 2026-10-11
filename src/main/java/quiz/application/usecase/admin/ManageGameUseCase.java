package quiz.application.usecase.admin;

import quiz.application.exception.BusinessException;
import quiz.application.port.output.GameRepository;
import quiz.application.port.output.QuestionRepository;
import quiz.application.port.output.UnitOfWork;
import quiz.domain.entity.Game;
import quiz.domain.entity.Question;
import quiz.domain.enums.GameStatus;
import quiz.domain.valueobject.QuestionId;
import quiz.shared.dto.GameInfoDTO;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * ManageGameUseCase — gestão completa da partida pelo admin:
 *   criar, configurar tempo, selecionar perguntas, abrir inscrições,
 *   pausar, retomar, encerrar.
 *
 * O Iniciar está no StartGameUseCase (spec §28 separa-os).
 * Todas as transições passam por lockById (SELECT ... FOR UPDATE) dentro
 * de uma transação — duas operações concorrentes sobre a mesma partida
 * nunca corrompem o estado.
 */
public class ManageGameUseCase {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // sem 0/O/1/I

    private final GameRepository games;
    private final QuestionRepository questions;
    private final UnitOfWork uow;

    public ManageGameUseCase(GameRepository games, QuestionRepository questions, UnitOfWork uow) {
        this.games = games;
        this.questions = questions;
        this.uow = uow;
    }

    /** CREATE_GAME|nome|q1,q2,q3|tempo  -> cria em DRAFT com código de entrada. */
    public GameInfoDTO create(String name, List<Long> questionIds, int timePerQuestion) {
        if (questionIds == null || questionIds.isEmpty())
            throw new BusinessException.Validation("Selecione pelo menos 1 pergunta");

        return uow.call(() -> {
            List<QuestionId> ids = new ArrayList<>();
            for (Long qid : questionIds) {
                Question q = questions.findById(qid)
                        .orElseThrow(() -> new BusinessException.NotFound("Pergunta " + qid + " não existe"));
                if (!q.isActive())
                    throw new BusinessException.Conflict("A pergunta " + qid + " está desativada");
                ids.add(new QuestionId(qid));
            }

            Game game = Game.create(name, timePerQuestion, ids);
            game.validate();
            game.setJoinCode(randomCode());
            Game saved = games.save(game);
            games.saveGameQuestions(saved.getId().value(), ids);
            return toInfo(saved);
        });
    }

    /** OPEN_GAME|gameId — abre as inscrições (DRAFT -> OPEN). */
    public GameInfoDTO open(long gameId) {
        return uow.call(() -> {
            Game g = games.lockById(gameId)
                    .orElseThrow(() -> new BusinessException.NotFound("Partida não encontrada"));
            if (g.getStatus() == GameStatus.OPEN) return toInfo(g);   // idempotente
            if (g.getStatus() != GameStatus.DRAFT)
                throw new BusinessException.Conflict("Só se pode abrir uma partida em DRAFT (estado: " + g.getStatus() + ")");
            g.setStatus(GameStatus.OPEN);
            games.update(g);
            return toInfo(g);
        });
    }

    /** PAUSE_GAME|gameId */
    public GameInfoDTO pause(long gameId) {
        return uow.call(() -> {
            Game g = games.lockById(gameId)
                    .orElseThrow(() -> new BusinessException.NotFound("Partida não encontrada"));
            g.pause(); // regra de domínio lança IllegalStateException se inválido
            games.update(g);
            return toInfo(g);
        });
    }

    /** RESUME_GAME|gameId */
    public GameInfoDTO resume(long gameId) {
        return uow.call(() -> {
            Game g = games.lockById(gameId)
                    .orElseThrow(() -> new BusinessException.NotFound("Partida não encontrada"));
            g.resume();
            games.update(g);
            return toInfo(g);
        });
    }

    /** STOP_GAME|gameId — encerra antecipadamente. */
    public GameInfoDTO stop(long gameId) {
        return uow.call(() -> {
            Game g = games.lockById(gameId)
                    .orElseThrow(() -> new BusinessException.NotFound("Partida não encontrada"));
            g.finish();
            games.update(g);
            return toInfo(g);
        });
    }

    /** Configura tempo/rodadas enquanto a partida não começou. */
    public GameInfoDTO configure(long gameId, int timePerQuestion, int totalRounds) {
        return uow.call(() -> {
            Game g = games.lockById(gameId)
                    .orElseThrow(() -> new BusinessException.NotFound("Partida não encontrada"));
            if (g.getStatus() != GameStatus.DRAFT && g.getStatus() != GameStatus.OPEN)
                throw new BusinessException.Conflict("Só é possível configurar em DRAFT/OPEN");
            if (timePerQuestion > 0) g.setTimePerQuestion(timePerQuestion);
            if (totalRounds > 0 && totalRounds <= g.getTotalRounds()) g.setTotalRounds(totalRounds);
            g.validate();
            games.update(g);
            return toInfo(g);
        });
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        return sb.toString();
    }

    public static GameInfoDTO toInfo(Game g) {
        return new GameInfoDTO(g.getId().value(), g.getName(), g.getJoinCode(),
                g.getStatus().name(), g.getCurrentRound(), g.getTotalRounds(),
                g.getTimePerQuestion(), 0, 0);
    }
}
