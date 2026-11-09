import { useEffect, useState } from "react";
import { ErroApi, pedir } from "../api";
import { alternativas, dataHora } from "../testes";
import type { Alternativa, Questao, Teste, TipoQuestao } from "../testes";
import { VisualizarTeste } from "./VisualizarTeste";
import { HistoricoTeste } from "./HistoricoTeste";

type QuestaoEditavel = Questao & { chave: string };
const editavel = (q: Questao): QuestaoEditavel => ({
  ...q,
  tipo: q.tipo ?? "MULTIPLA_ESCOLHA",
  chave: crypto.randomUUID(),
});
const nova = (): QuestaoEditavel =>
  editavel({
    enunciado: "",
    alternativaA: "",
    alternativaB: "",
    alternativaC: "",
    alternativaD: "",
    correcta: "A",
    categoria: "",
    tipo: "MULTIPLA_ESCOLHA",
  });

export function EditorTeste({
  inicial,
  token,
  voltar,
  falha,
  mudou,
}: {
  inicial: Teste | null;
  token: string;
  voltar: () => void;
  falha: (e: unknown) => void;
  mudou: (valor: boolean) => void;
}) {
  const [guardado, setGuardado] = useState(inicial);
  const [titulo, setTitulo] = useState(inicial?.titulo || "");
  const [descricao, setDescricao] = useState(inicial?.descricao || "");
  const [disponivelPratica, setDisponivelPratica] = useState(
    inicial?.disponivelPratica ?? false,
  );
  const [perguntas, setPerguntas] = useState<QuestaoEditavel[]>(
    () => inicial?.perguntas.map(editavel) || [],
  );
  const [alterado, setAlterado] = useState(false);
  const [ocupado, setOcupado] = useState(false);
  const [erro, setErro] = useState("");
  const [sucesso, setSucesso] = useState("");
  const [preview, setPreview] = useState(false);
  const [historico, setHistorico] = useState(false);
  const [conflito, setConflito] = useState(false);
  function marcar() {
    setAlterado(true);
    mudou(true);
    setSucesso("");
  }
  useEffect(() => {
    if (!alterado) return;
    const avisar = (e: BeforeUnloadEvent) => {
      e.preventDefault();
      e.returnValue = "";
    };
    window.addEventListener("beforeunload", avisar);
    return () => window.removeEventListener("beforeunload", avisar);
  }, [alterado]);
  function sair() {
    if (
      ocupado ||
      (alterado &&
        !window.confirm("Sair do editor e perder as alterações não guardadas?"))
    )
      return;
    mudou(false);
    voltar();
  }
  function questao(chave: string, campo: keyof Questao, valor: string) {
    marcar();
    setPerguntas((p) =>
      p.map((q) => (q.chave === chave ? { ...q, [campo]: valor } : q)),
    );
  }
  function mover(indice: number, deslocamento: number) {
    marcar();
    setPerguntas((p) => {
      const novaOrdem = [...p];
      [novaOrdem[indice], novaOrdem[indice + deslocamento]] = [
        novaOrdem[indice + deslocamento],
        novaOrdem[indice],
      ];
      return novaOrdem;
    });
  }
  async function recarregar() {
    if (
      !guardado ||
      !window.confirm(
        "Carregar a versão guardada e descartar as alterações deste editor?",
      )
    )
      return;
    setOcupado(true);
    setErro("");
    try {
      const t = await pedir<Teste>("/testes/" + guardado.id, token);
      setGuardado(t);
      setTitulo(t.titulo);
      setDescricao(t.descricao);
      setDisponivelPratica(t.disponivelPratica);
      setPerguntas(t.perguntas.map(editavel));
      setAlterado(false);
      mudou(false);
      setConflito(false);
    } catch (e) {
      tratar(e);
    } finally {
      setOcupado(false);
    }
  }
  function tratar(e: unknown) {
    if (e instanceof ErroApi && e.status === 401) {
      falha(e);
      return;
    }
    setErro(
      e instanceof Error ? e.message : "Não foi possível guardar o teste.",
    );
    if (e instanceof ErroApi && e.status === 409) setConflito(true);
  }
  return (
    <>
      <div className="editor-heading">
        <button className="text-button" disabled={ocupado} onClick={sair}>
          ← Os meus testes
        </button>
        <span className="pill">
          Rascunho · {alterado ? "alterações por guardar" : "em preparação"}
        </span>
      </div>
      <div className="page-heading">
        <div>
          <p className="eyebrow">PREPARAÇÃO DO TESTE</p>
          <h1>{guardado ? "Editar teste" : "Novo teste"}</h1>
          <p>Defina o conteúdo e a ordem das perguntas.</p>
        </div>
      </div>
      <div className="editor-toolbar">
        <button
          className="secondary"
          disabled={ocupado}
          onClick={() => setPreview((p) => !p)}
        >
          {preview ? "Voltar à edição" : "Pré-visualizar"}
        </button>
        {guardado && (
          <button
            className="secondary"
            disabled={ocupado}
            onClick={() => setHistorico(true)}
          >
            Histórico de versões
          </button>
        )}
        <span className="muted">{perguntas.length}/50 perguntas</span>
      </div>
      {erro && (
        <div role="alert" className="notice error">
          {erro}
        </div>
      )}
      {conflito && (
        <button className="secondary" disabled={ocupado} onClick={recarregar}>
          Carregar versão guardada
        </button>
      )}
      {sucesso && (
        <div role="status" className="notice success">
          {sucesso}
        </div>
      )}
      {guardado && (
        <p className="save-info">
          Versão {guardado.versao + 1} · Última gravação:{" "}
          {dataHora(guardado.actualizadoEm)}
        </p>
      )}
      {preview ? (
        <VisualizarTeste
          disponivelPratica={disponivelPratica}
          titulo={titulo}
          descricao={descricao}
          perguntas={perguntas}
        />
      ) : (
        <form
          onSubmit={async (e) => {
            e.preventDefault();
            if (ocupado || conflito) return;
            setOcupado(true);
            setErro("");
            setSucesso("");
            try {
              const payload = {
                titulo,
                descricao,
                disponivelPratica,
                ...(guardado ? { versao: guardado.versao } : {}),
                perguntas: perguntas.map(({ chave: _chave, ...q }) => q),
              };
              const t = await pedir<Teste>(
                guardado ? "/testes/" + guardado.id : "/testes",
                token,
                guardado ? "PUT" : "POST",
                payload,
              );
              setGuardado(t);
              setTitulo(t.titulo);
              setDescricao(t.descricao);
              setDisponivelPratica(t.disponivelPratica);
              setPerguntas(t.perguntas.map(editavel));
              setAlterado(false);
              mudou(false);
              setSucesso(
                "Teste guardado. Pode continuar a preparar as perguntas.",
              );
            } catch (err) {
              tratar(err);
            } finally {
              setOcupado(false);
            }
          }}
        >
          <fieldset className="editor-fields" disabled={ocupado}>
            <section className="card test-details">
              <h2>Informação do teste</h2>
              <label className="check-label">
                <input
                  type="checkbox"
                  checked={disponivelPratica}
                  onChange={(e) => {
                    marcar();
                    setDisponivelPratica(e.target.checked);
                  }}
                />
                Disponibilizar aos estudantes como quiz de preparação
              </label>
              <label>
                Título
                <input
                  required
                  maxLength={150}
                  value={titulo}
                  onChange={(e) => {
                    marcar();
                    setTitulo(e.target.value);
                  }}
                  placeholder="Ex.: Teste 1 — Programação concorrente"
                />
              </label>
              <label>
                Instruções para os estudantes
                <textarea
                  maxLength={2000}
                  rows={3}
                  value={descricao}
                  onChange={(e) => {
                    marcar();
                    setDescricao(e.target.value);
                  }}
                  placeholder="Indique o tema e as instruções de resposta."
                />
              </label>
            </section>
            <div className="section-heading">
              <h2>Perguntas</h2>
              <span>
                Escolha múltipla ou verdadeiro/falso. Assinale a resposta
                correcta.
              </span>
            </div>
            {perguntas.length === 0 && (
              <div className="card empty">
                Adicione a primeira pergunta. Também pode guardar um rascunho
                apenas com o título.
              </div>
            )}
            {perguntas.map((q, i) => (
              <fieldset className="card question-editor" key={q.chave}>
                <legend>Pergunta {i + 1}</legend>
                <div className="question-actions">
                  <button
                    type="button"
                    className="secondary"
                    disabled={i === 0}
                    onClick={() => mover(i, -1)}
                    aria-label={`Subir pergunta ${i + 1}`}
                  >
                    ↑ Subir
                  </button>
                  <button
                    type="button"
                    className="secondary"
                    disabled={i === perguntas.length - 1}
                    onClick={() => mover(i, 1)}
                    aria-label={`Descer pergunta ${i + 1}`}
                  >
                    ↓ Descer
                  </button>
                  <button
                    type="button"
                    className="text-button danger"
                    onClick={() => {
                      if (
                        window.confirm(
                          `Remover a pergunta ${i + 1} deste rascunho?`,
                        )
                      ) {
                        marcar();
                        setPerguntas((p) =>
                          p.filter((x) => x.chave !== q.chave),
                        );
                      }
                    }}
                  >
                    Remover pergunta {i + 1}
                  </button>
                </div>
                <label>
                  Tipo de pergunta
                  <select
                    value={q.tipo}
                    onChange={(e) => {
                      const tipo = e.target.value as TipoQuestao;
                      marcar();
                      setPerguntas((p) =>
                        p.map((x) =>
                          x.chave === q.chave
                            ? {
                                ...x,
                                tipo,
                                alternativaA:
                                  tipo === "VERDADEIRO_FALSO"
                                    ? "Verdadeiro"
                                    : x.alternativaA,
                                alternativaB:
                                  tipo === "VERDADEIRO_FALSO"
                                    ? "Falso"
                                    : x.alternativaB,
                                alternativaC:
                                  tipo === "VERDADEIRO_FALSO"
                                    ? ""
                                    : x.alternativaC,
                                alternativaD:
                                  tipo === "VERDADEIRO_FALSO"
                                    ? ""
                                    : x.alternativaD,
                                correcta:
                                  tipo === "VERDADEIRO_FALSO" &&
                                  x.correcta !== "A" &&
                                  x.correcta !== "B"
                                    ? "A"
                                    : x.correcta,
                              }
                            : x,
                        ),
                      );
                    }}
                  >
                    <option value="MULTIPLA_ESCOLHA">
                      Escolha múltipla · A, B, C e D
                    </option>
                    <option value="VERDADEIRO_FALSO">
                      Verdadeiro ou falso
                    </option>
                  </select>
                </label>
                <label>
                  Enunciado
                  <textarea
                    required
                    rows={3}
                    maxLength={2000}
                    value={q.enunciado}
                    onChange={(e) =>
                      questao(q.chave, "enunciado", e.target.value)
                    }
                  />
                </label>
                <div className="alternativas-grid">
                  {(q.tipo === "VERDADEIRO_FALSO"
                    ? alternativas.slice(0, 2)
                    : alternativas
                  ).map((a) => (
                    <label key={a}>
                      {q.tipo === "VERDADEIRO_FALSO"
                        ? a === "A"
                          ? "Verdadeiro"
                          : "Falso"
                        : `Alternativa ${a}`}
                      <input
                        required
                        maxLength={500}
                        value={q[`alternativa${a}`]}
                        readOnly={q.tipo === "VERDADEIRO_FALSO"}
                        onChange={(e) =>
                          questao(q.chave, `alternativa${a}`, e.target.value)
                        }
                      />
                    </label>
                  ))}
                </div>
                <div className="alternativas-grid">
                  <label>
                    Resposta correcta
                    <select
                      value={q.correcta}
                      onChange={(e) =>
                        questao(
                          q.chave,
                          "correcta",
                          e.target.value as Alternativa,
                        )
                      }
                    >
                      {(q.tipo === "VERDADEIRO_FALSO"
                        ? alternativas.slice(0, 2)
                        : alternativas
                      ).map((a) => (
                        <option key={a} value={a}>
                          {a}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    Categoria (opcional)
                    <input
                      maxLength={100}
                      value={q.categoria}
                      onChange={(e) =>
                        questao(q.chave, "categoria", e.target.value)
                      }
                    />
                  </label>
                </div>
              </fieldset>
            ))}
            <div className="editor-bottom">
              <button
                type="button"
                className="secondary"
                disabled={perguntas.length >= 50}
                onClick={() => {
                  marcar();
                  setPerguntas((p) => [...p, nova()]);
                }}
              >
                + Adicionar pergunta
              </button>
              <button
                className="primary"
                disabled={conflito || (!!guardado && !alterado)}
              >
                {ocupado ? "A guardar…" : "Guardar teste"}
              </button>
            </div>
          </fieldset>
        </form>
      )}
      <p className="help">
        Guardar conserva o rascunho e o histórico. A personalização da sala e a
        partilha por URL serão disponibilizadas na próxima fase.
      </p>
      {historico && guardado && (
        <HistoricoTeste
          id={guardado.id}
          token={token}
          fechar={() => setHistorico(false)}
          falha={falha}
        />
      )}
    </>
  );
}
