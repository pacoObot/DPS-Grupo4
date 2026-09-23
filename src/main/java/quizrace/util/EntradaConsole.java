package quizrace.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public final class EntradaConsole {
    private record Evento(String linha, IOException erro) { }
    private final BlockingQueue<Evento> linhas = new LinkedBlockingQueue<>();
    private boolean terminado;

    public EntradaConsole(InputStream entrada) {
        // Uma única thread lê o terminal, permitindo ao jogo aplicar um prazo sem bloquear em readLine().
        Thread leitor = new Thread(() -> {
            BufferedReader buffer = new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8));
            try {
                String linha;
                while ((linha = buffer.readLine()) != null) linhas.add(new Evento(linha, null));
                linhas.add(new Evento(null, null));
            } catch (IOException e) {
                linhas.add(new Evento(null, e));
            }
        }, "leitor-console");
        leitor.setDaemon(true);
        leitor.start();
    }

    public String ler() { return receber(-1); }
    public String ler(long prazoNanos) { return receber(Math.max(0, prazoNanos)); }

    private String receber(long prazoNanos) {
        if (terminado) throw new IllegalStateException("Entrada terminada.");
        try {
            Evento evento = prazoNanos < 0 ? linhas.take() : linhas.poll(prazoNanos, TimeUnit.NANOSECONDS);
            if (evento == null) return null;
            if (evento.linha() == null) {
                terminado = true;
                throw new IllegalStateException("Entrada terminada.", evento.erro());
            }
            return evento.linha();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Leitura interrompida.", e);
        }
    }
}
