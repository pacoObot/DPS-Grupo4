import { useState } from "react";
import { ErroApi } from "../api";
import { Logo } from "./Logo";

export function Login({
  aviso,
  entrar,
}: {
  aviso: string;
  entrar: (email: string, password: string) => Promise<void>;
}) {
  const [erro, setErro] = useState("");
  const [ocupado, setOcupado] = useState(false);
  return (
    <div className="login">
      <section className="login-story">
        <Logo />
        <div>
          <p className="eyebrow">UM ESPAÇO. NOVAS POSSIBILIDADES.</p>
          <h1>
            Conhecimento
            <br />
            que se partilha.
          </h1>
          <p>
            Entre no seu espaço académico e faça parte da próxima etapa do
            QuizRace.
          </p>
          <div className="story-line">
            <span>01</span> Aprender <span>02</span> Participar <span>03</span>{" "}
            Evoluir
          </div>
        </div>
        <small>QUIZRACE / DPS · GRUPO 4</small>
      </section>
      <main className="login-form">
        <form
          onSubmit={async (e) => {
            e.preventDefault();
            if (ocupado) return;
            const dados = new FormData(e.currentTarget);
            setOcupado(true);
            setErro("");
            try {
              await entrar(
                String(dados.get("email")).trim(),
                String(dados.get("password")),
              );
            } catch (err) {
              setErro(
                err instanceof ErroApi && err.status === 401
                  ? "Email ou palavra-passe inválidos."
                  : err instanceof Error
                    ? err.message
                    : "Não foi possível entrar.",
              );
            } finally {
              setOcupado(false);
            }
          }}
        >
          <span className="pill">ÁREA ACADÉMICA</span>
          <h2>Bem-vindo de volta.</h2>
          <p>Introduza os dados da sua conta para continuar.</p>
          {(erro || aviso) && (
            <div role="alert" className="notice error">
              {erro || aviso}
            </div>
          )}
          <label>
            Email
            <input
              name="email"
              type="email"
              autoComplete="username"
              maxLength={254}
              required
              placeholder="nome@instituicao.ac.mz"
            />
          </label>
          <label>
            Palavra-passe
            <input
              name="password"
              type="password"
              autoComplete="current-password"
              maxLength={72}
              required
              placeholder="A sua palavra-passe"
            />
          </label>
          <button className="primary wide" disabled={ocupado}>
            {ocupado ? "A entrar…" : "Entrar na minha conta"} <span>→</span>
          </button>
          <p className="help">
            Ainda não tem acesso? Peça uma conta ao administrador da sua
            instituição.
          </p>
        </form>
      </main>
    </div>
  );
}
