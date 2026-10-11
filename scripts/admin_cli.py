#!/usr/bin/env python3
"""
admin_cli.py — cliente de administração mínimo (protocolo TCP em texto).

Permite criar perguntas/partidas e iniciar uma partida SEM a interface JavaFX —
útil para demonstrações automáticas e testes de fumo.

Uso:
    admin_cli.py create --host 127.0.0.1 --port 5050 --questions 3 --time 10 --name Demo
        -> imprime:  GAMEID=<id> CODE=<codigo>

    admin_cli.py start  --host 127.0.0.1 --port 5050 --game <id>

As respostas corretas das perguntas geradas são sempre a opção A.
"""
import argparse
import socket
import sys

ADMIN_USER = "admin"
ADMIN_PASS = "admin"


class AdminClient:
    def __init__(self, host, port):
        self.sock = socket.create_connection((host, port), timeout=10)
        self.sock.settimeout(10)
        self.buf = b""
        self._send(f"ADMIN_LOGIN|{ADMIN_USER}|{ADMIN_PASS}")
        line = self._read()
        if not line or not line.startswith("LOGIN_OK"):
            raise SystemExit(f"Falha no login de admin: {line!r}")

    def _send(self, msg):
        self.sock.sendall((msg + "\n").encode("utf-8"))

    def _read(self):
        while b"\n" not in self.buf:
            chunk = self.sock.recv(4096)
            if not chunk:
                return None
            self.buf += chunk
        line, self.buf = self.buf.split(b"\n", 1)
        return line.decode("utf-8", "replace")

    def request(self, msg, expect):
        """Envia e devolve a primeira linha que começa por `expect` (ignora eventos)."""
        self._send(msg)
        for _ in range(50):
            line = self._read()
            if line is None:
                return None
            if line.startswith(expect):
                return line
        return None

    def close(self):
        try:
            self.sock.close()
        except OSError:
            pass


def create(args):
    c = AdminClient(args.host, args.port)
    qids = []
    for i in range(1, args.questions + 1):
        reply = c.request(
            f"CREATE_QUESTION|Pergunta {i}?|Demo|EASY|"
            f"Opção A{ i}|Opção B{i}|Opção C{i}|Opção D{i}|A|10|{args.time}|true",
            "QUESTION_CREATED")
        if not reply:
            raise SystemExit("Sem resposta a CREATE_QUESTION")
        qids.append(reply.split("|")[1])

    reply = c.request(
        f"CREATE_GAME|{args.name}|{','.join(qids)}|{args.time}",
        "GAME_CREATED")
    if not reply:
        raise SystemExit("Sem resposta a CREATE_GAME")
    _, game_id, code = reply.split("|")
    c.close()
    print(f"GAMEID={game_id} CODE={code}")


def start(args):
    c = AdminClient(args.host, args.port)
    reply = c.request(f"START_GAME|{args.game}", "GAME_STATUS")
    c.close()
    print(f"STARTED {reply}")


def main():
    p = argparse.ArgumentParser(description="Cliente admin mínimo (protocolo de texto)")
    sub = p.add_subparsers(dest="cmd", required=True)

    common = dict()
    for name in ("create", "start"):
        sp = sub.add_parser(name)
        sp.add_argument("--host", default="127.0.0.1")
        sp.add_argument("--port", type=int, default=5050)

    pc = sub.choices["create"]
    pc.add_argument("--questions", type=int, default=3)
    pc.add_argument("--time", type=int, default=10)
    pc.add_argument("--name", default="Demo")
    pc.set_defaults(func=create)

    ps = sub.choices["start"]
    ps.add_argument("--game", required=True)
    ps.set_defaults(func=start)

    args = p.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
