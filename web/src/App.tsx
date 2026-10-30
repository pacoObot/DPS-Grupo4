import { useEffect, useRef, useState } from "react";
import { ErroApi, pedir, perfis } from "./api";
import type { Sessao, Utilizador } from "./api";
import { Logo } from "./components/Logo";
import { Login } from "./components/Login";
import { Utilizadores } from "./components/Utilizadores";

export function App() {
  // O token fica apenas em memória; recarregar a página exige novo login.
  const [sessao, setSessao] = useState<Sessao | null>(null);
  const [aviso, setAviso] = useState("");
  const [vista, setVista] = useState("inicio");
  const geracao = useRef(0);
  const sair = (mensagem = "") => {
    geracao.current++;
    setSessao(null);
    setVista("inicio");
    setAviso(mensagem);
  };
  useEffect(() => {
    if (!sessao) return;
    const timer = window.setTimeout(
      () => sair("A sessão expirou. Entre novamente."),
      sessao.expiresIn * 1000,
    );
    const controller = new AbortController();
    const conferir = () =>
      pedir<Utilizador>(
        "/auth/me",
        sessao.accessToken,
        "GET",
        undefined,
        controller.signal,
      )
        .then((u) => {
          if (u.perfil !== sessao.utilizador.perfil || !u.activo)
            sair("A sua conta foi alterada. Entre novamente.");
        })
        .catch((e) => {
          if (e instanceof ErroApi && e.status === 401)
            sair("A sessão terminou. Entre novamente.");
        });
    window.addEventListener("focus", conferir);
    return () => {
      clearTimeout(timer);
      controller.abort();
      window.removeEventListener("focus", conferir);
    };
  }, [sessao]);
  function falha(e: unknown) {
    if (e instanceof ErroApi && e.status === 401) {
      sair("A sessão terminou. Entre novamente.");
      return;
    }
    setAviso(
      e instanceof Error ? e.message : "Ocorreu um erro. Tente novamente.",
    );
  }
  if (!sessao)
    return (
      <Login
        aviso={aviso}
        entrar={async (email, password) => {
          const id = ++geracao.current;
          const s = await pedir<Sessao>("/auth/login", undefined, "POST", {
            email,
            palavraPasse: password,
          });
          const u = await pedir<Utilizador>("/auth/me", s.accessToken);
          if (id === geracao.current) {
            setSessao({ ...s, utilizador: u });
            setAviso("");
          }
        }}
      />
    );
  const u = sessao.utilizador;
  return (
    <div className="app">
      <aside>
        <Logo />
        <p className="nav-label">ESPAÇO ACADÉMICO</p>
        <nav aria-label="Principal">
          <button
            className={vista === "inicio" ? "selected" : ""}
            onClick={() => {
              setVista("inicio");
              setAviso("");
            }}
          >
            ◈ <span>Visão geral</span>
          </button>
          {u.perfil === "ADMINISTRADOR" && (
            <button
              className={vista === "utilizadores" ? "selected" : ""}
              onClick={() => {
                setVista("utilizadores");
                setAviso("");
              }}
            >
              ♧ <span>Utilizadores</span>
            </button>
          )}
          <button
            className={vista === "conta" ? "selected" : ""}
            onClick={() => {
              setVista("conta");
              setAviso("");
            }}
          >
            ◎ <span>A minha conta</span>
          </button>
        </nav>
        <div className="sidebar-bottom">
          <span className="avatar">{u.nome.slice(0, 1).toUpperCase()}</span>
          <div>
            <strong>{u.nome}</strong>
            <small>{perfis[u.perfil]}</small>
          </div>
          <button
            className="logout"
            onClick={() => sair()}
            aria-label="Terminar sessão"
          >
            ↗
          </button>
        </div>
      </aside>
      <main>
        <header>
          <span>
            QuizRace{" "}
            <span className="muted">
              /{" "}
              {vista === "utilizadores"
                ? "Utilizadores"
                : vista === "conta"
                  ? "A minha conta"
                  : "Visão geral"}
            </span>
          </span>
          <div className="header-actions">
            <span className="pill">{perfis[u.perfil]}</span>
            <button className="text-button" onClick={() => sair()}>
              Sair
            </button>
          </div>
        </header>
        <div className="content">
          {aviso && (
            <div role="alert" className="notice error">
              {aviso}
              <button onClick={() => setAviso("")} aria-label="Fechar aviso">
                ×
              </button>
            </div>
          )}
          {vista === "inicio" && (
            <>
              <div className="page-heading">
                <div>
                  <p className="eyebrow">BEM-VINDO AO QUIZRACE</p>
                  <h1>Olá, {u.nome.split(" ")[0]}.</h1>
                  <p>O seu espaço para aprender, participar e acompanhar.</p>
                </div>
                <span className="date">ÁREA ACADÉMICA · GRUPO 4</span>
              </div>
              <section className="hero">
                <div>
                  <span className="pill light">Aprender em conjunto</span>
                  <h2>
                    Cada pergunta é<br />
                    um novo começo.
                  </h2>
                  <p>
                    {u.perfil === "ADMINISTRADOR"
                      ? "Organize o acesso da comunidade académica. Crie contas e atribua o perfil adequado a cada pessoa."
                      : "A sua conta está pronta. Consulte os seus dados e mantenha contacto com o responsável da sua instituição."}
                  </p>
                  {u.perfil === "ADMINISTRADOR" && (
                    <button
                      className="primary"
                      onClick={() => setVista("utilizadores")}
                    >
                      Gerir utilizadores <span>↗</span>
                    </button>
                  )}
                </div>
                <div className="hero-art" aria-hidden="true">
                  <span>Q</span>
                  <i>?</i>
                  <b>✓</b>
                </div>
              </section>
              <div className="cards">
                <section className="card">
                  <span className="card-icon">◎</span>
                  <h3>A sua identidade</h3>
                  <p>{u.email}</p>
                  <button
                    className="text-button"
                    onClick={() => setVista("conta")}
                  >
                    Consultar conta →
                  </button>
                </section>
                <section className="card">
                  <span className="card-icon">◇</span>
                  <h3>{perfis[u.perfil]}</h3>
                  <p>
                    {u.perfil === "ADMINISTRADOR"
                      ? "Pode criar contas e gerir perfis e acesso."
                      : "O seu acesso é definido pelo administrador."}
                  </p>
                </section>
                <section className="card">
                  <span className="card-icon">↗</span>
                  <h3>Próximas etapas</h3>
                  <p>
                    Preparação, avaliações e resultados serão disponibilizados
                    numa fase seguinte.
                  </p>
                </section>
              </div>
            </>
          )}
          {vista === "conta" && (
            <>
              <div className="page-heading">
                <div>
                  <p className="eyebrow">PERFIL</p>
                  <h1>A minha conta</h1>
                  <p>Os dados associados ao seu acesso académico.</p>
                </div>
              </div>
              <section className="card account">
                <dl>
                  <dt>Nome</dt>
                  <dd>{u.nome}</dd>
                  <dt>Email</dt>
                  <dd>{u.email}</dd>
                  <dt>Perfil</dt>
                  <dd>{perfis[u.perfil]}</dd>
                  <dt>Estado</dt>
                  <dd>Activo</dd>
                  <dt>Conta criada</dt>
                  <dd>{new Date(u.criadoEm).toLocaleDateString("pt-PT")}</dd>
                </dl>
                <p className="muted">
                  Para alterar os seus dados, contacte o administrador.
                </p>
              </section>
            </>
          )}
          {vista === "utilizadores" && u.perfil === "ADMINISTRADOR" && (
            <Utilizadores
              token={sessao.accessToken}
              actual={u.id}
              falha={falha}
              alterouProprio={() =>
                sair("A sua conta foi alterada. Entre novamente.")
              }
            />
          )}
        </div>
        <footer>
          QuizRace · Aprendizagem e avaliação{" "}
          <span>Projecto académico DPS</span>
        </footer>
      </main>
    </div>
  );
}
