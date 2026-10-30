import { useEffect, useRef, useState } from "react";
import { ErroApi, pedir, perfis } from "../api";
import type { Utilizador } from "../api";

export function Formulario({
  token,
  utilizador,
  actual,
  fechar,
  falhaSessao,
  concluiu,
}: {
  token: string;
  utilizador: Utilizador | "novo";
  actual: string;
  fechar: () => void;
  falhaSessao: (e: unknown) => void;
  concluiu: (u: Utilizador) => void;
}) {
  const dialog = useRef<HTMLDialogElement>(null);
  const [erro, setErro] = useState("");
  const [ocupado, setOcupado] = useState(false);
  const novo = utilizador === "novo";
  useEffect(() => {
    dialog.current?.showModal();
  }, []);
  return (
    <dialog
      ref={dialog}
      onCancel={(e) => {
        if (ocupado) e.preventDefault();
        else fechar();
      }}
      aria-labelledby="form-title"
    >
      <form
        onSubmit={async (e) => {
          e.preventDefault();
          if (ocupado) return;
          const d = new FormData(e.currentTarget);
          setErro("");
          const password = String(d.get("palavraPasse") || "");
          if (novo && new TextEncoder().encode(password).length > 72) {
            setErro("A palavra-passe deve ter no máximo 72 bytes UTF-8.");
            return;
          }
          setOcupado(true);
          try {
            const dados = novo
              ? {
                  nome: String(d.get("nome")).trim(),
                  email: String(d.get("email")).trim(),
                  palavraPasse: password,
                  perfil: d.get("perfil"),
                }
              : { perfil: d.get("perfil"), activo: d.get("activo") === "true" };
            const u = await pedir<Utilizador>(
              novo ? "/users" : "/users/" + utilizador.id,
              token,
              novo ? "POST" : "PATCH",
              dados,
            );
            concluiu(u);
          } catch (err) {
            if (err instanceof ErroApi && err.status === 401) falhaSessao(err);
            else
              setErro(
                err instanceof Error
                  ? err.message
                  : "Não foi possível guardar.",
              );
          } finally {
            setOcupado(false);
          }
        }}
      >
        <div className="dialog-heading">
          <h2 id="form-title">{novo ? "Novo utilizador" : "Editar conta"}</h2>
          <button
            type="button"
            className="close"
            disabled={ocupado}
            onClick={fechar}
            aria-label="Fechar formulário"
          >
            ×
          </button>
        </div>
        <p>
          {novo
            ? "Crie o acesso e escolha o perfil da pessoa."
            : utilizador.nome + " · " + utilizador.email}
        </p>
        {erro && (
          <div role="alert" className="notice error">
            {erro}
          </div>
        )}
        {novo && (
          <>
            <label>
              Nome completo
              <input
                name="nome"
                autoComplete="name"
                required
                minLength={2}
                maxLength={100}
              />
            </label>
            <label>
              Email
              <input
                name="email"
                type="email"
                autoComplete="off"
                required
                maxLength={254}
              />
            </label>
            <label>
              Palavra-passe inicial
              <input
                name="palavraPasse"
                type="password"
                autoComplete="new-password"
                required
                minLength={12}
                maxLength={72}
              />
              <small>Pelo menos 12 caracteres; máximo de 72 bytes UTF-8.</small>
            </label>
          </>
        )}
        <label>
          Perfil
          <select
            name="perfil"
            defaultValue={novo ? "ESTUDANTE" : utilizador.perfil}
          >
            {Object.entries(perfis).map(([v, n]) => (
              <option key={v} value={v}>
                {n}
              </option>
            ))}
          </select>
        </label>
        {!novo && (
          <>
            <label>
              Estado
              <select name="activo" defaultValue={String(utilizador.activo)}>
                <option value="true">Activo</option>
                <option value="false">Inactivo</option>
              </select>
            </label>
            <p className="help">
              Guardar invalida a sessão actual desta conta.
              {utilizador.id === actual
                ? " Está a editar a sua própria conta e terá de entrar novamente."
                : ""}
            </p>
          </>
        )}
        <div className="form-actions">
          <button
            type="button"
            className="secondary"
            disabled={ocupado}
            onClick={fechar}
          >
            Cancelar
          </button>
          <button className="primary" disabled={ocupado}>
            {ocupado ? "A guardar…" : "Guardar conta"}
          </button>
        </div>
      </form>
    </dialog>
  );
}
