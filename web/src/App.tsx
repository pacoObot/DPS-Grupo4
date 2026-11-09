import { useEffect, useRef, useState } from "react";
import { ErroApi, pedir, perfis } from "./api";
import type { Sessao, Utilizador } from "./api";
import { Logo } from "./components/Logo";
import { Login } from "./components/Login";
import { Utilizadores } from "./components/Utilizadores";
import { Testes } from "./components/Testes";
import { SalasDocente } from "./components/SalasDocente";
import { QuizPratica } from "./components/QuizPratica";

export function App() {
  // O token fica apenas em memória; recarregar a página exige novo login.
  const [sessao, setSessao] = useState<Sessao | null>(null);
  const [aviso, setAviso] = useState("");
  const [vista, setVista] = useState("inicio");
  const [testeParaSala, setTesteParaSala] = useState<string | null>(null);
  const [edicaoPendente, setEdicaoPendente] = useState(false);
  function navegar(destino: string) {
    if (destino === vista) return;
    if (
      edicaoPendente &&
      !window.confirm("Sair do editor e perder as alterações não guardadas?")
    )
      return;
    setEdicaoPendente(false);
    setVista(destino);
    setAviso("");
  }
  function terminarSessao() {
    if (
      edicaoPendente &&
      !window.confirm("Terminar sessão e perder as alterações não guardadas?")
    )
      return;
    sair();
  }
  const geracao = useRef(0);
  const sair = (mensagem = "") => {
    geracao.current++;
    setSessao(null);
    setEdicaoPendente(false);
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
              navegar("inicio");
              setAviso("");
            }}
          >
            ◈ <span>Visão geral</span>
          </button>
          {u.perfil === "ADMINISTRADOR" && (
            <button
              className={vista === "utilizadores" ? "selected" : ""}
              onClick={() => {
                navegar("utilizadores");
                setAviso("");
              }}
            >
              ♧ <span>Utilizadores</span>
            </button>
          )}
          {u.perfil === "DOCENTE" && (
            <button
              className={vista === "salas" ? "selected" : ""}
              onClick={() => navegar("salas")}
            >
              ◉ <span>Salas de teste</span>
            </button>
          )}
          {u.perfil === "ESTUDANTE" && (
            <button
              className={vista === "quizzes" ? "selected" : ""}
              onClick={() => navegar("quizzes")}
            >
              ✎ <span>Preparar quiz</span>
            </button>
          )}
          {u.perfil === "DOCENTE" && (
            <button
              className={vista === "testes" ? "selected" : ""}
              onClick={() => navegar("testes")}
            >
              ▤ <span>Os meus testes</span>
            </button>
          )}
          <button
            className={vista === "conta" ? "selected" : ""}
            onClick={() => {
              navegar("conta");
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
            onClick={terminarSessao}
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
                  : vista === "testes"
                    ? "Os meus testes"
                    : vista === "salas"
                      ? "Salas de teste"
                      : vista === "quizzes"
                        ? "Preparar quiz"
                        : "Visão geral"}
            </span>
          </span>
          <div className="header-actions">
            <span className="pill">{perfis[u.perfil]}</span>
            <button className="text-button" onClick={terminarSessao}>
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
                      : u.perfil === "DOCENTE"
                        ? "Prepare testes, partilhe uma sala e acompanhe a participação. Consulte as versões guardadas."
                        : "Pratique com quizzes e acompanhe a sua preparação. Nas avaliações, acompanhe o teste através do link partilhado pela docente."}
                  </p>
                  {u.perfil === "ADMINISTRADOR" && (
                    <button
                      className="primary"
                      onClick={() => navegar("utilizadores")}
                    >
                      Gerir utilizadores <span>↗</span>
                    </button>
                  )}
                  {u.perfil === "ESTUDANTE" && (
                    <button
                      className="primary"
                      onClick={() => navegar("quizzes")}
                    >
                      Praticar um quiz <span>↗</span>
                    </button>
                  )}
                  {u.perfil === "DOCENTE" && (
                    <button
                      className="primary"
                      onClick={() => navegar("testes")}
                    >
                      Preparar um teste <span>↗</span>
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
                    onClick={() => navegar("conta")}
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
                      : u.perfil === "DOCENTE"
                        ? "Pode criar testes, partilhar salas e acompanhar os estudantes."
                        : "Pode praticar com os quizzes que os docentes disponibilizam."}
                  </p>
                </section>
                <section className="card">
                  <span className="card-icon">↗</span>
                  <h3>Próximas etapas</h3>
                  <p>
                    Participe num quiz de preparação ou use o link de sala
                    enviado pela docente. As suas respostas são recebidas pelo
                    servidor.
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
          {vista === "testes" && u.perfil === "DOCENTE" && (
            <Testes
              token={sessao.accessToken}
              falha={falha}
              mudou={setEdicaoPendente}
              criarSala={(id) => {
                setTesteParaSala(id);
                navegar("salas");
              }}
            />
          )}
          {vista === "salas" && u.perfil === "DOCENTE" && (
            <SalasDocente
              token={sessao.accessToken}
              seleccionado={testeParaSala}
              falha={falha}
            />
          )}
          {vista === "quizzes" && u.perfil === "ESTUDANTE" && (
            <QuizPratica token={sessao.accessToken} falha={falha} />
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
