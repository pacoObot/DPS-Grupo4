package quizrace.server;

import quizrace.model.*;
import quizrace.network.ProtocoloMensagem;
import quizrace.service.RankingService;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import static quizrace.network.ProtocoloMensagem.Tipo.*;

public final class SalaQuiz implements AutoCloseable {
    public static final int MINIMO = 2;
    public static final int MAXIMO = 4;
    private final Map<Jogador, CanalJogador> participantes = new LinkedHashMap<>();
    private final List<Pergunta> perguntas;
    private final Duration prazo;
    private final Duration intervaloContagem;
    private final ScheduledExecutorService coordenador = Executors.newSingleThreadScheduledExecutor();
    private EstadoSala estado = EstadoSala.AGUARDANDO;
    private ScheduledFuture<?> contagem;
    private volatile Rodada rodada;
    private volatile List<Jogador> ranking = List.of();
    private boolean fechada;

    public SalaQuiz(List<Pergunta> perguntas) {
        this(perguntas, Duration.ofSeconds(Pergunta.LIMITE_SEGUNDOS), Duration.ofSeconds(1));
    }
    public SalaQuiz(List<Pergunta> perguntas, Duration prazo, Duration intervaloContagem) {
        if (perguntas.isEmpty() || prazo.isNegative() || prazo.isZero()
                || prazo.compareTo(Duration.ofSeconds(15)) > 0 || intervaloContagem.isNegative())
            throw new IllegalArgumentException("Configuração de sala inválida.");
        if (perguntas.stream().anyMatch(p -> p.identificador() <= 0)
                || perguntas.stream().map(Pergunta::identificador).distinct().count() != perguntas.size())
            throw new IllegalArgumentException("As perguntas da sala precisam de identificadores únicos positivos.");
        this.perguntas = List.copyOf(perguntas);
        this.prazo = prazo;
        this.intervaloContagem = intervaloContagem;
    }

    public synchronized void entrar(Jogador jogador, CanalJogador canal) {
        if (fechada || estado == EstadoSala.EM_JOGO || estado == EstadoSala.FINALIZADO)
            throw new IllegalStateException("Sala em jogo ou finalizada.");
        if (participantes.size() >= MAXIMO) throw new IllegalStateException("Sala cheia.");
        if (participantes.keySet().stream().anyMatch(j -> j.getNome().equalsIgnoreCase(jogador.getNome())))
            throw new IllegalArgumentException("Nome já utilizado.");
        participantes.put(jogador, canal);
        canal.enviarMensagem(ProtocoloMensagem.construir(MENSAGEM, "Bem-vindo, " + jogador.getNome() + "!"));
        distribuir(ENTROU, jogador.getNome());
        distribuir(JOGADORES, String.valueOf(participantes.size()), String.valueOf(MAXIMO));
        canal.enviarMensagem(ProtocoloMensagem.construir(AGUARDAR, "Aguardando o início da partida."));
        System.out.println("[JOGADOR] " + jogador.getNome() + " entrou na sala");
        System.out.println("[SALA] " + participantes.size() + "/" + MAXIMO + " jogadores");
        if (podeComecar() && estado == EstadoSala.AGUARDANDO) {
            estado = EstadoSala.INICIANDO;
            agendar(3);
        }
    }

    private void agendar(int numero) {
        contagem = coordenador.schedule(() -> contar(numero), intervaloContagem.toNanos(), TimeUnit.NANOSECONDS);
    }
    private void contar(int numero) {
        synchronized (this) {
            if (fechada || estado != EstadoSala.INICIANDO || !podeComecar()) return;
            if (numero > 0) { distribuir(INICIO, String.valueOf(numero)); agendar(numero - 1); return; }
            estado = EstadoSala.EM_JOGO;
        }
        executarPartida();
    }

    public synchronized void sair(Jogador jogador) {
        jogador.desconectar();
        if (estado == EstadoSala.AGUARDANDO || estado == EstadoSala.INICIANDO) {
            participantes.remove(jogador);
            if (participantes.size() < MINIMO) {
                if (contagem != null) contagem.cancel(false);
                estado = EstadoSala.AGUARDANDO;
            }
            distribuir(JOGADORES, String.valueOf(participantes.size()), String.valueOf(MAXIMO));
        }
        Rodada actual = rodada;
        if (actual != null) actual.desistir(jogador);
    }

