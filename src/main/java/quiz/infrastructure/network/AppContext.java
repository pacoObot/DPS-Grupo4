package quiz.infrastructure.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quiz.application.port.output.AdminRepository;
import quiz.application.port.output.AnswerRepository;
import quiz.application.port.output.GameRepository;
import quiz.application.port.output.PasswordHasher;
import quiz.application.port.output.QuestionRepository;
import quiz.application.port.output.TeamRepository;
import quiz.application.port.output.UnitOfWork;
import quiz.application.service.ConcurrencyService;
import quiz.application.service.GameService;
import quiz.application.service.QuestionService;
import quiz.application.service.ScoreService;
import quiz.application.service.StatsService;
import quiz.application.usecase.admin.AdminLoginUseCase;
import quiz.application.usecase.admin.CreateQuestionUseCase;
import quiz.application.usecase.admin.DeleteQuestionUseCase;
import quiz.application.usecase.admin.ListQuestionsUseCase;
import quiz.application.usecase.admin.ManageGameUseCase;
import quiz.application.usecase.admin.UpdateQuestionUseCase;
import quiz.application.usecase.game.FinishGameUseCase;
import quiz.application.usecase.game.FinishRoundUseCase;
import quiz.application.usecase.game.StartGameUseCase;
import quiz.application.usecase.game.StartRoundUseCase;
import quiz.application.usecase.player.GetQuestionUseCase;
import quiz.application.usecase.player.GetScoreboardUseCase;
import quiz.application.usecase.player.JoinGameUseCase;
import quiz.application.usecase.player.LeaveGameUseCase;
import quiz.application.usecase.player.SubmitAnswerUseCase;
import quiz.infrastructure.Config;
import quiz.infrastructure.concurrency.ClientThreadPool;
import quiz.infrastructure.concurrency.GameScheduler;
import quiz.infrastructure.concurrency.RoundCoordinatorRegistry;
import quiz.infrastructure.persistence.postgres.AdminRepositoryImpl;
import quiz.infrastructure.persistence.postgres.AnswerRepositoryImpl;
import quiz.infrastructure.persistence.postgres.GameRepositoryImpl;
import quiz.infrastructure.persistence.postgres.Jdbc;
import quiz.infrastructure.persistence.postgres.JdbcUnitOfWork;
import quiz.infrastructure.persistence.postgres.PostgreSQLDataSource;
import quiz.infrastructure.persistence.postgres.QuestionRepositoryImpl;
import quiz.infrastructure.persistence.postgres.TeamRepositoryImpl;
import quiz.infrastructure.security.PBKDF2PasswordHasher;

/**
 * AppContext — COMPOSITION ROOT (raiz de composição / injeção manual).
 *
 *  É o único sítio que conhece TODAS as implementações concretas.
 *  As camadas superiores (use cases, handlers) só recebem as interfaces
 *  das portas — dependência apontada para dentro (Clean Architecture).
 *
 *  Ordem de construção:
 *    infra (DataSource, Jdbc, UoW)
 *      -> repositórios (port.output)
 *        -> serviços + use cases (application)
 *          -> rede/concorrência (network, concurrency)
 */
public final class AppContext {

    private static final Logger log = LoggerFactory.getLogger(AppContext.class);

    private final Config config;

    // infra
    private final PostgreSQLDataSource dataSource;
    private final Jdbc jdbc;
    private final UnitOfWork unitOfWork;
    private final PasswordHasher passwordHasher;

    // repositories
    private final QuestionRepository questionRepository;
    private final TeamRepository teamRepository;
    private final GameRepository gameRepository;
    private final AnswerRepository answerRepository;
    private final AdminRepository adminRepository;

    // services
    private final ScoreService scoreService;
    private final ConcurrencyService concurrencyService;
    private final GameService gameService;
    private final QuestionService questionService;
    private final StatsService statsService;

    // use cases (guardados para teste/monitorização)
    private final AdminLoginUseCase adminLoginUseCase;

    // rede + concorrência
    private final ClientRegistry registry;
    private final RoundCoordinatorRegistry roundCoordinators;
    private final ClientThreadPool threadPool;
    private final GameScheduler gameScheduler;
    private final AdminClientHandler adminClientHandler;
    private final PlayerClientHandler playerClientHandler;
    private final TcpServer tcpServer;

