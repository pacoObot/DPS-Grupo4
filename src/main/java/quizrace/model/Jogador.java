package quizrace.model;

public final class Jogador {
    private final String nome;
    private double pontuacao;
    private double tempoTotal;
    private double tempoUltimaResposta;
    private int respostasCorrectas;
    private int respostasErradas;

    public Jogador(String nome) {
        if (nome == null || nome.isBlank()) throw new IllegalArgumentException("Nome obrigatório.");
        this.nome = nome.trim();
    }

    // Na segunda etapa, cada tarefa será dona de um jogador; o ranking será lido após a conclusão.
    public void registarResposta(boolean correcta, double pontos, double tempoSegundos) {
        if (!Double.isFinite(pontos) || pontos < 0 || (!correcta && pontos != 0)
                || !Double.isFinite(tempoSegundos) || tempoSegundos < 0) {
            throw new IllegalArgumentException("Resultado inválido.");
        }
        pontuacao += pontos;
        tempoUltimaResposta = tempoSegundos;
        tempoTotal += tempoSegundos;
        if (correcta) respostasCorrectas++; else respostasErradas++;
    }

    public String getNome() { return nome; }
    public double getPontuacao() { return pontuacao; }
    public double getTempoTotal() { return tempoTotal; }
    public double getTempoUltimaResposta() { return tempoUltimaResposta; }
    public int getRespostasCorrectas() { return respostasCorrectas; }
    public int getRespostasErradas() { return respostasErradas; }
}
