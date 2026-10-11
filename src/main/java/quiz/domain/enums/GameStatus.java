package quiz.domain.enums;

/**
 * Ciclo de vida de uma partida.
 * OPEN (inscrições abertas) -> RUNNING -> PAUSED -> RUNNING -> FINISHED
 */
public enum GameStatus {
    DRAFT,     // criada, a configurar (perguntas/tempo)
    OPEN,      // inscrições abertas — jogadores podem entrar
    RUNNING,   // partida a correr
    PAUSED,    // pausada (o timer da ronda é congelado pelo servidor)
    FINISHED   // terminada
}