    public boolean responder(Jogador jogador, ProtocoloMensagem.Mensagem mensagem) {
        Rodada actual = rodada;
        if (actual == null || mensagem.tipo() != RESPOSTA) return false;
        // O formato antigo é aceite só na primeira pergunta: sem ID não há como distinguir respostas tardias.
        int id = mensagem.campos().size() == 2 ? Integer.parseInt(mensagem.campo(0)) : perguntas.get(0).identificador();
        boolean aceite = actual.responder(jogador, id, mensagem.campo(mensagem.campos().size() - 1));
        if (aceite) System.out.println("[RESPOSTA] " + jogador.getNome() + " respondeu à pergunta " + id);
        return aceite;
    }

    private void executarPartida() {
        var inscritos = participantes();
        try {
            distribuir(INICIO, "GO");
            System.out.println("[JOGO] Iniciando partida");
            for (Pergunta pergunta : perguntas) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
                distribuir(PERGUNTA, String.valueOf(pergunta.identificador()), pergunta.enunciado());
                for (int i = 0; i < 4; i++) distribuir(OPCAO, String.valueOf((char) ('A' + i)), pergunta.alternativas().get(i));
                Rodada actual = new Rodada(pergunta, new ArrayList<>(inscritos.keySet()), prazo);
                rodada = actual;
                for (Jogador jogador : inscritos.keySet()) if (!jogador.isConectado()) actual.desistir(jogador);
                distribuir(TEMPO, String.valueOf(Math.max(1, prazo.toSeconds())));
                System.out.println("[PERGUNTA] " + pergunta.identificador() + "/" + perguntas.size());
                actual.aguardar();
                var respostas = actual.encerrar();
                rodada = null;
                // Só este coordenador altera pontuações; handlers apenas entregam respostas à rodada.
                for (var inscrito : inscritos.entrySet()) {
                    Jogador jogador = inscrito.getKey();
                    Resposta resposta = respostas.get(jogador);
                    jogador.registarResposta(resposta);
                    double pontos = new quizrace.service.CalculadoraPontuacao().calcular(resposta.correcta(), resposta.tempoSegundos());
                    String resultado = resposta.tempoEsgotado() ? "TIMEOUT" : resposta.correcta() ? "CORRECTO" : "ERRADO";
                    inscrito.getValue().enviarMensagem(ProtocoloMensagem.construir(RESULTADO, resultado, numero(pontos)));
                    inscrito.getValue().enviarMensagem(ProtocoloMensagem.construir(PONTUACAO, numero(jogador.getPontuacao())));
                    System.out.printf(Locale.ROOT, "[RESULTADO] %s +%.2f em %.3fs (%s)%n", jogador.getNome(), pontos, resposta.tempoSegundos(), resultado);
                }
            }
            ranking = new RankingService().ordenar(new ArrayList<>(inscritos.keySet()));
            synchronized (this) { estado = EstadoSala.FINALIZADO; }
            int posicao = 1;
            for (Jogador jogador : ranking) distribuir(RANKING, String.valueOf(posicao++), jogador.getNome(),
                    numero(jogador.getPontuacao()), String.valueOf(jogador.getRespostasCorrectas()),
                    String.valueOf(jogador.getRespostasErradas()), String.valueOf(jogador.getTimeouts()), numero(jogador.getTempoTotal()));
            Jogador vencedor = ranking.get(0);
            distribuir(VENCEDOR, vencedor.getNome(), numero(vencedor.getPontuacao()));
            System.out.println("[FIM] Vencedor: " + vencedor.getNome());
            distribuir(FIM, "Partida terminada");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            rodada = null;
            synchronized (this) { estado = EstadoSala.FINALIZADO; }
            inscritos.values().forEach(CanalJogador::fechar);
        }
    }

    public synchronized boolean podeComecar() { return participantes.size() >= MINIMO; }
    public synchronized EstadoSala getEstado() { return estado; }
    public synchronized int getNumeroJogadores() { return participantes.size(); }
    public List<Jogador> getRanking() { return ranking; }
    private synchronized Map<Jogador, CanalJogador> participantes() { return new LinkedHashMap<>(participantes); }
    private void distribuir(ProtocoloMensagem.Tipo tipo, String... campos) {
        String mensagem = ProtocoloMensagem.construir(tipo, campos);
        participantes().values().forEach(c -> c.enviarMensagem(mensagem));
    }
    private static String numero(double valor) { return String.format(Locale.ROOT, "%.2f", valor); }

    @Override public void close() {
        synchronized (this) {
            if (fechada) return;
            fechada = true;
            estado = EstadoSala.FINALIZADO;
        }
        coordenador.shutdownNow();
        participantes().values().forEach(CanalJogador::fechar);
        try { coordenador.awaitTermination(3, TimeUnit.SECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
