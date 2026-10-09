package quizrace;

import org.junit.jupiter.api.Test;
import quizrace.network.ProtocoloMensagem;
import quizrace.service.*;
import static quizrace.network.ProtocoloMensagem.Tipo.*;
import static org.junit.jupiter.api.Assertions.*;

class ProtocoloMensagemTest {
    @Test void constroiMensagens() {
        assertEquals("NOME|Clay", ProtocoloMensagem.construir(NOME, "Clay"));
        assertEquals("OPCAO|B|Maputo", ProtocoloMensagem.construir(OPCAO, "B", "Maputo"));
        assertEquals("RESULTADO|CORRECTO|85.00", ProtocoloMensagem.construir(RESULTADO, "CORRECTO", "85.00"));
    }

    @Test void interpretaTextoComAcentos() {
        var mensagem = ProtocoloMensagem.interpretar("MENSAGEM|Olá, Moçambique!");
        assertEquals(MENSAGEM, mensagem.tipo());
        assertEquals("Olá, Moçambique!", mensagem.campo(0));
    }

    @Test void rejeitaMensagensInvalidas() {
        for (String linha : new String[]{"", "OUTRO|x", "NOME|", "NOME|Ana|Bia", "RESPOSTA|E",
                "OPCAO|A", "RESULTADO|CORRECTO|NaN", "RESULTADO|OUTRO|5", "RESULTADO|ERRADO|-1", "NOME|Ana\n"}) {
            assertThrows(IllegalArgumentException.class, () -> ProtocoloMensagem.interpretar(linha), linha);
        }
        assertThrows(IllegalArgumentException.class, () -> ProtocoloMensagem.construir(NOME, "Ana|Bia"));
        assertThrows(IllegalArgumentException.class, () -> ProtocoloMensagem.construir(NOME, "Ana\rBia"));
    }

    @Test void validaRespostaUsandoRegrasExistentes() throws Exception {
        var pergunta = new GerenciadorPerguntas().carregar().get(0);
        var resposta = ProtocoloMensagem.paraResposta(ProtocoloMensagem.interpretar("RESPOSTA|a"), 3);
        assertTrue(pergunta.estaCorrecta(resposta));
        assertEquals(85, new CalculadoraPontuacao().calcular(pergunta.estaCorrecta(resposta), resposta.tempoSegundos()));
        var errada = ProtocoloMensagem.paraResposta(ProtocoloMensagem.interpretar("RESPOSTA|B"), 3);
        assertFalse(pergunta.estaCorrecta(errada));
        var tardia = ProtocoloMensagem.paraResposta(ProtocoloMensagem.interpretar("RESPOSTA|A"), 16);
        assertFalse(pergunta.estaCorrecta(tardia));
        assertThrows(IllegalArgumentException.class, () -> ProtocoloMensagem.paraResposta(ProtocoloMensagem.interpretar("NOME|Ana"), 1));
    }

    @Test void todasAsMensagensMantemCamposNaConversao() {
        for (String linha : new String[]{"NOME|Ana", "MENSAGEM|Olá", "PERGUNTA|Quanto é 2+2?",
                "OPCAO|A|4", "RESPOSTA|A", "RESULTADO|CORRECTO|85", "ERRO|Use A a D", "FIM|Até breve"}) {
            var mensagem = ProtocoloMensagem.interpretar(linha);
            assertEquals(linha, ProtocoloMensagem.construir(mensagem.tipo(), mensagem.campos().toArray(String[]::new)));
        }
    }
}
