import { useEffect, useState } from "react";
import { ErroApi, pedir } from "../api";
import type { PaginaTeste } from "../testes";
import { alternativas } from "../testes";
type Question = {
  numero: number;
  enunciado: string;
  alternativaA: string;
  alternativaB: string;
  alternativaC: string;
  alternativaD: string;
  categoria: string;
  tipo: "MULTIPLA_ESCOLHA" | "VERDADEIRO_FALSO";
};
type Quiz = {
  id: string;
  versao: number;
  titulo: string;
  descricao: string;
  perguntas: Question[];
};
type Resumo = {
  id: string;
  titulo: string;
  totalPerguntas: number;
  versao: number;
};
type Resultado = {
  titulo: string;
  totalPerguntas: number;
  correctas: number;
  notaPercentual: number;
  feedback: {
    numeroPergunta: number;
    correcta: boolean;
    respostaCorrecta: string;
    tipo: string;
  }[];
};
type Tentativa = {
  id: string;
  titulo: string;
  totalPerguntas: number;
  correctas: number;
  notaPercentual: number;
  realizadaEm: string;
};
export function QuizPratica({
  token,
  falha,
}: {
  token: string;
  falha: (e: unknown) => void;
}) {
  const [pagina, setPagina] = useState<PaginaTeste<Resumo> | null>(null),
    [tentativas, setTentativas] = useState<Tentativa[]>([]),
    [quiz, setQuiz] = useState<Quiz | null>(null),
    [resultado, setResultado] = useState<Resultado | null>(null),
    [selecao, setSelecao] = useState<Record<number, string>>({}),
    [erro, setErro] = useState(""),
    [ocupado, setOcupado] = useState(true);
  async function carregar() {
    setOcupado(true);
    setErro("");
    try {
      const [quizzes, historico] = await Promise.all([
        pedir<PaginaTeste<Resumo>>("/quizzes/pratica", token),
        pedir<PaginaTeste<Tentativa>>("/quizzes/pratica/historico", token),
      ]);
      setPagina(quizzes);
      setTentativas(historico.content);
    } catch (e) {
      if (e instanceof ErroApi && e.status === 401) falha(e);
      else
        setErro(
          e instanceof Error
            ? e.message
            : "Não foi possível carregar os quizzes.",
        );
    } finally {
      setOcupado(false);
    }
  }
  async function actualizarHistorico() {
    try {
      const historico = await pedir<PaginaTeste<Tentativa>>(
        "/quizzes/pratica/historico",
        token,
      );
      setTentativas(historico.content);
    } catch (e) {
      if (e instanceof ErroApi && e.status === 401) falha(e);
      else
        setErro(
          e instanceof Error
            ? e.message
            : "Não foi possível actualizar o histórico.",
        );
    }
  }
  useEffect(() => {
    void carregar();
  }, [token]);
  if (quiz)
    return (
      <>
        <button
          className="text-button"
          onClick={() => {
            setQuiz(null);
            setResultado(null);
            setSelecao({});
          }}
        >
          ← Quizzes disponíveis
        </button>
        <div className="page-heading">
          <div>
            <p className="eyebrow">MODO DE PREPARAÇÃO</p>
            <h1>{quiz.titulo}</h1>
            <p>{quiz.descricao || "Responda e veja a correcção no final."}</p>
          </div>
        </div>
        {!resultado ? (
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              if (Object.keys(selecao).length !== quiz.perguntas.length) {
                setErro("Responda a todas as perguntas antes de concluir.");
                return;
              }
              setOcupado(true);
              setErro("");
              try {
                const correcao = await pedir<Resultado>(
                  "/quizzes/pratica/" + quiz.id + "/respostas",
                  token,
                  "POST",
                  {
                    versao: quiz.versao,
                    respostas: quiz.perguntas.map((q) => ({
                      numeroPergunta: q.numero,
                      resposta: selecao[q.numero],
                    })),
                  },
                );
                setResultado(correcao);
                void actualizarHistorico();
              } catch (err) {
                if (err instanceof ErroApi && err.status === 401) falha(err);
                else
                  setErro(
                    err instanceof Error
                      ? err.message
                      : "Não foi possível corrigir o quiz.",
                  );
              } finally {
                setOcupado(false);
              }
            }}
          >
            {quiz.perguntas.map((q) => {
              const opts =
                q.tipo === "VERDADEIRO_FALSO" ? ["A", "B"] : alternativas;
              return (
                <fieldset key={q.numero} className="card practice-question">
                  <legend>
                    Pergunta {q.numero}
                    {q.categoria ? " · " + q.categoria : ""}
                  </legend>
                  <h2>{q.enunciado}</h2>
                  {opts.map((l) => {
                    const texto =
                      q[
                        `alternativa${l}` as
                          | "alternativaA"
                          | "alternativaB"
                          | "alternativaC"
                          | "alternativaD"
                      ];
                    return (
                      <label className="radio-opcao" key={l}>
                        <input
                          required
                          type="radio"
                          name={`pergunta-${q.numero}`}
                          value={l}
                          checked={selecao[q.numero] === l}
                          onChange={() =>
                            setSelecao((s) => ({ ...s, [q.numero]: l }))
                          }
                        />
                        <b>{l}</b>
                        {texto}
                      </label>
                    );
                  })}
                </fieldset>
              );
            })}
            {erro && (
              <div role="alert" className="notice error">
                {erro}
              </div>
            )}
            <button className="primary" disabled={ocupado}>
              {ocupado ? "A corrigir…" : "Concluir e corrigir quiz"}
            </button>
          </form>
        ) : (
          <section className="card">
            <p className="eyebrow">RESULTADO DA PREPARAÇÃO</p>
            <h2>
              {resultado.correctas} de {resultado.totalPerguntas} correctas ·{" "}
              {resultado.notaPercentual}%
            </h2>
            {resultado.feedback.map((f) => (
              <p key={f.numeroPergunta}>
                Pergunta {f.numeroPergunta}:{" "}
                {f.correcta ? "Correcta" : "Reveja a resposta"}. Resposta:{" "}
                {f.respostaCorrecta}.
              </p>
            ))}
            <button
              className="primary"
              onClick={() => {
                setResultado(null);
                setSelecao({});
              }}
            >
              Tentar novamente
            </button>
          </section>
        )}
      </>
    );
  return (
    <>
      <div className="page-heading">
        <div>
          <p className="eyebrow">PARA ESTUDANTES</p>
          <h1>Preparar com quizzes</h1>
          <p>
            Pratique com os quizzes disponibilizados pelos docentes. A nota de
            treino não é uma nota de avaliação.
          </p>
        </div>
        <button
          className="secondary"
          disabled={ocupado}
          onClick={() => void carregar()}
        >
          Actualizar ↻
        </button>
      </div>
      {erro && (
        <div className="notice error" role="alert">
          {erro}
        </div>
      )}
      {ocupado ? (
        <p role="status">A carregar quizzes…</p>
      ) : pagina?.content.length ? (
        <div className="cards">
          {pagina.content.map((q) => (
            <section className="card" key={q.id}>
              <span className="status active">Quiz de preparação</span>
              <h2>{q.titulo}</h2>
              <p>
                {q.totalPerguntas} perguntas · Versão {q.versao + 1}
              </p>
              <button
                className="primary"
                onClick={async () => {
                  setOcupado(true);
                  try {
                    setQuiz(await pedir("/quizzes/pratica/" + q.id, token));
                  } catch (e) {
                    if (e instanceof ErroApi && e.status === 401) falha(e);
                    else
                      setErro(
                        e instanceof Error
                          ? e.message
                          : "Não foi possível abrir o quiz.",
                      );
                  } finally {
                    setOcupado(false);
                  }
                }}
              >
                Começar quiz →
              </button>
            </section>
          ))}
        </div>
      ) : (
        <div className="card empty">
          Ainda não há quizzes de preparação disponíveis. Peça ao docente para
          disponibilizar um.
        </div>
      )}
      <section className="practice-history">
        <div className="section-heading">
          <h2>Histórico de preparação</h2>
          <button
            className="text-button"
            disabled={ocupado}
            onClick={() => void actualizarHistorico()}
          >
            Actualizar histórico ↻
          </button>
        </div>
        {!tentativas.length ? (
          <div className="card empty">
            Ainda não existem tentativas guardadas.
          </div>
        ) : (
          <div className="cards">
            {tentativas.map((tentativa) => (
              <article className="card" key={tentativa.id}>
                <p className="eyebrow">
                  {new Date(tentativa.realizadaEm).toLocaleString("pt-PT", {
                    dateStyle: "short",
                    timeStyle: "short",
                  })}
                </p>
                <h3>{tentativa.titulo}</h3>
                <p>
                  {tentativa.correctas}/{tentativa.totalPerguntas} correctas ·{" "}
                  {tentativa.notaPercentual.toLocaleString("pt-PT", {
                    maximumFractionDigits: 1,
                  })}
                  %
                </p>
              </article>
            ))}
          </div>
        )}
      </section>
    </>
  );
}
