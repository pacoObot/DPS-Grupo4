package quiz.shared.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes do DTO {@link ServerStats}: serialização de ida e volta e
 * tolerância do parser a campos em falta/formatos inesperados.
 */
class ServerStatsTest {

    @Test
    void roundTripPreservaTodosOsCampos() {
        ServerStats original = new ServerStats(
                5050, 50, 3, 7,
                20, 18, 6,
                4, "RUNNING", "Demo", 2, 3,
                12, 9, 40, 2,
                List.of(new ServerStats.TopEntry(1, "Alpha", 120),
                        new ServerStats.TopEntry(2, "Beta", 80)));

        ServerStats parsed = ServerStats.fromProtocol(original.toProtocol().split("\\|", -1));

        assertEquals(5050, parsed.port());
        assertEquals(50, parsed.poolSize());
        assertEquals(3, parsed.activeThreads());
        assertEquals(7, parsed.connections());
        assertEquals(20, parsed.questionsTotal());
        assertEquals(18, parsed.questionsActive());
        assertEquals(6, parsed.categories());
        assertEquals(4, parsed.gameId());
        assertEquals("RUNNING", parsed.gameStatus());
        assertEquals("Demo", parsed.gameName());
        assertEquals(2, parsed.round());
        assertEquals(3, parsed.rounds());
        assertEquals(12, parsed.teamsJoined());
        assertEquals(9, parsed.teamsOnline());
        assertEquals(40, parsed.answers());
        assertEquals(2, parsed.timeouts());
        assertEquals(2, parsed.top().size());
        assertEquals("Alpha", parsed.top().get(0).team());
        assertEquals(120, parsed.top().get(0).points());
        assertEquals(1, parsed.top().get(0).position());
    }

    @Test
    void parserToleraCamposEmFalta() {
        ServerStats s = ServerStats.fromProtocol(new String[]{"STATS", "port:5050"});
        assertEquals(5050, s.port());
        assertEquals(0, s.poolSize());
        assertEquals(0, s.connections());
        assertTrue(s.questionsTotal() == 0);
        assertTrue(s.top().isEmpty());
    }

    @Test
    void parserIgnoraChavesDesconhecidasEValoresNaoNumericos() {
        ServerStats s = ServerStats.fromProtocol(new String[]{
                "STATS", "port:abc", "threads:5", "futuro:qualquer", "malformado"});
        assertEquals(0, s.port());
        assertEquals(5, s.activeThreads());
    }

    @Test
    void cleanRemoveSeparadoresQueQuebrariamOFormato() {
        ServerStats s = new ServerStats(
                1, 1, 1, 1, 1, 1, 1, 1, "A|B", "n:ome;x", 1, 1, 1, 1, 1, 1,
                List.of(new ServerStats.TopEntry(1, "eq|uipe", 5)));
        String line = s.toProtocol();
        // não pode conter '|' nem ':' dentro dos valores de texto
        String parsedName = ServerStats.fromProtocol(line.split("\\|", -1)).gameName();
        assertEquals("n-ome-x", parsedName);
        assertEquals("A-B", ServerStats.fromProtocol(line.split("\\|", -1)).gameStatus());
    }

    @Test
    void topVazioNaoSerializaCampoTop() {
        ServerStats s = new ServerStats(1, 1, 1, 1, 0, 0, 0, -1, "OPEN", "x", 0, 0, 0, 0, 0, 0, List.of());
        assertFalse(s.toProtocol().contains("|top:"));
        assertTrue(ServerStats.fromProtocol(s.toProtocol().split("\\|", -1)).top().isEmpty());
    }
}