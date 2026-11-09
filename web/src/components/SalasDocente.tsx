import { useEffect, useState } from "react";
import { ErroApi, pedir } from "../api";
import type { PaginaTeste, ResumoTeste } from "../testes";
import { MonitorSala } from "./MonitorSala";
export type Sala = {
  codigo: string;
  nome: string;
  tituloTeste: string;
  totalPerguntas: number;
  segundosPorPergunta: number;
  aberta: boolean;
  criadaEm: string;
  fechadaEm: string | null;
};
export function SalasDocente({
  token,
  seleccionado,
  falha,
}: {
  token: string;
  seleccionado: string | null;
  falha: (e: unknown) => void;
}) {
  const [testes, setTestes] = useState<ResumoTeste[]>([]),
    [salas, setSalas] = useState<Sala[]>([]),
    [sala, setSala] = useState<Sala | null>(null),
    [testeId, setTesteId] = useState(seleccionado || ""),
    [nome, setNome] = useState("Sala de demonstração"),
    [tempo, setTempo] = useState(15),
    [erro, setErro] = useState(""),
    [ocupado, setOcupado] = useState(true),
    [criando, setCriando] = useState(false);
  async function carregar() {
    setOcupado(true);
    try {
      const [t, s] = await Promise.all([
        pedir<PaginaTeste<ResumoTeste>>("/testes?page=0&size=50", token),
        pedir<Sala[]>("/salas", token),
      ]);
      setTestes(t.content);
      setSalas(s);
      setErro("");
    } catch (e) {
      if (e instanceof ErroApi && e.status === 401) falha(e);
      else
        setErro(
          e instanceof Error
            ? e.message
            : "Não foi possível carregar as salas.",
        );
    } finally {
      setOcupado(false);
    }
  }
  useEffect(() => {
    void carregar();
  }, [token]);
  if (sala)
    return (
      <MonitorSala
        codigo={sala.codigo}
        token={token}
        falha={falha}
        voltar={() => {
          setSala(null);
          void carregar();
        }}
      />
    );
  return (
    <>
      <div className="page-heading">
        <div>
          <p className="eyebrow">ÁREA DO DOCENTE</p>
          <h1>Salas de teste</h1>
          <p>Crie um link, valide a entrada e acompanhe as respostas.</p>
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
      <form
        className="card create-room-form"
        onSubmit={async (e) => {
          e.preventDefault();
          if (criando) return;
          setCriando(true);
          setErro("");
          try {
            const criada = await pedir<Sala>("/salas", token, "POST", {
              testeId: testes.some((t) => t.id === testeId)
                ? testeId
                : testes[0]?.id,
              nome,
              segundosPorPergunta: tempo,
            });
            setSala(criada);
          } catch (err) {
            if (err instanceof ErroApi && err.status === 401) falha(err);
            else
              setErro(
                err instanceof Error
                  ? err.message
                  : "Não foi possível criar a sala.",
              );
          } finally {
            setCriando(false);
          }
        }}
      >
        <p className="eyebrow">NOVA SESSÃO</p>
        <h2>Disponibilizar um teste</h2>
        {!testes.length ? (
          <p>
            Prepare primeiro um teste com pelo menos uma pergunta em **Os meus
            testes**.
          </p>
        ) : (
          <>
            <label>
              Teste
              <select
                required
                value={
                  testes.some((t) => t.id === testeId) ? testeId : testes[0].id
                }
                onChange={(e) => setTesteId(e.target.value)}
              >
                {testes.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.titulo} ({t.totalPerguntas} perguntas)
                  </option>
                ))}
              </select>
            </label>
            <label>
              Nome da sala
              <input
                required
                maxLength={100}
                value={nome}
                onChange={(e) => setNome(e.target.value)}
              />
            </label>
            <label>
              Tempo para cada pergunta (segundos)
              <input
                type="number"
                min={10}
                max={300}
                required
                value={tempo}
                onChange={(e) => setTempo(Number(e.target.value))}
              />
              <small>O servidor controla o limite e guarda as respostas.</small>
            </label>
            <button className="primary" disabled={criando}>
              {criando ? "A criar sala…" : "Criar sala e gerar URL"}
            </button>
          </>
        )}
      </form>
      <div className="section-heading">
        <h2>As minhas salas</h2>
      </div>
      {ocupado ? (
        <p role="status">A carregar salas…</p>
      ) : !salas.length ? (
        <div className="card empty">Ainda não foram criadas salas.</div>
      ) : (
        <div className="cards">
          {salas.map((s) => (
            <article className="card" key={s.codigo}>
              <span className={"status " + (s.aberta ? "active" : "inactive")}>
                {s.aberta ? "Aberta" : "Encerrada"}
              </span>
              <h3>{s.nome}</h3>
              <p>
                {s.tituloTeste} · {s.totalPerguntas} perguntas ·{" "}
                {s.segundosPorPergunta}s
              </p>
              <button className="secondary" onClick={() => setSala(s)}>
                Abrir monitor →
              </button>
            </article>
          ))}
        </div>
      )}
    </>
  );
}
