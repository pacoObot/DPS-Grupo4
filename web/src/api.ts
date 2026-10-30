export type Perfil = "ADMINISTRADOR" | "DOCENTE" | "ESTUDANTE";
export interface Utilizador {
  id: string;
  nome: string;
  email: string;
  perfil: Perfil;
  activo: boolean;
  criadoEm: string;
}
export interface Sessao {
  accessToken: string;
  expiresIn: number;
  utilizador: Utilizador;
}
export interface Pagina {
  content: Utilizador[];
  page: number;
  size: number;
  totalElements: number;
}
export class ErroApi extends Error {
  constructor(
    public status: number,
    mensagem: string,
  ) {
    super(mensagem);
  }
}
export async function pedir<T>(
  rota: string,
  token?: string,
  metodo = "GET",
  dados?: unknown,
  signal?: AbortSignal,
): Promise<T> {
  let resposta: Response;
  try {
    resposta = await fetch("/api/v1" + rota, {
      method: metodo,
      signal,
      cache: "no-store",
      headers: {
        ...(token ? { Authorization: "Bearer " + token } : {}),
        ...(dados ? { "Content-Type": "application/json" } : {}),
      },
      body: dados ? JSON.stringify(dados) : undefined,
    });
  } catch (e) {
    if (signal?.aborted) throw e;
    throw new ErroApi(
      0,
      "Não foi possível contactar o servidor. Tente novamente.",
    );
  }
  const corpo = await resposta.json().catch(() => null);
  if (!resposta.ok)
    throw new ErroApi(
      resposta.status,
      resposta.status === 403
        ? "Não tem permissão para esta operação."
        : corpo?.mensagem || "Não foi possível concluir a operação.",
    );
  if (!corpo)
    throw new ErroApi(0, "O servidor devolveu uma resposta inválida.");
  return corpo as T;
}
export const perfis: Record<Perfil, string> = {
  ADMINISTRADOR: "Administrador",
  DOCENTE: "Docente",
  ESTUDANTE: "Estudante",
};
