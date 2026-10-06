import { describe, expect, test } from "vitest";
import { canAccess, defaultPageFor, getRole, isAdmin, isFinance, isRh, isSupervisor, PAGE_ACCESS } from "./access";

/**
 * ACHADO C2 (crítico) da auditoria de 05/10/2026, agora corrigido:
 * access.js#getRole() tinha o mesmo padrão de "deny-list" do backend
 * (AccessControl.isAdmin): qualquer jobTittle que não fosse exatamente RH,
 * FINANCEIRO ou SUPERVISOR era tratado como ADMIN na interface.
 *
 * Estes testes comprovam a correção: getRole() agora retorna null pra um
 * cargo desconhecido, e canAccess() trata null como "sem acesso a nada".
 */
describe("access.js — getRole", () => {
  test("retorna RH/FINANCEIRO/SUPERVISOR/ADMIN para os cargos exatos (case/espaço insensível)", () => {
    expect(getRole("RH")).toBe("RH");
    expect(getRole("  rh ")).toBe("RH");
    expect(getRole("Financeiro")).toBe("FINANCEIRO");
    expect(getRole("SUPERVISOR")).toBe("SUPERVISOR");
    expect(getRole("Admin")).toBe("ADMIN");
  });

  test("CORRIGIDO: cargo nulo, vazio ou desconhecido não vira mais ADMIN (retorna null)", () => {
    expect(getRole(null)).toBeNull();
    expect(getRole(undefined)).toBeNull();
    expect(getRole("")).toBeNull();
    expect(getRole("   ")).toBeNull();
    expect(getRole("Estoquista")).toBeNull();
    expect(getRole("RH ")).toBe("RH"); // esse continua intencional (trim) — não é bug
  });
});

describe("access.js — isRh / isFinance / isSupervisor / isAdmin", () => {
  test("são case/trim insensíveis e não dão falso positivo entre si", () => {
    expect(isRh("rh")).toBe(true);
    expect(isFinance("rh")).toBe(false);
    expect(isSupervisor("rh")).toBe(false);
    expect(isAdmin("rh")).toBe(false);
  });

  test("isAdmin só é true pra jobTittle 'ADMIN' explícito", () => {
    expect(isAdmin("ADMIN")).toBe(true);
    expect(isAdmin("  admin ")).toBe(true);
    expect(isAdmin("Estoquista")).toBe(false);
    expect(isAdmin(null)).toBe(false);
  });
});

describe("access.js — canAccess / PAGE_ACCESS", () => {
  test("CORRIGIDO: usuário sem cargo reconhecido não acessa NENHUMA página da matriz", () => {
    for (const page of Object.keys(PAGE_ACCESS)) {
      expect(canAccess("CARGO-QUE-NAO-EXISTE", page)).toBe(false);
    }
    expect(canAccess(null, "promotores")).toBe(false);
    expect(canAccess("", "promotores")).toBe(false);
  });

  test("RH não acessa páginas exclusivas de Financeiro/Admin", () => {
    expect(canAccess("RH", "faturamento")).toBe(false);
    expect(canAccess("RH", "extrato")).toBe(false);
    expect(canAccess("RH", "usuarios")).toBe(false);
  });

  test("ADMIN explícito continua acessando tudo", () => {
    for (const page of Object.keys(PAGE_ACCESS)) {
      expect(canAccess("ADMIN", page)).toBe(true);
    }
  });

  test("CORRIGIDO: defaultPageFor leva cargo desconhecido para /login, não mais para a home do admin", () => {
    expect(defaultPageFor("CARGO-QUE-NAO-EXISTE")).toBe("/login");
  });

  test("defaultPageFor continua levando ADMIN pra primeira página da lista", () => {
    expect(defaultPageFor("ADMIN")).toBe("/promotores");
  });
});
