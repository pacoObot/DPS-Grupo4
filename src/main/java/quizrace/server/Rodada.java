package quizrace.server;

import quizrace.model.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.LongSupplier;

public final class Rodada {
    private final Pergunta pergunta;
    private final Set<Jogador> jogadores;
    private final Map<Jogador, Resposta> respostas = new LinkedHashMap<>();
    private final CountDownLatch conclusoes;
    private final LongSupplier relogio;
    private final long inicio;
    private final long limiteNanos;
    private boolean encerrada;

    public Rodada(Pergunta pergunta, List<Jogador> jogadores, Duration limite) {
        this(pergunta, jogadores, limite, System::nanoTime);
    }
    public Rodada(Pergunta pergunta, List<Jogador> jogadores, Duration limite, LongSupplier relogio) {
        if (limite.isNegative() || limite.isZero()) throw new IllegalArgumentException("Prazo inválido.");
        this.pergunta = pergunta;
        this.jogadores = new LinkedHashSet<>(jogadores);
        this.conclusoes = new CountDownLatch(this.jogadores.size());
        this.relogio = relogio;
        this.inicio = relogio.getAsLong();
        this.limiteNanos = limite.toNanos();
    }

    // Verificar e inserir formam uma única secção crítica: uma resposta por jogador e rodada.
    public synchronized boolean responder(Jogador jogador, int perguntaId, String alternativa) {
        long decorrido = Math.max(0, relogio.getAsLong() - inicio);
        if (encerrada || perguntaId != pergunta.identificador() || !jogadores.contains(jogador)
                || respostas.containsKey(jogador) || decorrido >= limiteNanos) return false;
        if (alternativa == null || !alternativa.matches("(?i)[ABCD]")) throw new IllegalArgumentException("Alternativa inválida.");
        respostas.put(jogador, Resposta.avaliar(jogador, pergunta, alternativa, decorrido / 1_000_000_000.0, false));
        conclusoes.countDown();
        return true;
    }

    public synchronized void desistir(Jogador jogador) {
        if (!encerrada && jogadores.contains(jogador) && !respostas.containsKey(jogador)) {
            respostas.put(jogador, timeout(jogador));
            conclusoes.countDown();
        }
    }

    public void aguardar() throws InterruptedException {
        // Não se reinicia o prazo a cada resposta nem se espera indefinidamente.
        conclusoes.await(Math.max(0, limiteNanos - (relogio.getAsLong() - inicio)), TimeUnit.NANOSECONDS);
    }

    public synchronized Map<Jogador, Resposta> encerrar() {
        encerrada = true;
        for (Jogador jogador : jogadores) respostas.putIfAbsent(jogador, timeout(jogador));
        return Map.copyOf(respostas);
    }
    private Resposta timeout(Jogador jogador) {
        return Resposta.avaliar(jogador, pergunta, "", limiteNanos / 1_000_000_000.0, true);
    }
    public int getPerguntaId() { return pergunta.identificador(); }
    public long getPendentes() { return conclusoes.getCount(); }
}
