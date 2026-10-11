package quiz.domain.entity;

/**
 * Pontuação de uma equipa.
 * Entidade de domínio pura: nenhuma tecnologia externa (JDBC, Socket, JavaFX).
 *
 * CONCORRÊNCIA: os métodos de mutação são synchronized porque esta entidade
 * é atualizada por múltiplas threads (uma por cliente) em simultâneo.
 * Em conjunto com o SELECT ... FOR UPDATE no repositório, elimina "lost updates".
 */
public class Score {

    private int points;
    private int correctCount;
    private int wrongCount;

    public Score() {
    }

    public Score(int points, int correctCount, int wrongCount) {
        this.points = points;
        this.correctCount = correctCount;
        this.wrongCount = wrongCount;
    }

    /**
     * Fórmula de pontuação (documentada na spec):
     *   pontos = 10 + bônus de velocidade
     *   bônus  = round(10 * tempo_restante / tempo_total)   -> 0..10
     * Resposta correta rápida vale até 20 pontos.
     */
    public static int calculate(int basePoints, long remainingMillis, long totalMillis) {
        if (basePoints <= 0) basePoints = 10;
        if (totalMillis <= 0 || remainingMillis <= 0) return basePoints;
        double ratio = Math.min(1.0, (double) remainingMillis / totalMillis);
        return basePoints + (int) Math.round(10 * ratio);
    }

    public synchronized void addCorrect(int earned) {
        points += earned;
        correctCount++;
    }

    public synchronized void addWrong() {
        wrongCount++;
    }

    public synchronized int getPoints() { return points; }
    public synchronized int getCorrectCount() { return correctCount; }
    public synchronized int getWrongCount() { return wrongCount; }

    @Override
    public synchronized String toString() {
        return points + " pts (ok=" + correctCount + ", err=" + wrongCount + ")";
    }
}
