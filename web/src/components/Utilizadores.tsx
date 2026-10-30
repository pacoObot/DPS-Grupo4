import { useEffect, useState } from "react";
import { pedir, perfis } from "../api";
import type { Pagina, Utilizador } from "../api";
import { Formulario } from "./Formulario";

export function Utilizadores({
  token,
  actual,
  falha,
  alterouProprio,
}: {
  token: string;
  actual: string;
  falha: (e: unknown) => void;
  alterouProprio: () => void;
}) {
  const [pagina, setPagina] = useState(0);
  const [dados, setDados] = useState<Pagina | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [revisao, setRevisao] = useState(0);
  const [form, setForm] = useState<Utilizador | "novo" | null>(null);
  const [sucesso, setSucesso] = useState("");
  useEffect(() => {
    const c = new AbortController();
    setCarregando(true);
    setDados(null);
    pedir<Pagina>(
      "/users?page=" + pagina + "&size=10",
      token,
      "GET",
      undefined,
      c.signal,
    )
      .then(setDados)
      .catch((e) => {
        if (!c.signal.aborted) falha(e);
      })
      .finally(() => {
        if (!c.signal.aborted) setCarregando(false);
      });
    return () => c.abort();
  }, [token, pagina, revisao]);
  return (
    <>
      <div className="page-heading">
        <div>
          <p className="eyebrow">ADMINISTRAÇÃO</p>
          <h1>Utilizadores</h1>
          <p>As pessoas que fazem parte da comunidade.</p>
        </div>
        <button
          className="primary"
          onClick={() => {
            setSucesso("");
            setForm("novo");
          }}
        >
          + Novo utilizador
        </button>
      </div>
      {sucesso && (
        <div role="status" className="notice success">
          {sucesso}
        </div>
      )}
      <section className="table-card">
        <div className="table-top">
          <h3>Directório de contas</h3>
          <span>{dados ? dados.totalElements + " utilizadores" : "—"}</span>
          <button
            className="text-button"
            onClick={() => setRevisao((v) => v + 1)}
            disabled={carregando}
          >
            Actualizar ↻
          </button>
        </div>
        {carregando ? (
          <p role="status" className="empty">
            A carregar utilizadores…
          </p>
        ) : !dados ? (
          <p className="empty">
            Não foi possível carregar a lista. Utilize “Actualizar” para tentar
            novamente.
          </p>
        ) : dados.content.length === 0 ? (
          <p className="empty">Ainda não existem utilizadores nesta página.</p>
        ) : (
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>Utilizador</th>
                  <th>Perfil</th>
                  <th>Estado</th>
                  <th>Criado em</th>
                  <th>
                    <span className="sr-only">Acções</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {dados.content.map((u) => (
                  <tr key={u.id}>
                    <td>
                      <strong>
                        {u.nome}
                        {u.id === actual && <small> (você)</small>}
                      </strong>
                      <small>{u.email}</small>
                    </td>
                    <td>{perfis[u.perfil]}</td>
                    <td>
                      <span
                        className={
                          "status " + (u.activo ? "active" : "inactive")
                        }
                      >
                        {u.activo ? "Activo" : "Inactivo"}
                      </span>
                    </td>
                    <td>{new Date(u.criadoEm).toLocaleDateString("pt-PT")}</td>
                    <td>
                      <button
                        className="secondary"
                        onClick={() => {
                          setSucesso("");
                          setForm(u);
                        }}
                        aria-label={"Editar " + u.nome}
                      >
                        Editar
                      </button>
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
              ? " de " + Math.max(1, Math.ceil(dados.totalElements / 10))
              : ""}
          </span>
          <div>
            <button
              className="secondary"
              disabled={pagina === 0 || carregando}
              onClick={() => setPagina((p) => p - 1)}
            >
              ← Anterior
            </button>
            <button
              className="secondary"
              disabled={
                !dados || (pagina + 1) * 10 >= dados.totalElements || carregando
              }
              onClick={() => setPagina((p) => p + 1)}
            >
              Seguinte →
            </button>
          </div>
        </div>
      </section>
      {form && (
        <Formulario
          token={token}
          utilizador={form}
          actual={actual}
          fechar={() => setForm(null)}
          falhaSessao={falha}
          concluiu={(u) => {
            setForm(null);
            if (u.id === actual) {
              alterouProprio();
              return;
            }
            setSucesso("Conta guardada com sucesso.");
            setRevisao((v) => v + 1);
          }}
        />
      )}
    </>
  );
}
