import React from "react";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import ConfirmDeleteDialog from "./ConfirmDeleteDialog";
import { AuthProvider } from "../context/AuthContext";

/**
 * ACHADO C6 (crítico), lado do frontend: antes, onConfirmed() era chamado
 * sem nenhuma informação vinda da verificação de senha — agora precisa
 * repassar o ticket devolvido por /account/verify-password, que o DELETE
 * real envia no header X-Delete-Confirmation (ver api.js e
 * DeleteConfirmationFilter no backend).
 */
describe("ConfirmDeleteDialog", () => {
  beforeEach(() => {
    global.fetch = vi.fn();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  function digitarSenhaEConfirmar(senha) {
    const input = document.querySelector('input[type="password"]');
    fireEvent.change(input, { target: { value: senha } });
    fireEvent.click(screen.getByRole("button", { name: /excluir definitivamente/i }));
  }

  test("senha correta: onConfirmed recebe o ticket devolvido pelo backend", async () => {
    global.fetch.mockResolvedValue({
      ok: true,
      text: async () => JSON.stringify({ ticket: "ticket-123" }),
    });

    const onConfirmed = vi.fn();
    render(
      <AuthProvider>
        <ConfirmDeleteDialog open onClose={() => {}} onConfirmed={onConfirmed} itemLabel="este item" />
      </AuthProvider>
    );

    digitarSenhaEConfirmar("SenhaForte123!");

    await waitFor(() => expect(onConfirmed).toHaveBeenCalledWith("ticket-123"));
  });

  test("senha incorreta: mostra erro e NÃO chama onConfirmed", async () => {
    global.fetch.mockResolvedValue({ ok: false, status: 401, json: async () => ({ message: "Senha incorreta." }) });

    const onConfirmed = vi.fn();
    render(
      <AuthProvider>
        <ConfirmDeleteDialog open onClose={() => {}} onConfirmed={onConfirmed} itemLabel="este item" />
      </AuthProvider>
    );

    digitarSenhaEConfirmar("senha-errada");

    await waitFor(() => expect(screen.getByText(/senha incorreta/i)).toBeInTheDocument());
    expect(onConfirmed).not.toHaveBeenCalled();
  });
});
