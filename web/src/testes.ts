export type Alternativa = "A" | "B" | "C" | "D";
export type TipoQuestao = "MULTIPLA_ESCOLHA" | "VERDADEIRO_FALSO";
export const alternativas: Alternativa[] = ["A", "B", "C", "D"];
export interface Questao {
  enunciado: string;
  alternativaA: string;
  alternativaB: string;
  alternativaC: string;
  alternativaD: string;
  correcta: Alternativa;
  categoria: string;
  tipo: TipoQuestao;
}
export interface Teste {
  id: string;
  titulo: string;
  descricao: string;
  versao: number;
  disponivelPratica: boolean;
  criadoEm: string;
  actualizadoEm: string;
  perguntas: Questao[];
}
export interface ResumoTeste {
  id: string;
  titulo: string;
  versao: number;
  disponivelPratica: boolean;
  totalPerguntas: number;
  actualizadoEm: string;
}
export interface RevisaoTeste {
  versao: number;
  guardadoEm: string;
}
export interface PaginaTeste<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
}
export function dataHora(data: string) {
  return new Date(data).toLocaleString("pt-PT");
}