    public AppContext(Config config) {
        this.config = config;

        // ---------- infraestrutura de persistência ----------
        this.dataSource = new PostgreSQLDataSource(config);
        this.jdbc = new Jdbc(dataSource);
        this.unitOfWork = new JdbcUnitOfWork(dataSource);
        this.passwordHasher = new PBKDF2PasswordHasher();

        this.questionRepository = new QuestionRepositoryImpl(jdbc);
        this.teamRepository = new TeamRepositoryImpl(jdbc);
        this.gameRepository = new GameRepositoryImpl(jdbc);
        this.answerRepository = new AnswerRepositoryImpl(jdbc);
        this.adminRepository = new AdminRepositoryImpl(jdbc);

        // ---------- serviços da aplicação ----------
        this.scoreService = new ScoreService(teamRepository);
        this.concurrencyService = new ConcurrencyService();
        this.gameService = new GameService(gameRepository, teamRepository, questionRepository);
        this.questionService = new QuestionService(questionRepository);
        this.statsService = new StatsService(questionRepository, gameRepository,
                teamRepository, answerRepository, scoreService);

        // ---------- use cases ----------
        this.adminLoginUseCase = new AdminLoginUseCase(adminRepository, passwordHasher);
        CreateQuestionUseCase createQuestion = new CreateQuestionUseCase(questionRepository, unitOfWork);
        UpdateQuestionUseCase updateQuestion = new UpdateQuestionUseCase(questionRepository, unitOfWork);
        DeleteQuestionUseCase deleteQuestion = new DeleteQuestionUseCase(questionRepository, unitOfWork);
        ListQuestionsUseCase listQuestions = new ListQuestionsUseCase(questionRepository);
        ManageGameUseCase manageGame = new ManageGameUseCase(gameRepository, questionRepository, unitOfWork);
        StartGameUseCase startGame = new StartGameUseCase(gameRepository, teamRepository, unitOfWork);
        StartRoundUseCase startRound = new StartRoundUseCase(gameRepository, teamRepository, gameService, unitOfWork);
        FinishRoundUseCase finishRound = new FinishRoundUseCase(gameRepository, teamRepository, answerRepository, unitOfWork);
        FinishGameUseCase finishGame = new FinishGameUseCase(gameRepository, teamRepository, unitOfWork, scoreService);
        JoinGameUseCase joinGame = new JoinGameUseCase(gameRepository, teamRepository, unitOfWork);
        SubmitAnswerUseCase submitAnswer = new SubmitAnswerUseCase(teamRepository, gameRepository,
                questionRepository, answerRepository, unitOfWork);
        GetQuestionUseCase getQuestion = new GetQuestionUseCase(teamRepository, gameService);
        GetScoreboardUseCase getScoreboard = new GetScoreboardUseCase(scoreService);
        LeaveGameUseCase leaveGame = new LeaveGameUseCase(teamRepository, unitOfWork);

        // ---------- concorrência + rede ----------
        this.registry = new ClientRegistry();
        this.roundCoordinators = new RoundCoordinatorRegistry();
        this.threadPool = new ClientThreadPool(config.threadPoolSize());
        this.gameScheduler = new GameScheduler(gameRepository, teamRepository,
                startRound, finishRound, finishGame, scoreService,
                roundCoordinators, registry, concurrencyService);

        this.playerClientHandler = new PlayerClientHandler(registry, roundCoordinators,
                concurrencyService, joinGame, submitAnswer, getQuestion,
                getScoreboard, leaveGame, teamRepository, gameRepository);

        this.adminClientHandler = new AdminClientHandler(adminLoginUseCase, createQuestion,
                updateQuestion, deleteQuestion, listQuestions, manageGame, startGame,
                getScoreboard, scoreService, concurrencyService, teamRepository,
                gameRepository, registry, gameScheduler, statsService,
                config.serverPort(), config.threadPoolSize());

        this.tcpServer = new TcpServer(config.serverPort(), threadPool, registry,
                adminClientHandler, playerClientHandler, concurrencyService);
    }

    /** Arranque completo: migrations + admin default + scheduler + TCP. */
    public void start() {
        quiz.infrastructure.persistence.postgres.SchemaMigrator.migrate(config);
        adminLoginUseCase.bootstrapDefaultAdmin();
        log.info("Admin inicial verificado (admin/admin)");

        Thread schedulerThread = new Thread(gameScheduler, "game-scheduler");
        schedulerThread.setDaemon(false);
        schedulerThread.start();

        try {
            tcpServer.start();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao iniciar o servidor TCP: " + e.getMessage(), e);
        }
    }

    public void stop() {
        gameScheduler.shutdown();
        tcpServer.stop();
    }

    // ---- getters (usados pelo ServerMain, testes e monitor) -------------
    public Config config() { return config; }
    public PostgreSQLDataSource dataSource() { return dataSource; }
    public Jdbc jdbc() { return jdbc; }
    public UnitOfWork unitOfWork() { return unitOfWork; }
    public QuestionRepository questionRepository() { return questionRepository; }
    public TeamRepository teamRepository() { return teamRepository; }
    public GameRepository gameRepository() { return gameRepository; }
    public AnswerRepository answerRepository() { return answerRepository; }
    public AdminRepository adminRepository() { return adminRepository; }
    public ScoreService scoreService() { return scoreService; }
    public ConcurrencyService concurrencyService() { return concurrencyService; }
    public GameService gameService() { return gameService; }
    public QuestionService questionService() { return questionService; }
    public StatsService statsService() { return statsService; }
    public ClientRegistry registry() { return registry; }
    public RoundCoordinatorRegistry roundCoordinators() { return roundCoordinators; }
    public ClientThreadPool threadPool() { return threadPool; }
    public GameScheduler gameScheduler() { return gameScheduler; }
    public TcpServer tcpServer() { return tcpServer; }
}
