import { useEffect, useRef, useState } from "react";
import { pedir } from "../api";
import { Logo } from "./Logo";
type Opcao = { codigo: string; texto: string };
type Pergunta = {
  numero: number;
  enunciado: string;
  categoria: string;
  tipo: string;
  opcoes: Opcao[];
  segundosPorPergunta: number;
  segundosRestantes: number;
};
type Estado = {
  estado: string;
  codigoSala: string;
  tituloTeste: string;
  respondidas: number;
  totalPerguntas: number;
  pergunta: Pergunta | null;
  terminado: boolean;
  nota: number | null;
};
type Credencial = {
  id: string;
  token: string;
  estado: string;
  mensagem: string;
};
const chave = (codigo: string) => "quizrace-participante-" + codigo;
export function SalaPublica({ codigo }: { codigo: string }) {
  const [valor, setValor] = useState(""),
    [credencial, setCredencial] = useState<Credencial | null>(() => {
      try {
        return JSON.parse(sessionStorage.getItem(chave(codigo)) || "null");
      } catch {
        return null;
      }
    }),
    [estado, setEstado] = useState<Estado | null>(null),
    [erro, setErro] = useState(""),
    [ocupado, setOcupado] = useState(false),
    [respostaEnviada, setRespostaEnviada] = useState(false),
    [escolha, setEscolha] = useState(""),
    [tempo, setTempo] = useState(0),
    [relogio, setRelogio] = useState(0);
  const ultimaPosicao = useRef(0);
  const submetida = useRef(false);
  const perguntaActual = useRef<number | null>(null);
  useEffect(() => {
    if (!credencial) return;
    const ctrl = new AbortController();
    let a = false;
    const actualizar = async () => {
      if (a) return;
      a = true;
      try {
        const e = await pedir<Estado>(
          "/salas/participacoes/" + credencial.id,
          undefined,
          "GET",
          undefined,
          ctrl.signal,
          { "X-Quiz-Participante": credencial.token },
        );
        setEstado(e);
        if (e.pergunta) {
          if (perguntaActual.current !== e.pergunta.numero) {
            perguntaActual.current = e.pergunta.numero;
            setEscolha("");
            setTempo(e.pergunta.segundosRestantes);
            setRespostaEnviada(false);
            submetida.current = false;
          }
        } else if (e.estado === "FECHADO")
          sessionStorage.removeItem(chave(codigo));
      } catch (err) {
        if (!ctrl.signal.aborted)
          setErro(
            err instanceof Error
              ? err.message
              : "Não foi possível consultar a sala.",
          );
      } finally {
        a = false;
      }
    };
    void actualizar();
    const timer = window.setInterval(actualizar, 2000);
    return () => {
      ctrl.abort();
      clearInterval(timer);
    };
  }, [credencial, codigo, relogio]);
  useEffect(() => {
    if (!estado?.pergunta) return;
    const t = window.setInterval(
      () => setTempo((v) => Math.max(0, v - 1)),
      1000,
    );
    return () => clearInterval(t);
  }, [estado?.pergunta?.numero, estado?.pergunta?.segundosRestantes]);
  useEffect(() => {
    if (tempo !== 0 || !estado?.pergunta || !credencial || submetida.current)
      return;
    void enviar(null);
  }, [tempo]);
  async function enviar(resposta: string | null) {
    if (!credencial || !estado?.pergunta || ocupado || submetida.current)
      return;
    submetida.current = true;
    setOcupado(true);
    try {
      await pedir(
        "/salas/participacoes/" + credencial.id + "/respostas",
        undefined,
        "POST",
        { numeroPergunta: estado.pergunta.numero, resposta },
        undefined,
        { "X-Quiz-Participante": credencial.token },
      );
      setRespostaEnviada(true);
      setEscolha("");
      setErro("");
      setRelogio((v) => v + 1);
    } catch (e) {
      submetida.current = false;
      setRespostaEnviada(false);
      if (resposta !== null) setEscolha("");
      setErro(
        e instanceof Error
          ? e.message
          : "Não foi possível registar a resposta.",
      );
    } finally {
      setOcupado(false);
    }
  }
  async function entrar(e: React.FormEvent) {
    e.preventDefault();
    setOcupado(true);
    setErro("");
    try {
      const dispositivo = /Mobi|Android|iPhone|iPad/i.test(navigator.userAgent)
        ? "TELEMOVEL"
        : "COMPUTADOR";
      const c = await pedir<Credencial>("/salas/entrada", undefined, "POST", {
        codigo,
        entrada: {
          nome: valor.trim(),
          identificacao: valor.trim(),
          dispositivo,
        },
      });
      sessionStorage.setItem(chave(codigo), JSON.stringify(c));
      setCredencial(c);
    } catch (err) {
      setErro(
        err instanceof Error
          ? err.message
          : "Não foi possível entrar nesta sala.",
      );
    } finally {
      setOcupado(false);
    }
  }
  async function cursor(e: React.PointerEvent<HTMLDivElement>) {
    if (
      !credencial ||
      !estado?.pergunta ||
      Date.now() - ultimaPosicao.current < 600
    )
      return;
    ultimaPosicao.current = Date.now();
    const r = e.currentTarget.getBoundingClientRect();
    const x = Math.round(
        Math.min(1, Math.max(0, (e.clientX - r.left) / r.width)) * 1000,
      ),
      y = Math.round(
        Math.min(1, Math.max(0, (e.clientY - r.top) / r.height)) * 1000,
      );
    void pedir(
      "/salas/participacoes/" + credencial.id + "/cursor",
      undefined,
      "POST",
      { x, y },
      undefined,
      { "X-Quiz-Participante": credencial.token },
    ).catch(() => {});
  }
  const mensagemErro = erro && (
    <div role="alert" className="notice error">
      {erro}
      <button onClick={() => setErro("")}>×</button>
    </div>
  );
  if (!credencial)
    return (
      <main className="public-room">
        <header>
          <Logo />
          <a href="/">Sobre o QuizRace</a>
        </header>
        <section className="card join-card">
          <p className="eyebrow">ENTRADA NA SALA</p>
          <h1>Entre para participar</h1>
          <p>
            Introduza o seu nome ou número de registo académico. A docente vai
            validar a entrada antes de mostrar as perguntas.
          </p>
          {mensagemErro}
          <form onSubmit={entrar}>
            <label>
              Nome ou número de registo
              <input
                autoFocus
                required
                minLength={2}
                maxLength={40}
                value={valor}
                onChange={(e) => setValor(e.target.value)}
                placeholder="Ex.: Ana Silva ou 20260001"
              />
            </label>
            <p className="help">
              Durante o teste, a docente vê o seu progresso, dispositivo
              aproximado e posição do cursor nesta página.
            </p>
            <button className="primary" disabled={ocupado}>
              {ocupado ? "A pedir entrada…" : "Pedir entrada"}
            </button>
          </form>
        </section>
      </main>
    );
  const p = estado?.pergunta;
  return (
    <main className="public-room">
      <header>
        <Logo />
        <span className="pill">
          {estado?.estado === "PENDENTE"
            ? "A aguardar validação"
            : estado?.estado || credencial.estado}
        </span>
      </header>
      <section className="join-card">
        <p className="eyebrow">{estado?.tituloTeste || "SALA DE TESTE"}</p>
        {mensagemErro}
        {estado?.estado === "PENDENTE" && (
          <>
            <h1>Pedido enviado</h1>
            <p>
              Aguarde a docente confirmar a sua entrada. Esta página verifica a
              sala automaticamente.
            </p>
          </>
        )}
        {estado?.estado === "RECUSADO" && (
          <>
            <h1>Entrada não autorizada</h1>
            <p>Confirme os seus dados com a docente.</p>
          </>
        )}
        {estado?.estado === "FECHADO" && (
          <>
            <h1>Sessão encerrada</h1>
            <p>
              Respondeu a {estado.respondidas} de {estado.totalPerguntas}{" "}
              perguntas.
            </p>
            <strong className="nota-final">{estado.nota}%</strong>
            <p>A docente pode consultar a lista final de resultados.</p>
          </>
        )}
        {estado?.estado === "APROVADO" && p && (
          <section className="card question-preview" onPointerMove={cursor}>
            <div className="student-progress">
              Pergunta {p.numero} de {estado.totalPerguntas}
              <span>{tempo}s</span>
            </div>
            <progress max={estado.totalPerguntas} value={estado.respondidas} />
            <p className="eyebrow">
              {p.tipo === "VERDADEIRO_FALSO"
                ? "VERDADEIRO OU FALSO"
                : p.categoria || "ESCOLHA UMA ALTERNATIVA"}
            </p>
            <h1>{p.enunciado}</h1>
            <div className="student-options">
              {p.opcoes.map((o) => (
                <label
                  key={o.codigo}
                  className={escolha === o.codigo ? "option-selected" : ""}
                >
                  <input
                    type="radio"
                    name="resposta"
                    value={o.codigo}
                    checked={escolha === o.codigo}
                    disabled={ocupado || respostaEnviada || tempo <= 0}
                    onChange={() => {
                      setEscolha(o.codigo);
                      void enviar(o.codigo);
                    }}
                  />
                  <b>{o.codigo}</b>
                  {o.texto}
                </label>
              ))}
            </div>
            <small role="status">
              {ocupado
                ? "A resposta está a ser registada…"
                : respostaEnviada
                  ? "Resposta registada. A pergunta seguinte será apresentada automaticamente."
                  : "Seleccione uma alternativa para a enviar automaticamente."}
            </small>
            <small>
              O seu cursor ou toque aparece na área da pergunta, para a docente
              acompanhar a actividade.
            </small>
          </section>
        )}
        {estado?.estado === "APROVADO" && !p && estado.terminado && (
          <>
            <h1>Respostas registadas</h1>
            <p>
              Concluiu {estado.respondidas} de {estado.totalPerguntas}. Aguarde
              a docente encerrar a sessão para ver o resultado.
            </p>
            <progress max={estado.totalPerguntas} value={estado.respondidas} />
          </>
        )}
      </section>
    </main>
  );
}
