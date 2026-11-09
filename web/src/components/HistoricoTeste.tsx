import { useEffect, useRef, useState } from "react";
import { ErroApi, pedir } from "../api";
import { dataHora } from "../testes";
import type { PaginaTeste, RevisaoTeste, Teste } from "../testes";
import { VisualizarTeste } from "./VisualizarTeste";

export function HistoricoTeste({
  id,
  token,
  fechar,
  falha,
}: {
  id: string;
  token: string;
  fechar: () => void;
  falha: (e: unknown) => void;
}) {
  const dialog = useRef<HTMLDialogElement>(null);
  const [pagina, setPagina] = useState(0);
  const [dados, setDados] = useState<PaginaTeste<RevisaoTeste> | null>(null);
  const [versao, setVersao] = useState<number | null>(null);
  const [detalhe, setDetalhe] = useState<Teste | null>(null);
  const [erro, setErro] = useState("");
  const [ocupado, setOcupado] = useState(true);
  useEffect(() => {
    dialog.current?.showModal();
  }, []);
  useEffect(() => {
    const c = new AbortController();
    setErro("");
    setOcupado(true);
    setDetalhe(null);
    const rota = `/testes/${id}/historico`;
    const consulta =
      versao === null
        ? pedir<PaginaTeste<RevisaoTeste>>(
            rota + `?page=${pagina}&size=10`,
            token,
            "GET",
            undefined,
            c.signal,
          ).then(setDados)
        : pedir<Teste>(
            rota + "/" + versao,
            token,
            "GET",
            undefined,
            c.signal,
          ).then(setDetalhe);
    consulta
      .catch((e) => {
        if (c.signal.aborted) return;
        if (e instanceof ErroApi && e.status === 401) falha(e);
        else
          setErro(
            e instanceof Error
              ? e.message
              : "Não foi possível carregar o histórico.",
          );
      })
      .finally(() => {
        if (!c.signal.aborted) setOcupado(false);
      });
    return () => c.abort();
  }, [id, token, pagina, versao]);
  return (
    <dialog
      className="history-dialog"
      ref={dialog}
      onCancel={fechar}
      aria-labelledby="history-title"
    >
      <div className="dialog-heading">
        <h2 id="history-title">Histórico do teste</h2>
        <button
          type="button"
          className="close"
          onClick={fechar}
          aria-label="Fechar histórico"
        >
          ×
        </button>
      </div>
      <p>
        Cada gravação conserva uma cópia. As versões anteriores são apenas para
        consulta.
      </p>
      {erro && (
        <p className="notice error" role="alert">
          {erro}
        </p>
      )}
      {versao !== null && (
        <button className="secondary" onClick={() => setVersao(null)}>
          ← Voltar às versões
        </button>
      )}
      {ocupado ? (
        <p role="status">A carregar…</p>
      ) : versao === null && dados ? (
        <>
          <ul className="history-list">
            {dados.content.map((r) => (
              <li key={r.versao}>
                <div>
                  <strong>Versão {r.versao + 1}</strong>
                  <small>{dataHora(r.guardadoEm)}</small>
                </div>
                <button
                  className="secondary"
                  onClick={() => setVersao(r.versao)}
                  aria-label={`Consultar versão ${r.versao + 1}`}
                >
                  Consultar
                </button>
              </li>
            ))}
          </ul>
          <div className="pagination">
            <span>Página {pagina + 1}</span>
            <div>
              <button
                className="secondary"
                disabled={pagina === 0}
                onClick={() => setPagina((p) => p - 1)}
              >
                Anterior
              </button>
              <button
                className="secondary"
                disabled={(pagina + 1) * 10 >= dados.totalElements}
                onClick={() => setPagina((p) => p + 1)}
              >
                Seguinte
              </button>
            </div>
          </div>
        </>
      ) : (
        detalhe && (
          <>
            <p className="eyebrow">
              VERSÃO {detalhe.versao + 1} · {dataHora(detalhe.actualizadoEm)}
            </p>
            <VisualizarTeste {...detalhe} />
          </>
        )
      )}
    </dialog>
  );
}
