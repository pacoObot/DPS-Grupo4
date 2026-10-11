package quiz.shared.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProtocolTest {

    @Test
    void parseSeparacaoPorPipe() {
        Protocol.Message m = Protocol.parse("ANSWER|42|B");
        assertNotNull(m);
        assertEquals("ANSWER", m.type());
        assertEquals(2, m.count());
        assertEquals(42, m.intField(0, -1));
        assertEquals("B", m.field(1));
    }

    @Test
    void parsePreservaCamposVazios() {
        Protocol.Message m = Protocol.parse("LIST_QUESTIONS|||true");
        assertEquals("LIST_QUESTIONS", m.type());
        assertEquals(3, m.count());
        assertEquals("", m.field(0));
        assertTrue(m.boolField(2, false));
    }

    @Test
    void parseRejeitaVazio() {
        assertNull(Protocol.parse(""));
        assertNull(Protocol.parse("   "));
        assertNull(Protocol.parse(null));
    }

    @Test
    void parseRejeitaTipoInvalido() {
        assertNull(Protocol.parse("JOIN GAME|x"));   // espaço no tipo
        assertNull(Protocol.parse("9LIVES|x"));      // começa por dígito
        assertNull(Protocol.parse("DROP;TABLE|x"));
    }

    @Test
    void parseRejeitaMensagemGigante() {
        String huge = "A|" + "x".repeat(Protocol.MAX_MESSAGE_LENGTH + 10);
        assertNull(Protocol.parse(huge));
    }

    @Test
    void buildEJuntaCampos() {
        assertEquals("ANSWER_RESULT|7|CORRECT|18|8|26",
                Protocol.build("ANSWER_RESULT", 7, "CORRECT", 18, 8, 26));
    }

    @Test
    void sanitizeLimpaPipesEBreaks() {
        assertEquals("a-b c", Protocol.sanitize("a|b\nc"));
        assertEquals("", Protocol.sanitize(null));
    }

    @Test
    void intFieldUsaDefaultEmLixo() {
        Protocol.Message m = Protocol.parse("ANSWER|abc|B");
        assertEquals(-1, m.intField(0, -1));
    }
}