import { useEffect, useRef, useState } from "react";
import { pedir, ErroApi } from "../api";
import { dataHora } from "../testes";
import type { PaginaTeste, ResumoTeste, Teste } from "../testes";
import { EditorTeste } from "./EditorTeste";

export function Testes({
  token,
  falha,
  mudou,
  criarSala,
}: {
  token: string;
  falha: (e: unknown) => void;
  mudou: (valor: boolean) => void;
  criarSala: (id: string) => void;
}) {
  const [pagina, setPagina] = useState(0);
  const [revisao, setRevisao] = useState(0);
  const [dados, setDados] = useState<PaginaTeste<ResumoTeste> | null>(null);
  const [editor, setEditor] = useState<Teste | "novo" | null>(null);
  const [ocupado, setOcupado] = useState(true);
  const [abrindo, setAbrindo] = useState(false);
  const [erro, setErro] = useState("");
  const detalhe = useRef<AbortController | null>(null);
  useEffect(() => () => detalhe.current?.abort(), []);
  useEffect(() => {
    const c = new AbortController();
    setOcupado(true);
    setErro("");
    setDados(null);
    pedir<PaginaTeste<ResumoTeste>>(
      `/testes?page=${pagina}&size=10`,
      token,
      "GET",
      undefined,
      c.signal,
    )
      .then(setDados)
      .catch((e) => {
        if (!c.signal.aborted) tratar(e);
      })
      .finally(() => {
        if (!c.signal.aborted) setOcupado(false);
      });
    return () => c.abort();
  }, [token, pagina, revisao]);
  function tratar(e: unknown) {
    if (e instanceof ErroApi && e.status === 401) falha(e);
    else
      setErro(
        e instanceof Error ? e.message : "Não foi possível carregar os testes.",
      );
  }
  if (editor)
    return (
      <EditorTeste
        token={token}
        inicial={editor === "novo" ? null : editor}
        falha={falha}
        mudou={mudou}
        voltar={() => {
          setEditor(null);
          setRevisao((r) => r + 1);
        }}
      />
    );
  return (
    <>
      <div className="page-heading">
        <div>
          <p className="eyebrow">ÁREA DO DOCENTE</p>
          <h1>Os meus testes</h1>
          <p>Prepare as perguntas e guarde cada etapa do seu trabalho.</p>
        </div>
        <button
          className="primary"
          disabled={abrindo}
          onClick={() => setEditor("novo")}
        >
          + Novo teste
        </button>
      </div>
      {erro && (
        <div className="notice error" role="alert">
          {erro}
        </div>
      )}
      <section className="table-card">
        <div className="table-top">
          <h3>Testes em preparação</h3>
          <span>{dados ? `${dados.totalElements} testes` : "—"}</span>
          <button
            className="text-button"
            disabled={ocupado || abrindo}
            onClick={() => setRevisao((r) => r + 1)}
          >
            Actualizar ↻
          </button>
        </div>
        {ocupado ? (
          <p className="empty" role="status">
            A carregar testes…
          </p>
        ) : !dados ? (
          <p className="empty">Utilize “Actualizar” para tentar novamente.</p>
        ) : dados.content.length === 0 ? (
          <p className="empty">
            Ainda não há testes nesta página. Crie um teste para começar.
          </p>
        ) : (
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>Teste</th>
                  <th>Perguntas</th>
                  <th>Última gravação</th>
                  <th>Estado</th>
                  <th>Acções</th>
                </tr>
              </thead>
              <tbody>
                {dados.content.map((t) => (
                  <tr key={t.id}>
                    <td>
                      <strong>{t.titulo}</strong>
                      <small>Versão {t.versao + 1}</small>
                    </td>
                    <td>{t.totalPerguntas}</td>
                    <td>{dataHora(t.actualizadoEm)}</td>
                    <td>
                      <span
                        className={
                          "status " + (t.disponivelPratica ? "active" : "")
                        }
                      >
                        {t.disponivelPratica
                          ? "Quiz de preparação"
                          : "Rascunho"}
                      </span>
                    </td>
                    <td>
                      <div className="question-actions">
                        <button
                          className="secondary"
                          disabled={abrindo}
                          aria-label={`Editar ${t.titulo}`}
                          onClick={async () => {
                            const c = new AbortController();
                            detalhe.current?.abort();
                            detalhe.current = c;
                            setAbrindo(true);
                            setErro("");
                            try {
                              const teste = await pedir<Teste>(
                                "/testes/" + t.id,
                                token,
                                "GET",
                                undefined,
                                c.signal,
                              );
                              if (!c.signal.aborted) setEditor(teste);
                            } catch (e) {
                              if (!c.signal.aborted) tratar(e);
                            } finally {
                              if (!c.signal.aborted) setAbrindo(false);
                            }
                          }}
                        >
                          Editar
                        </button>
                        <button
                          className="text-button"
                          disabled={t.totalPerguntas === 0}
                          onClick={() => criarSala(t.id)}
                        >
                          Criar sala ↗
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <div className="pagination">
          <span>
            Página {pagina + 1}
            {dados
              ? ` de ${Math.max(1, Math.ceil(dados.totalElements / 10))}`
              : ""}
          </span>
          <div>
            <button
              className="secondary"
              disabled={pagina === 0 || ocupado || abrindo}
              onClick={() => setPagina((p) => p - 1)}
            >
              ← Anterior
            </button>
            <button
              className="secondary"
              disabled={
                !dados ||
                (pagina + 1) * 10 >= dados.totalElements ||
                ocupado ||
                abrindo
              }
              onClick={() => setPagina((p) => p + 1)}
            >
              Seguinte →
            </button>
          </div>
        </div>
      </section>
      <p className="help">
        Só os seus testes aparecem aqui. Publique uma sala para gerar um link de
        participação.
      </p>
    </>
  );
}
