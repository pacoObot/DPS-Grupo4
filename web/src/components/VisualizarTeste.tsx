import { useState } from "react";
import { alternativas } from "../testes";
import type { Questao } from "../testes";

export function VisualizarTeste({
  titulo,
  descricao,
  disponivelPratica = false,
  perguntas,
}: {
  titulo: string;
  descricao: string;
  disponivelPratica?: boolean;
  perguntas: Questao[];
}) {
  const [gabarito, setGabarito] = useState(false);
  return (
    <section className="preview-teste" aria-label="Pré-visualização do teste">
      <div className="notice">
        Pré-visualização para o docente.{" "}
        {disponivelPratica
          ? "O quiz de preparação está disponível aos estudantes."
          : "O gabarito não é partilhado com os estudantes."}
      </div>
      <h2>{titulo || "Teste sem título"}</h2>
      <p className="texto-multilinha">{descricao}</p>
      <label className="check-label">
        <input
          type="checkbox"
          checked={gabarito}
          onChange={(e) => setGabarito(e.target.checked)}
        />{" "}
        Mostrar respostas correctas
      </label>
      {perguntas.length === 0 && (
        <p className="empty">Ainda não foram adicionadas perguntas.</p>
      )}
      {perguntas.map((q, i) => (
        <section key={i} className="card question-preview">
          <p className="eyebrow">
            PERGUNTA {i + 1}
            {q.categoria ? " · " + q.categoria : ""}
          </p>
          <h3 className="texto-multilinha">
            {q.enunciado || "Enunciado por preencher"}
          </h3>
          <ol className="opcoes-preview">
            {(q.tipo === "VERDADEIRO_FALSO"
              ? alternativas.slice(0, 2)
              : alternativas
            ).map((letra) => (
              <li
                key={letra}
                className={
                  gabarito && q.correcta === letra ? "opcao-correcta" : ""
                }
              >
                <b>{letra}</b>
                <span>
                  {q[`alternativa${letra}`] || "Alternativa por preencher"}
                </span>
                {gabarito && q.correcta === letra && (
                  <strong>✓ Correcta</strong>
                )}
              </li>
            ))}
          </ol>
        </section>
      ))}
    </section>
  );
}
