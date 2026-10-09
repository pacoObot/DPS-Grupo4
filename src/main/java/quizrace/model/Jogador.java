package quizrace.model;

public final class Jogador {
    private final String nome;
    private double pontuacao;
    private double tempoTotal;
    private double tempoUltimaResposta;
    private int respostasCorrectas;
    private int respostasErradas;
    private int timeouts;
    private volatile boolean conectado = true;

    public Jogador(String nome) {
        if (nome == null || nome.isBlank() || nome.trim().length() > 40
                || nome.chars().anyMatch(c -> Character.isISOControl(c) || c == '|')) throw new IllegalArgumentException("Nome obrigatório.");
        this.nome = nome.trim();
    }

    // Apenas o coordenador da sala (ou a tarefa dona deste jogador) altera os resultados.
    public void registarResposta(boolean correcta, double pontos, double tempoSegundos) {
        if (!Double.isFinite(pontos) || pontos < 0 || pontos > 100 || (!correcta && pontos != 0)
                || !Double.isFinite(tempoSegundos) || tempoSegundos < 0) {
            throw new IllegalArgumentException("Resultado inválido.");
        }
        pontuacao += pontos;
        tempoUltimaResposta = tempoSegundos;
        tempoTotal += tempoSegundos;
        if (correcta) respostasCorrectas++; else respostasErradas++;
    }

    public void registarResposta(Resposta resposta) {
        if (resposta.tempoEsgotado()) registrarTimeout(resposta.tempoSegundos());
        else registarResposta(resposta.correcta(),
                new quizrace.service.CalculadoraPontuacao().calcular(resposta.correcta(), resposta.tempoSegundos()),
                resposta.tempoSegundos());
    }

    public void registrarRespostaCorreta(double pontos, double tempo) { registarResposta(true, pontos, tempo); }
    public void registrarRespostaErrada(double tempo) { registarResposta(false, 0, tempo); }
    public void registrarTimeout(double tempo) {
        registarResposta(false, 0, tempo);
        timeouts++;
    }
    public int getTimeouts() { return timeouts; }
    public boolean isConectado() { return conectado; }
    public void desconectar() { conectado = false; }

    public String getNome() { return nome; }
    public double getPontuacao() { return pontuacao; }
    public double getTempoTotal() { return tempoTotal; }
    public double getTempoUltimaResposta() { return tempoUltimaResposta; }
    public int getRespostasCorrectas() { return respostasCorrectas; }
    public int getRespostasErradas() { return respostasErradas; }
}
