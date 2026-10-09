package quizrace;

import org.junit.jupiter.api.Test;
import quizrace.client.ClienteSimulado.Configuracao;
import static org.junit.jupiter.api.Assertions.*;

class ClienteSimuladoTest {
    @Test void validaAtrasosEProbabilidade() {
        assertDoesNotThrow(() -> new Configuracao("BOT-1", 100, 500, 1));
        assertThrows(IllegalArgumentException.class, () -> new Configuracao("BOT-1", 500, 100, .5));
        assertThrows(IllegalArgumentException.class, () -> new Configuracao("BOT-1", 0, 10, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new Configuracao("BOT-1", 0, 10, 1.1));
    }
}
