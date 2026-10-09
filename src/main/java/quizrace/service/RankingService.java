package quizrace.service;

import quizrace.model.Jogador;
import java.util.*;

public final class RankingService {
    public List<Jogador> ordenar(List<Jogador> jogadores) {
        return jogadores.stream().sorted(Comparator.comparingDouble(Jogador::getPontuacao).reversed()
                .thenComparingDouble(Jogador::getTempoTotal)
                .thenComparing(Comparator.comparingInt(Jogador::getRespostasCorrectas).reversed())
                .thenComparing(Jogador::getNome)).toList();
    }
}
