package quizrace;

import org.junit.jupiter.api.Test;
import quizrace.model.*;
import quizrace.service.*;
import quizrace.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class QuizRaceTest {
    @Test void calculaPontuacaoComFraccoesEMinimo() {
        var calculadora = new CalculadoraPontuacao();
        assertEquals(0, calculadora.calcular(false, 2));
        assertEquals(100, calculadora.calcular(true, 0));
        assertEquals(87.5, calculadora.calcular(true, 2.5));
        assertEquals(25, calculadora.calcular(true, 15));
        assertEquals(10, calculadora.calcular(true, 30));
        assertThrows(IllegalArgumentException.class, () -> calculadora.calcular(true, -1));
        assertThrows(IllegalArgumentException.class, () -> calculadora.calcular(true, Double.NaN));
    }

    @Test void acumulaResultadosDoJogador() {
        var jogador = new Jogador(" Ana ");
        jogador.registarResposta(true, 90, 2);
        jogador.registarResposta(false, 0, 15);
        assertEquals("Ana", jogador.getNome());
        assertEquals(90, jogador.getPontuacao());
        assertEquals(17, jogador.getTempoTotal());
        assertEquals(15, jogador.getTempoUltimaResposta());
        assertEquals(1, jogador.getRespostasCorrectas());
        assertEquals(1, jogador.getRespostasErradas());
        assertThrows(IllegalArgumentException.class, () -> new Jogador(" "));
        assertThrows(IllegalArgumentException.class, () -> jogador.registarResposta(false, 50, 1));
    }

    @Test void validaPerguntaEPrazo() {
        var alternativas = new ArrayList<>(List.of("Um", "Dois", "Três", "Quatro"));
        var pergunta = new Pergunta("Quanto é 1 + 1?", alternativas, 'b', null);
        alternativas.set(1, "Alterada");
        assertEquals("Dois", pergunta.alternativas().get(1));
        assertTrue(pergunta.estaCorrecta(new Resposta(" b ", 2, false)));
        assertFalse(pergunta.estaCorrecta(new Resposta("A", 2, false)));
        assertFalse(pergunta.estaCorrecta(new Resposta("B", 15, true)));
        assertFalse(pergunta.estaCorrecta(new Resposta("B", 16, false)));
        assertThrows(IllegalArgumentException.class, () -> new Pergunta("?", List.of("A"), 'A', ""));
        assertThrows(IllegalArgumentException.class, () -> new Pergunta("?", alternativas, 'E', ""));
    }

    @Test void carregaPerguntasDeExemplo() throws IOException {
        var perguntas = new GerenciadorPerguntas().carregar();
        assertEquals(3, perguntas.size());
        assertEquals('A', perguntas.get(0).respostaCorrecta());
    }

    @Test void rankingOrdenaPorPontosEDesempataPorTempo() {
        var ana = new Jogador("Ana");
        var bia = new Jogador("Bia");
        var carlos = new Jogador("Carlos");
        ana.registarResposta(true, 90, 2);
        bia.registarResposta(true, 90, 1);
        carlos.registarResposta(true, 95, 1);
        var original = List.of(ana, bia, carlos);
        assertEquals(List.of(carlos, bia, ana), new RankingService().ordenar(original));
        assertEquals(ana, original.get(0));
    }

    @Test void processaTodosOsJogadoresSequencialmente() {
        var entrada = new EntradaConsole(new ByteArrayInputStream("X\nA\nB\n".getBytes(StandardCharsets.UTF_8)));
        var saida = new ByteArrayOutputStream();
        var ana = new Jogador("Ana");
        var bia = new Jogador("Bia");
        var pergunta = new Pergunta("?", List.of("1", "2", "3", "4"), 'A', "");
        double tempo = new QuizSequencial(entrada, new PrintStream(saida)).executar(List.of(ana, bia), List.of(pergunta));
        assertEquals(1, ana.getRespostasCorrectas());
        assertEquals(1, bia.getRespostasErradas());
        assertTrue(ana.getPontuacao() > 0);
        assertEquals(0, bia.getPontuacao());
        assertTrue(tempo >= 0);
        String texto = saida.toString(StandardCharsets.UTF_8);
        assertTrue(texto.contains("Alternativa inválida"));
        assertTrue(texto.indexOf("Vez de Ana") < texto.indexOf("Vez de Bia"));
    }

    @Test void leituraRespeitaPrazoESinalizaFim() throws IOException {
        try (var escritor = new PipedOutputStream(); var leitor = new PipedInputStream(escritor)) {
            var entrada = new EntradaConsole(leitor);
            assertNull(entrada.ler(20_000_000));
            escritor.write("A\n".getBytes(StandardCharsets.UTF_8));
            escritor.flush();
            assertEquals("A", entrada.ler(1_000_000_000));
        }
        var vazia = new EntradaConsole(new ByteArrayInputStream(new byte[0]));
        assertThrows(IllegalStateException.class, vazia::ler);
    }
}
