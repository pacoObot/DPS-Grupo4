"use strict";

const $ = (id) => document.getElementById(id);
let ws = null;
let teamName = "";
let currentQid = -1;
let answered = false;
let timerId = null;
let totalTime = 15;

/* ---------------------------------------------------------------- screen */
function show(id) {
  document.querySelectorAll(".screen").forEach((s) => s.classList.remove("active"));
  $(id).classList.add("active");
}

function conn(state) {
  const el = $("conn");
  el.textContent = state;
  el.classList.toggle("online", state === "online");
  el.classList.toggle("offline", state !== "online");
}

function banner(el, text, kind) {
  el.textContent = text;
  el.className = "banner " + (kind || "info");
  el.classList.remove("hidden");
}

/* ------------------------------------------------------------------- ws */
function connect() {
  const proto = location.protocol === "https:" ? "wss" : "ws";
  ws = new WebSocket(proto + "://" + location.host + "/ws");

  ws.onopen = () => conn("online");
  ws.onclose = () => {
    conn("offline");
    stopTimer();
  };
  ws.onerror = () => conn("offline");
  ws.onmessage = (ev) => {
    let msg;
    try { msg = JSON.parse(ev.data); } catch (e) { return; }
    handle(msg);
  };
}

function send(obj) {
  if (ws && ws.readyState === WebSocket.OPEN) ws.send(JSON.stringify(obj));
}

/* --------------------------------------------------------------- actions */
function join() {
  const code = ($("code").value || "").trim().toUpperCase();
  const team = ($("team").value || "").trim();
  $("code").classList.remove("err");
  $("team").classList.remove("err");

  if (!code) { $("code").classList.add("err"); banner($("join-status"), "Introduza o código da partida.", "error"); return; }
  if (!team) { $("team").classList.add("err"); banner($("join-status"), "Escolha um nome de equipa.", "error"); return; }

  teamName = team;
  $("btn-join").disabled = true;
  $("btn-join").textContent = "A LIGAR…";
  banner($("join-status"), "A ligar ao servidor…", "info");

  send({
    action: "join",
    host: ($("host").value || "127.0.0.1").trim(),
    port: parseInt($("port").value || "5050", 10),
    code: code,
    team: team,
  });
}

function answer(option) {
  if (answered || currentQid < 0) return;
  answered = true;
  stopTimer();
  document.querySelectorAll(".opt").forEach((b) => (b.disabled = true));
  const btn = document.querySelector('.opt[data-opt="' + option + '"]');
  if (btn) btn.classList.add("correct");
  banner($("q-feedback"), "Resposta enviada (" + option + ") — a aguardar as outras equipas…", "info");
  send({ action: "answer", qid: currentQid, option: option });
}

function joinBusy(busy) {
  $("btn-join").disabled = busy;
  $("btn-join").textContent = busy ? "A LIGAR…" : "ENTRAR";
}

/* ---------------------------------------------------------------- render */
function showQuestion(f) {
  if (f.length < 9) return;
  answered = false;
  currentQid = parseInt(f[2], 10);
  const limit = parseInt(f[3], 10) || 15;
  totalTime = limit;

  $("q-round").textContent = "Ronda " + f[0] + " / " + f[1];
  $("q-text").textContent = f[4];
  $("q-feedback").classList.add("hidden");
  $("paused-banner").classList.add("hidden");

  const opts = document.querySelectorAll(".opt");
  ["A", "B", "C", "D"].forEach((letter, i) => {
    const btn = opts[i];
    btn.disabled = false;
    btn.classList.remove("correct", "wrong");
    btn.querySelector("em").textContent = f[5 + i] || "";
  });

  startTimer(limit);
}

function startTimer(limit) {
  stopTimer();
  let remaining = limit;
  const tick = () => {
    $("q-timer").textContent = remaining + "s";
    $("q-bar").style.width = (remaining / totalTime) * 100 + "%";
    $("q-timer").classList.toggle("critical", remaining <= 5);
    $("q-bar").classList.toggle("low", remaining <= 5);
    if (remaining <= 0) {
      stopTimer();
      document.querySelectorAll(".opt").forEach((b) => (b.disabled = true));
      banner($("q-feedback"), "⏱ Tempo esgotado!", "warn");
      return;
    }
    remaining--;
  };
  tick();
  timerId = setInterval(tick, 1000);
}

function stopTimer() {
  if (timerId) { clearInterval(timerId); timerId = null; }
}

function renderBoard(f) {
  const box = $("q-board");
  if (f.length === 0) { box.innerHTML = '<div class="muted">Sem classificação ainda.</div>'; return; }
  box.innerHTML = "";
  f.forEach((entry) => {
    const p = entry.split(":");
    if (p.length < 6) return;
    const row = document.createElement("div");
    row.className = "row" + (p[1] === teamName ? " me" : "");
    row.innerHTML =
      "<span>" + p[0] + "º " + p[1] + "</span>" +
      "<span>" + p[2] + " pts</span>";
    box.appendChild(row);
  });
}

