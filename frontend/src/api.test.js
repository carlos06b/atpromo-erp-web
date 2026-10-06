import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import { apiFetch, setUnauthorizedHandler } from "./api";

/**
 * ACHADO B1 (corrigido): apiFetch não tratava 401 de nenhuma forma
 * especial — não existia nenhum lugar no código que, ao receber 401,
 * disparasse logout()/redirecionamento automático pro /login. Um token
 * expirado ou inválido só gerava um Error genérico que cada tela mostrava
 * à sua maneira (quando mostrava).
 *
 * Agora apiFetch chama um "unauthorized handler" registrado via
 * setUnauthorizedHandler() sempre que recebe 401 — é isso que o
 * AuthContext usa pra se auto-registrar com o logout() de verdade (ver
 * src/context/AuthContext.jsx). Continua lançando o Error normalmente.
 */
describe("apiFetch", () => {
  beforeEach(() => {
    global.fetch = vi.fn();
    setUnauthorizedHandler(null);
  });

  afterEach(() => {
    vi.restoreAllMocks();
    setUnauthorizedHandler(null);
  });

  test("em caso de sucesso, retorna o JSON parseado", async () => {
    global.fetch.mockResolvedValue({
      ok: true,
      text: async () => JSON.stringify({ hello: "world" }),
    });

    const result = await apiFetch("/qualquer-coisa", { token: "abc" });
    expect(result).toEqual({ hello: "world" });
  });

  test("em caso de resposta vazia (204), retorna null em vez de lançar erro de parse", async () => {
    global.fetch.mockResolvedValue({ ok: true, text: async () => "" });
    const result = await apiFetch("/delete-algo", { method: "DELETE", token: "abc" });
    expect(result).toBeNull();
  });

  test("usa a mensagem de erro do corpo da resposta quando existe", async () => {
    global.fetch.mockResolvedValue({
      ok: false,
      status: 400,
      json: async () => ({ message: "Dados inválidos." }),
    });

    await expect(apiFetch("/algo")).rejects.toThrow("Dados inválidos.");
  });

  test("401 continua lançando o erro normalmente, mesmo com um handler registrado", async () => {
    global.fetch.mockResolvedValue({
      ok: false,
      status: 401,
      json: async () => ({ message: "Token inválido ou expirado." }),
    });

    await expect(apiFetch("/qualquer-rota-protegida", { token: "expirado" }))
        .rejects.toThrow("Token inválido ou expirado.");
  });

  test("ACHADO B1 (corrigido): 401 chama o unauthorized handler registrado (ex: logout do AuthContext)", async () => {
    global.fetch.mockResolvedValue({
      ok: false,
      status: 401,
      json: async () => ({ message: "Token inválido ou expirado." }),
    });

    const handler = vi.fn();
    setUnauthorizedHandler(handler);

    await expect(apiFetch("/qualquer-rota-protegida", { token: "expirado" })).rejects.toThrow();

    expect(handler).toHaveBeenCalledTimes(1);
  });

  test("erros que não são 401 não chamam o unauthorized handler", async () => {
    global.fetch.mockResolvedValue({
      ok: false,
      status: 403,
      json: async () => ({ message: "Sem permissão." }),
    });

    const handler = vi.fn();
    setUnauthorizedHandler(handler);

    await expect(apiFetch("/algo")).rejects.toThrow();

    expect(handler).not.toHaveBeenCalled();
  });

  test("se nenhum handler foi registrado, 401 só lança o erro (não quebra)", async () => {
    global.fetch.mockResolvedValue({
      ok: false,
      status: 401,
      json: async () => ({ message: "Token inválido ou expirado." }),
    });

    await expect(apiFetch("/qualquer-rota-protegida", { token: "expirado" }))
        .rejects.toThrow("Token inválido ou expirado.");
  });

  test("quando o corpo do erro não é JSON válido, cai no fallback 'Erro <status>'", async () => {
    global.fetch.mockResolvedValue({
      ok: false,
      status: 500,
      json: async () => {
        throw new Error("não é json");
      },
    });

    await expect(apiFetch("/algo")).rejects.toThrow("Erro 500");
  });

  test("envia o header Authorization só quando um token é passado", async () => {
    global.fetch.mockResolvedValue({ ok: true, text: async () => "" });

    await apiFetch("/sem-token");
    expect(global.fetch.mock.calls[0][1].headers.Authorization).toBeUndefined();

    await apiFetch("/com-token", { token: "meu-token" });
    expect(global.fetch.mock.calls[1][1].headers.Authorization).toBe("Bearer meu-token");
  });
});
