package quizrace;

import org.junit.jupiter.api.Test;
import quizrace.model.*;
import quizrace.server.*;
import quizrace.service.*;
import quizrace.network.*;
import java.io.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

class JogoConcorrenteTest {
    private Pergunta pergunta() { return new Pergunta("Quanto é 1+1?", List.of("2", "3", "4", "5"), 'A', "", 1); }
    private static final CanalJogador CANAL = new CanalJogador() {
        public void enviarMensagem(String texto) { }
        public void fechar() { }
    };

    @Test void registaTimeoutSeparadoSemPerderCompatibilidade() {
        var jogador = new Jogador("Ana");
        jogador.registarResposta(Resposta.avaliar(jogador, pergunta(), "A", 2, false));
        jogador.registrarTimeout(15);
        assertEquals(90, jogador.getPontuacao());
        assertEquals(1, jogador.getRespostasCorrectas());
        assertEquals(1, jogador.getRespostasErradas());
        assertEquals(1, jogador.getTimeouts());
        assertEquals(17, jogador.getTempoTotal());
        jogador.desconectar();
        assertFalse(jogador.isConectado());
        assertThrows(IllegalArgumentException.class, () -> new Jogador("Ana|Bia"));
        assertThrows(IllegalArgumentException.class, () -> jogador.registarResposta(true, 101, 0));
    }

    @Test void salaValidaMinimoMaximoNomesERegressoAEspera() {
        try (var sala = new SalaQuiz(List.of(pergunta()), Duration.ofSeconds(15), Duration.ofHours(1))) {
            var ana = new Jogador("Ana");
            sala.entrar(ana, CANAL);
            assertFalse(sala.podeComecar());
            assertEquals(EstadoSala.AGUARDANDO, sala.getEstado());
            assertThrows(IllegalArgumentException.class, () -> sala.entrar(new Jogador("ana"), CANAL));
            var bia = new Jogador("Bia");
            sala.entrar(bia, CANAL);
            assertTrue(sala.podeComecar());
            assertEquals(EstadoSala.INICIANDO, sala.getEstado());
            sala.sair(bia);
            assertEquals(EstadoSala.AGUARDANDO, sala.getEstado());
            sala.entrar(new Jogador("C"), CANAL);
            sala.entrar(new Jogador("D"), CANAL);
            sala.entrar(new Jogador("E"), CANAL);
            assertEquals(4, sala.getNumeroJogadores());
            assertThrows(IllegalStateException.class, () -> sala.entrar(new Jogador("F"), CANAL));
        }
    }

    @Test void rodadaRejeitaDuplicacoesConcorrentesEContaUmaConclusao() throws Exception {
        var ana = new Jogador("Ana");
        var bia = new Jogador("Bia");
        var relogio = new AtomicLong(0);
        var rodada = new Rodada(pergunta(), List.of(ana, bia), Duration.ofSeconds(15), relogio::get);
        try (var pool = Executors.newFixedThreadPool(8)) {
            List<Callable<Boolean>> chamadas = new ArrayList<>();
            for (int i = 0; i < 100; i++) chamadas.add(() -> rodada.responder(ana, 1, "A"));
            int aceites = 0;
            for (var futuro : pool.invokeAll(chamadas)) if (futuro.get()) aceites++;
            assertEquals(1, aceites);
        }
        assertEquals(1, rodada.getPendentes());
        assertFalse(rodada.responder(bia, 2, "A"));
        assertThrows(IllegalArgumentException.class, () -> rodada.responder(bia, 1, "X"));
        rodada.desistir(bia);
        rodada.desistir(bia);
        assertEquals(0, rodada.getPendentes());
        rodada.aguardar();
        var respostas = rodada.encerrar();
        assertTrue(respostas.get(ana).correcta());
        assertTrue(respostas.get(bia).tempoEsgotado());
        assertFalse(rodada.responder(ana, 1, "A"));
    }

    @Test void prazoExactoExpiraSemDormir() {
        var ana = new Jogador("Ana");
        var relogio = new AtomicLong();
        var rodada = new Rodada(pergunta(), List.of(ana), Duration.ofSeconds(15), relogio::get);
        relogio.set(15_000_000_000L);
        assertFalse(rodada.responder(ana, 1, "A"));
        var resposta = rodada.encerrar().get(ana);
        assertTrue(resposta.tempoEsgotado());
        assertEquals(15, resposta.tempoSegundos());
        assertEquals(0, new CalculadoraPontuacao().calcular(resposta.correcta(), resposta.tempoSegundos()));
    }

    @Test void rankingDesempataPeloNumeroDeCorrectas() {
        var ana = new Jogador("Ana");
        var bia = new Jogador("Bia");
        ana.registarResposta(true, 80, 4);
        bia.registarResposta(true, 40, 2);
        bia.registarResposta(true, 40, 2);
        assertEquals(List.of(bia, ana), new RankingService().ordenar(List.of(ana, bia)));
    }

    @Test void protocoloLimitaLinhasEValidaNovosCampos() throws Exception {
        assertEquals("NOME|Ana", ProtocoloMensagem.lerLinha(new StringReader("NOME|Ana\r\n")));
        assertThrows(IOException.class, () -> ProtocoloMensagem.lerLinha(new StringReader("A".repeat(2049))));
        assertThrows(EOFException.class, () -> ProtocoloMensagem.lerLinha(new StringReader("NOME|Ana")));
        for (String linha : List.of("RESPOSTA|1|B", "PERGUNTA|1|Teste?", "RANKING|1|Ana|90|1|0|0|2.00", "RESULTADO|TIMEOUT|0"))
            assertNotNull(ProtocoloMensagem.interpretar(linha));
        for (String linha : List.of("RESPOSTA|-1|B", "RESPOSTA|1|Z", "RESULTADO|CORRECTO|101", "JOGADORES|5|4", "INICIO|X"))
            assertThrows(IllegalArgumentException.class, () -> ProtocoloMensagem.interpretar(linha));
    }

    @Test void configuracaoPriorizaArgumentosDepoisAmbiente() {
        var ambiente = Map.of("QUIZ_HOST", "server", "QUIZ_PORT", "6000");
        assertEquals("outro", ConfiguracaoRede.host("outro", ambiente));
        assertEquals("server", ConfiguracaoRede.host(null, ambiente));
        assertEquals("localhost", ConfiguracaoRede.host(null, Map.of()));
        assertEquals(7000, ConfiguracaoRede.porta("7000", ambiente));
        assertEquals(6000, ConfiguracaoRede.porta(null, ambiente));
        assertThrows(IllegalArgumentException.class, () -> ConfiguracaoRede.porta("0", ambiente));
    }
}