function renderResult(f) {
  const podium = $("podium");
  const rank = $("rank");
  podium.innerHTML = "";
  rank.innerHTML = "";
  const rest = [];
  f.forEach((entry) => {
    const p = entry.split(":");
    if (p.length < 6) return;
    const pos = parseInt(p[0], 10);
    if (pos <= 3) {
      const medal = pos === 1 ? "🥇" : pos === 2 ? "🥈" : "🥉";
      const div = document.createElement("div");
      div.className = "pod p" + pos;
      div.innerHTML = '<div class="medal">' + medal + "</div>" +
        '<div class="name">' + p[1] + "</div>" +
        '<div class="pts">' + p[2] + " pts</div>";
      podium.appendChild(div);
    } else {
      rest.push('<div class="row">' + p[0] + "º  " + p[1] + "   " + p[2] + " pts</div>");
    }
  });
  rank.innerHTML = rest.join("");
}

/* -------------------------------------------------------------- dispatch */
function handle(msg) {
  const f = msg.fields || [];
  switch (msg.type) {
    case "CONNECTED":
      conn("online");
      break;

    case "JOIN_OK": {
      joinBusy(false);
      $("lobby-code").textContent = ($("code").value || "").trim().toUpperCase();
      $("lobby-team").textContent = "Equipa " + teamName + "  ·  " + (f[2] || "");
      $("lobby-waiting").textContent = "A aguardar equipas…";
      banner($("lobby-status"), "Entrou na partida. Aguarde o início.", "info");
      show("screen-lobby");
      break;
    }

    case "JOIN_ERROR":
      joinBusy(false);
      banner($("join-status"), f[0] || "Não foi possível entrar.", "error");
      show("screen-join");
      break;

    case "WAITING":
      if (f.length >= 2) {
        $("lobby-waiting").textContent = f[0] + " de " + f[1] + " equipas ligadas";
      }
      break;

    case "GAME_STARTED":
      show("screen-quiz");
      break;

    case "GAME_PAUSED":
      $("paused-banner").classList.remove("hidden");
      stopTimer();
      document.querySelectorAll(".opt").forEach((b) => (b.disabled = true));
      show("screen-quiz");
      break;

    case "GAME_RESUMED":
      $("paused-banner").classList.add("hidden");
      break;

    case "GAME_STOPPED":
      $("paused-banner").classList.remove("hidden");
      stopTimer();
      break;

    case "QUESTION":
      show("screen-quiz");
      showQuestion(f);
      break;

    case "ANSWER_RESULT": {
      stopTimer();
      document.querySelectorAll(".opt").forEach((b) => (b.disabled = true));
      $("q-points").textContent = f[4] || "0";
      const status = f[1];
      if (status === "CORRECT") banner($("q-feedback"), "✔ Correto! +" + f[2] + " pts (bónus " + f[3] + ")", "success");
      else if (status === "WRONG") banner($("q-feedback"), "✘ Errada.", "error");
      else if (status === "TIMEOUT") banner($("q-feedback"), "⏱ Tempo esgotado!", "warn");
      else banner($("q-feedback"), "Resposta registada: " + status, "info");
      break;
    }

    case "NEXT_ROUND":
      stopTimer();
      $("q-text").textContent = "Ronda " + (f[0] || "?") + " a começar…";
      $("q-timer").textContent = "—";
      $("q-bar").style.width = "0%";
      break;

    case "SCOREBOARD":
      renderBoard(f);
      break;

    case "GAME_OVER":
      stopTimer();
      $("result-title").textContent = "Fim de jogo — " + (teamName || "");
      show("screen-result");
      break;

    case "ERROR":
      if (msg.fields && msg.fields.length) banner($("q-feedback"), f[0], "error");
      break;

    case "LOCAL_ERROR":
      joinBusy(false);
      banner($("join-status"), msg.message || "Erro local.", "error");
      show("screen-join");
      break;

    case "DISCONNECTED":
      joinBusy(false);
      stopTimer();
      banner($("join-status"), "Ligação terminada: " + (msg.message || "") + " — tente novamente.", "error");
      show("screen-join");
      break;

    default:
      break;
  }
}

/* ------------------------------------------------------------------ init */
document.querySelectorAll(".opt").forEach((btn) => {
  btn.addEventListener("click", () => answer(btn.dataset.opt));
});
$("btn-join").addEventListener("click", join);
$("team").addEventListener("keydown", (e) => { if (e.key === "Enter") join(); });
$("btn-again").addEventListener("click", () => {
  joinBusy(false);
  $("q-points").textContent = "0";
  $("q-board").innerHTML = "";
  if (!ws || ws.readyState !== WebSocket.OPEN) connect();
  show("screen-join");
});

connect();