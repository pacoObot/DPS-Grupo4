package quiz.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * QuizWebApplication — gateway web para o Quiz Multiplayer Paralelo.
 *
 *  Serve uma página mobile (browser do telemóvel) e, por cada jogador,
 *  abre uma ligação TCP ao servidor do quiz (porta 5050) fazendo de
 *  ponte entre WebSocket (browser) e o protocolo de texto (servidor).
 */
@SpringBootApplication
public class QuizWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuizWebApplication.class, args);
    }
}