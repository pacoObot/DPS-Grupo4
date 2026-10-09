package quizrace;

import org.junit.jupiter.api.Test;
import quizrace.benchmark.*;
import quizrace.service.*;
import quizrace.util.MedidorDesempenho;
import static org.junit.jupiter.api.Assertions.*;

class BenchmarkTest {
    @Test void calculaSpeedupEEficiencia() {
        var resultado = new ResultadoBenchmark(100, 800, 200, 4);
        assertEquals(4, resultado.speedup());
        assertEquals(1, resultado.eficiencia());
        assertThrows(IllegalArgumentException.class, () -> new ResultadoBenchmark(100, 800, 0, 4));
    }
    @Test void executaCargaIdenticaSemPartilharJogadoresMutaveis() throws Exception {
        var perguntas = new GerenciadorPerguntas().carregar();
        var carga = JogadorSimulado.gerar(20, perguntas, 42);
        assertEquals(carga, JogadorSimulado.gerar(20, perguntas, 42));
        var sequencial = QuizSequencial.executarSimulados(carga, perguntas, 10);
        try (var paralelo = new QuizParalelo(4)) {
            var resultados = paralelo.executar(carga, perguntas, 10);
            BenchmarkRunner.verificar(sequencial, resultados);
            assertNotSame(sequencial.get(0).jogador(), resultados.get(0).jogador());
            assertEquals(10, resultados.get(0).jogador().getRespostasCorrectas() + resultados.get(0).jogador().getRespostasErradas());
        }
    }
    @Test void medidorValidaCicloEConversao() {
        var medidor = new MedidorDesempenho();
        assertThrows(IllegalStateException.class, medidor::getNanos);
        assertThrows(IllegalStateException.class, medidor::terminar);
        medidor.iniciar(); medidor.terminar();
        assertTrue(medidor.getNanos() >= 0);
        assertEquals(medidor.getNanos() / 1_000_000.0, medidor.getMilissegundos());
        assertThrows(IllegalStateException.class, medidor::terminar);
    }
}
