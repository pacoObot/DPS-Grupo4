package quizrace;

import org.junit.jupiter.api.*;
import quizrace.server.*;
import quizrace.model.Pergunta;
import quizrace.network.ProtocoloMensagem;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class TcpIntegrationTest {
    private static final class Cliente implements AutoCloseable {
        final Socket socket;
        final BufferedReader entrada;
        final BufferedWriter saida;
        Cliente(int porta) throws IOException {
            socket = new Socket("127.0.0.1", porta);
            socket.setSoTimeout(5000);
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            saida = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        }
        void enviar(String mensagem) throws IOException { saida.write(mensagem + "\n"); saida.flush(); }
        String ate(String prefixo) throws IOException {
            String linha;
            while ((linha = ProtocoloMensagem.lerLinha(entrada)) != null) {
                if (linha.startsWith(prefixo)) return linha;
            }
            throw new EOFException("Esperava " + prefixo);
        }
        public void close() throws IOException { socket.close(); }
    }

    @Test @Timeout(20) void doisClientesRodadasTimeoutRankingESalaFechada() throws Exception {
        var perguntas = List.of(new Pergunta("Um?", List.of("A", "B", "C", "D"), 'A', "", 1),
                new Pergunta("Dois?", List.of("A", "B", "C", "D"), 'B', "", 2));
        try (var servidor = new QuizServer(0, perguntas, Duration.ofSeconds(2), Duration.ofMillis(20));
             var executor = Executors.newSingleThreadExecutor()) {
            Future<?> tarefa = executor.submit(() -> { try { servidor.executar(); } catch (IOException e) { throw new UncheckedIOException(e); } });
            try (var ana = new Cliente(servidor.getPorta()); var bia = new Cliente(servidor.getPorta())) {
                ana.enviar("NOME|Ana"); ana.ate("AGUARDAR|");
                try (var duplicado = new Cliente(servidor.getPorta())) {
                    duplicado.enviar("NOME|ana");
                    assertTrue(duplicado.ate("ERRO|").contains("Nome"));
                }
                bia.enviar("NOME|Bia");
                assertEquals(ana.ate("PERGUNTA|"), bia.ate("PERGUNTA|"));
                ana.ate("TEMPO|"); bia.ate("TEMPO|");
                try (var tardio = new Cliente(servidor.getPorta())) {
                    tardio.enviar("NOME|Tardio"); assertTrue(tardio.ate("ERRO|").contains("Sala"));
                }
                ana.enviar("RESPOSTA|1|X"); ana.ate("ERRO|");
                ana.enviar("RESPOSTA|1|A");
                ana.enviar("RESPOSTA|1|A");
                ana.ate("ERRO|"); // Bia ainda não respondeu: a rodada não avançou.
                bia.enviar("RESPOSTA|1|D");
                assertTrue(ana.ate("RESULTADO|").contains("CORRECTO"));
                assertEquals("RESULTADO|ERRADO|0.00", bia.ate("RESULTADO|"));
                assertEquals(ana.ate("PERGUNTA|"), bia.ate("PERGUNTA|"));
                ana.ate("TEMPO|"); bia.ate("TEMPO|");
                ana.enviar("RESPOSTA|1|A"); ana.ate("ERRO|");
                ana.enviar("RESPOSTA|2|B");
                assertTrue(ana.ate("RESULTADO|").contains("CORRECTO"));
                assertEquals("RESULTADO|TIMEOUT|0.00", bia.ate("RESULTADO|"));
                assertTrue(ana.ate("RANKING|1|").contains("Ana"));
                assertTrue(bia.ate("RANKING|1|").contains("Ana"));
                assertTrue(ana.ate("VENCEDOR|").contains("Ana"));
                ana.ate("FIM|"); bia.ate("FIM|");
                assertEquals(EstadoSala.FINALIZADO, servidor.getSala().getEstado());
                assertEquals(2, servidor.getSala().getRanking().get(0).getRespostasCorrectas());
            } finally { servidor.close(); }
            tarefa.get(3, TimeUnit.SECONDS);
        }
    }
}
