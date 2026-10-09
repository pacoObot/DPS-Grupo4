package quizrace.service;

import quizrace.model.Pergunta;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class GerenciadorPerguntas {
    public List<Pergunta> carregar() throws IOException {
        InputStream recurso = getClass().getResourceAsStream("/perguntas.txt");
        if (recurso == null) throw new IOException("Recurso perguntas.txt não encontrado.");
        List<Pergunta> perguntas = new ArrayList<>();
        try (BufferedReader leitor = new BufferedReader(new InputStreamReader(recurso, StandardCharsets.UTF_8))) {
            String linha;
            int numero = 0;
            while ((linha = leitor.readLine()) != null) {
                numero++;
                if (linha.isBlank() || linha.startsWith("#")) continue;
                String[] campos = linha.split("\\|", -1);
                if (campos.length != 7 || campos[5].length() != 1) throw new IOException("Pergunta inválida na linha " + numero);
                try {
                    perguntas.add(new Pergunta(campos[0], List.of(campos[1], campos[2], campos[3], campos[4]), campos[5].charAt(0), campos[6], perguntas.size() + 1));
                } catch (IllegalArgumentException e) {
                    throw new IOException("Pergunta inválida na linha " + numero, e);
                }
            }
        }
        if (perguntas.isEmpty()) throw new IOException("Não existem perguntas.");
        return List.copyOf(perguntas);
    }
}
