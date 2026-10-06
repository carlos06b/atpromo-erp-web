export function isRh(jobTittle) {
    return (jobTittle || "").trim().toUpperCase() === "RH";
}

export function isFinance(jobTittle) {
    return (jobTittle || "").trim().toUpperCase() === "FINANCEIRO";
}

export function isSupervisor(jobTittle) {
    return (jobTittle || "").trim().toUpperCase() === "SUPERVISOR";
}

export function isAdmin(jobTittle) {
    return (jobTittle || "").trim().toUpperCase() === "ADMIN";
}

// ACHADO C2 (crítico) da auditoria de 05/10/2026: getRole() tinha "ADMIN"
// como valor default para QUALQUER cargo que não fosse RH/FINANCEIRO/
// SUPERVISOR — ou seja, um cargo nulo, vazio ou desconhecido virava admin.
// Agora retorna null para um cargo não reconhecido, que canAccess()/
// defaultPageFor() tratam como "sem acesso a nada" (ver abaixo).
export function getRole(jobTittle) {
    if (isRh(jobTittle)) return "RH";
    if (isFinance(jobTittle)) return "FINANCEIRO";
    if (isSupervisor(jobTittle)) return "SUPERVISOR";
    if (isAdmin(jobTittle)) return "ADMIN";
    return null;
}

export const PAGE_ORDER = [
    "promotores",
    "lojas",
    "clientes",
    "descritivos",
    "faturamento",
    "folha-pagamento",
    "despesas",
    "extrato",
    "relatorios",
    "indicadores-rh",
    "uniformes",
    "estoque",
    "solicitacoes",
    "redefinicoes-senha",
    "usuarios",
];

export const PAGE_ACCESS = {
    promotores: ["RH", "SUPERVISOR", "ADMIN"],
    lojas: ["RH", "FINANCEIRO", "SUPERVISOR", "ADMIN"],
    clientes: ["FINANCEIRO", "ADMIN"],
    descritivos: ["FINANCEIRO", "SUPERVISOR", "ADMIN"],
    faturamento: ["FINANCEIRO", "ADMIN"],
    "folha-pagamento": ["RH", "FINANCEIRO", "ADMIN"],
    despesas: ["FINANCEIRO", "ADMIN"],
    extrato: ["FINANCEIRO", "ADMIN"],
    relatorios: ["FINANCEIRO", "ADMIN"],
    "indicadores-rh": ["RH", "ADMIN"],
    uniformes: ["RH", "ADMIN"],
    estoque: ["RH", "ADMIN"],
    solicitacoes: ["RH", "FINANCEIRO", "ADMIN"],
    "redefinicoes-senha": ["ADMIN"],
    usuarios: ["ADMIN"],
};

export function canAccess(jobTittle, page) {
    const role = getRole(jobTittle);
    if (!role) return false;

    const allowed = PAGE_ACCESS[page];
    if (!allowed) return true;
    return allowed.includes(role);
}

export function defaultPageFor(jobTittle) {
    const page = PAGE_ORDER.find((p) => canAccess(jobTittle, p));
    return page ? `/${page}` : "/login";
}
