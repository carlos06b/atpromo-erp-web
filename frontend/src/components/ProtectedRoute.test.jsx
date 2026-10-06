import React from "react";
import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, test } from "vitest";
import ProtectedRoute from "./ProtectedRoute";
import { AuthProvider } from "../context/AuthContext";

function renderProtected({ page, storage }) {
  if (storage) {
    for (const [key, value] of Object.entries(storage)) {
      localStorage.setItem(key, value);
    }
  }

  return render(
    <MemoryRouter initialEntries={["/pagina-protegida"]}>
      <AuthProvider>
        <Routes>
          <Route
            path="/pagina-protegida"
            element={
              <ProtectedRoute page={page}>
                <div>Conteúdo protegido</div>
              </ProtectedRoute>
            }
          />
          <Route path="/login" element={<div>Tela de login</div>} />
          <Route path="/promotores" element={<div>Tela de promotores (home do ADMIN)</div>} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>
  );
}

describe("ProtectedRoute", () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
  });

  test("sem token, redireciona pro /login", () => {
    renderProtected({ page: "usuarios" });
    expect(screen.getByText("Tela de login")).toBeInTheDocument();
  });

  test("com token e cargo permitido, mostra o conteúdo", () => {
    renderProtected({
      page: "folha-pagamento",
      storage: { token: "fake-token", userJobTittle: "RH" },
    });
    expect(screen.getByText("Conteúdo protegido")).toBeInTheDocument();
  });

  test("com token mas cargo sem permissão pra página, redireciona (não mostra o conteúdo)", () => {
    renderProtected({
      page: "usuarios", // só ADMIN
      storage: { token: "fake-token", userJobTittle: "RH" },
    });
    expect(screen.queryByText("Conteúdo protegido")).not.toBeInTheDocument();
  });

  test("CORRIGIDO (achado C2): cargo desconhecido guardado no localStorage não destrava mais páginas de ADMIN", () => {
    renderProtected({
      page: "usuarios",
      storage: { token: "qualquer-coisa-nao-validada-aqui", userJobTittle: "CARGO-INVENTADO" },
    });
    // Antes da correção, CARGO-INVENTADO caía no bug de getRole()/isAdmin() e
    // "usuarios" (ADMIN-only) ficava visível. Agora é redirecionado.
    expect(screen.queryByText("Conteúdo protegido")).not.toBeInTheDocument();
    expect(screen.getByText("Tela de login")).toBeInTheDocument();
  });

  test("ACHADO ainda válido: ProtectedRoute continua sendo só uma barreira de UX — um jobTittle RECONHECIDO guardado manualmente no localStorage (ex: via DevTools) já destrava a tela no front, sem nenhum token real do backend confirmando esse cargo", () => {
    renderProtected({
      page: "usuarios",
      storage: { token: "qualquer-coisa-nao-validada-aqui", userJobTittle: "ADMIN" },
    });
    // O front nunca confirma com o backend se esse token realmente pertence
    // a um admin — ele só olha o que está guardado no localStorage. A
    // correção do achado C2 fecha o "cargo desconhecido virar admin", mas
    // não transforma essa checagem de rota em um mecanismo de segurança: a
    // proteção real continua sendo (e precisa continuar sendo) a checagem
    // de papel no backend, endpoint por endpoint.
    expect(screen.getByText("Conteúdo protegido")).toBeInTheDocument();
  });
});
