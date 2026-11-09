import { useEffect, useState } from "react";
import { ErroApi, pedir } from "../api";
import type { Sala } from "./SalasDocente";
type Aluno = {
  id: string;
  nome: string;
  identificacao: string;
  estado: string;
  respondidas: number;
  totalPerguntas: number;
  correctas: number;
  notaPercentual: number;
  dispositivo: "COMPUTADOR" | "TELEMOVEL";
  ligado: boolean;
  cursorX: number | null;
  cursorY: number | null;
};
export function MonitorSala({
  codigo,
  token,
  voltar,
  falha,
}: {
  codigo: string;
  token: string;
  voltar: () => void;
  falha: (e: unknown) => void;
}) {
  const [sala, setSala] = useState<Sala | null>(null),
    [alunos, setAlunos] = useState<Aluno[]>([]),
    [erro, setErro] = useState(""),
    [fechando, setFechando] = useState(false),
    [copiado, setCopiado] = useState(false);
  useEffect(() => {
    let busy = false;
    let alive = true;
    const atualizar = async () => {
      if (busy) return;
      busy = true;
      try {
        const [s, a] = await Promise.all([
          pedir<Sala>("/salas/" + codigo, token),
          pedir<Aluno[]>("/salas/" + codigo + "/participantes", token),
        ]);
        if (alive) {
          setSala(s);
          setAlunos(a);
          setErro("");
        }
      } catch (e) {
        if (alive && e instanceof ErroApi && e.status === 401) falha(e);
        else if (alive)
          setErro(
            e instanceof Error
              ? e.message
              : "Não foi possível actualizar o monitor.",
          );
      } finally {
        busy = false;
      }
    };
    void atualizar();
    const timer = window.setInterval(atualizar, 2000);
    return () => {
      alive = false;
      clearInterval(timer);
    };
  }, [codigo, token]);
  const url = window.location.origin + "/sala/" + codigo;
  if (!sala) return <div role="status">A carregar a sala…</div>;
  return (
    <>
      <div className="editor-heading">
        <button className="text-button" onClick={voltar}>
          ← Salas
        </button>
        <span className={"status " + (sala.aberta ? "active" : "inactive")}>
          {sala.aberta ? "Aberta" : "Encerrada"}
        </span>
      </div>
      <div className="page-heading">
        <div>
          <p className="eyebrow">PAINEL DA DOCENTE</p>
          <h1>{sala.nome}</h1>
          <p>
            {sala.tituloTeste} · {sala.totalPerguntas} perguntas ·{" "}
            {sala.segundosPorPergunta}s por pergunta
          </p>
        </div>
        {sala.aberta && (
          <button
            className="primary danger-fill"
            disabled={fechando}
            onClick={async () => {
              if (
                !window.confirm(
                  "Encerrar a sala para todos? Depois desta acção, os estudantes não podem enviar respostas.",
                )
              )
                return;
              setFechando(true);
              try {
                await pedir("/salas/" + codigo + "/fechar", token, "POST", {});
                const r = await pedir<Sala>("/salas/" + codigo, token);
                setSala(r);
              } catch (e) {
                setErro(
                  e instanceof Error
                    ? e.message
                    : "Não foi possível encerrar a sala.",
                );
              } finally {
                setFechando(false);
              }
            }}
          >
            {fechando ? "A encerrar…" : "Encerrar sessão"}
          </button>
        )}
      </div>
      {erro && (
        <div role="alert" className="notice error">
          {erro}
        </div>
      )}
      <section className="card share-card">
        <div>
          <p className="eyebrow">LINK DA SALA</p>
          <a href={url} target="_blank" rel="noreferrer">
            {url}
          </a>
          <p>
            Os estudantes pedem entrada; aprove cada nome e identificação antes
            de começar.
          </p>
        </div>
        <button
          className="secondary"
          onClick={async () => {
            try {
              await navigator.clipboard.writeText(url);
              setCopiado(true);
              setTimeout(() => setCopiado(false), 2500);
            } catch {
              window.prompt("Copie o link da sala:", url);
            }
          }}
        >
          {copiado ? "Link copiado ✓" : "Copiar link"}
        </button>
      </section>
      <div className="table-top monitor-heading">
        <h2>Participantes</h2>
        <span>
          {
            alunos.filter(
              (a) => a.estado === "APROVADO" || a.estado === "FECHADO",
            ).length
          }{" "}
          participantes · {alunos.filter((a) => a.estado === "PENDENTE").length}{" "}
          pendentes
        </span>
        <small>Actualiza a cada 2 segundos</small>
      </div>
      {!alunos.length ? (
        <div className="card empty">
          Ainda não chegaram pedidos de entrada. Partilhe o link com os
          estudantes.
        </div>
      ) : (
        <div className="cards monitor-grid">
          {alunos.map((a) => (
            <article className="card participant-card" key={a.id}>
              <div className="participant-heading">
                <span className="avatar">
                  {a.nome.slice(0, 1).toUpperCase()}
                </span>
                <div>
                  <strong>{a.nome}</strong>
                  <small>{a.identificacao}</small>
                </div>
                <span
                  className={"status " + (a.ligado ? "active" : "inactive")}
                >
                  {!sala.aberta
                    ? a.estado === "FECHADO"
                      ? "Concluiu"
                      : "Sessão encerrada"
                    : a.ligado
                      ? "Ligado"
                      : "Ausente"}
                </span>
              </div>
              <p>
                {a.dispositivo === "TELEMOVEL" ? "Telemóvel" : "Computador"} ·{" "}
                {a.respondidas}/{a.totalPerguntas} perguntas
              </p>
              <progress max={a.totalPerguntas || 1} value={a.respondidas} />
              {a.estado === "PENDENTE" ? (
                <div className="question-actions">
                  <button
                    className="primary"
                    onClick={async () => {
                      try {
                        await pedir(
                          "/salas/" + codigo + "/participantes/" + a.id,
                          token,
                          "PATCH",
                          { aprovada: true },
                        );
                      } catch (e) {
                        setErro(
                          e instanceof Error
                            ? e.message
                            : "Não foi possível aprovar a entrada.",
                        );
                      }
                    }}
                  >
                    Validar entrada
                  </button>
                  <button
                    className="secondary"
                    onClick={async () => {
                      try {
                        await pedir(
                          "/salas/" + codigo + "/participantes/" + a.id,
                          token,
                          "PATCH",
                          { aprovada: false },
                        );
                      } catch (e) {
                        setErro(
                          e instanceof Error
                            ? e.message
                            : "Não foi possível recusar a entrada.",
                        );
                      }
                    }}
                  >
                    Recusar
                  </button>
                </div>
              ) : a.estado === "APROVADO" && sala.aberta ? (
                <div
                  className="cursor-map"
                  aria-label="Posição aproximada do cursor nesta página"
                >
                  <small>
                    Cursor / toque ·{" "}
                    {a.respondidas === a.totalPerguntas
                      ? "Concluído"
                      : "Pergunta " + (a.respondidas + 1)}
                  </small>
                  {a.cursorX !== null && a.cursorY !== null && (
                    <span
                      style={{
                        left: `${a.cursorX / 10}%`,
                        top: `${a.cursorY / 10}%`,
                      }}
                      title="Última posição do cursor"
                    >
                      ●
                    </span>
                  )}
                </div>
              ) : a.estado === "RECUSADO" ? (
                <p>Entrada não validada.</p>
              ) : !sala.aberta ? (
                <strong>
                  Nota: {a.notaPercentual}% · {a.correctas}/{a.totalPerguntas}
                </strong>
              ) : (
                <p>A aguardar a abertura da pergunta…</p>
              )}
            </article>
          ))}
        </div>
      )}
      {!sala.aberta && (
        <>
          <div className="page-heading">
            <div>
              <p className="eyebrow">RESULTADOS FINAIS</p>
              <h2>Lista de notas</h2>
            </div>
            <button className="secondary" onClick={() => window.print()}>
              Imprimir lista
            </button>
          </div>
          <div className="table-card table-scroll">
            <table>
              <thead>
                <tr>
                  <th>Estudante</th>
                  <th>Identificação</th>
                  <th>Correctas</th>
                  <th>Total</th>
                  <th>Nota</th>
                </tr>
              </thead>
              <tbody>
                {alunos
                  .filter((a) => a.estado === "FECHADO")
                  .map((a) => (
                    <tr key={a.id}>
                      <td>{a.nome}</td>
                      <td>{a.identificacao}</td>
                      <td>{a.correctas}</td>
                      <td>{a.totalPerguntas}</td>
                      <td>{a.notaPercentual.toFixed(1)}%</td>
                    </tr>
                  ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </>
  );
}
