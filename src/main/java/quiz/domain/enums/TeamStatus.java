package quiz.domain.enums;

/** Estados possíveis de uma equipa durante a partida. */
public enum TeamStatus {
    WAITING,   // inscrita, à espera do início
    PLAYING,   // ronda ativa, pode responder
    ANSWERED,  // já respondeu nesta ronda
    TIMEOUT,   // não respondeu a tempo
    OFFLINE,   // desconectada (o jogo continua com as outras)
    FINISHED   // partida terminada
}
