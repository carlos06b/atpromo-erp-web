import { NavLink } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { canAccess } from "../access";

const navItems = [
    { to: "/promotores", page: "promotores", label: "Promotores" },
    { to: "/clientes", page: "clientes", label: "Clientes" },
    { to: "/faturamento", page: "faturamento", label: "Faturamento" },
    { to: "/folha-pagamento", page: "folha-pagamento", label: "Folha de pagamento" },
    { to: "/despesas", page: "despesas", label: "Despesas" },
    { to: "/relatorios", page: "relatorios", label: "Relatórios" },
    { to: "/indicadores-rh", page: "indicadores-rh", label: "Indicadores RH" },
    { to: "/uniformes", page: "uniformes", label: "Uniformes e materiais" },
    { to: "/estoque", page: "estoque", label: "Estoque" },
    { to: "/solicitacoes", page: "solicitacoes", label: "Solicitações" },
];

function getInitials(name) {
    if (!name) return "?";
    const parts = name.trim().split(/\s+/);
    if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

export default function Layout({ title, children }) {
    const { logout, userName, jobTittle } = useAuth();
    const visibleNavItems = navItems.filter((item) => canAccess(jobTittle, item.page));

    return (
        <div className="flex h-screen w-screen bg-neutral-100 text-neutral-800">
            <aside className="flex w-64 flex-shrink-0 flex-col bg-black text-white">
                <div className="flex items-center gap-2 px-6 py-5 border-b border-neutral-800">
                    <div className="flex h-8 w-8 items-center justify-center rounded-md bg-orange-500 text-sm font-bold text-black">
                        AP
                    </div>
                    <span className="text-lg font-semibold tracking-wide">At Promo</span>
                </div>

                <nav className="flex-1 px-3 py-4 space-y-1">
                    {visibleNavItems.map((item) => (
                        <NavLink
                            key={item.to}
                            to={item.to}
                            className={({ isActive }) =>
                                `block rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                                    isActive
                                        ? "bg-orange-500 text-black"
                                        : "text-neutral-300 hover:bg-neutral-800 hover:text-orange-400"
                                }`
                            }
                        >
                            {item.label}
                        </NavLink>
                    ))}
                </nav>

                <div className="border-t border-neutral-800 p-3 space-y-3">
                    <div className="flex items-center gap-3 px-1">
                        <div className="flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-full bg-orange-500 text-sm font-bold text-black">
                            {getInitials(userName)}
                        </div>
                        <div className="min-w-0">
                            <p className="truncate text-sm font-medium text-white">{userName || "Usuário"}</p>
                            {jobTittle && <p className="truncate text-xs text-neutral-400">{jobTittle}</p>}
                        </div>
                    </div>

                    <button
                        onClick={logout}
                        className="w-full rounded-lg px-3 py-2 text-left text-sm font-medium text-neutral-300 hover:bg-neutral-800 hover:text-orange-400"
                    >
                        Sair
                    </button>
                </div>
            </aside>

            <div className="flex flex-1 flex-col overflow-hidden">
                <header className="flex items-center justify-between border-b-2 border-orange-500 bg-white px-8 py-4 shadow-sm">
                    <h1 className="text-xl font-semibold text-black">{title}</h1>
                </header>

                <main className="flex-1 overflow-auto p-8">{children}</main>
            </div>
        </div>
    );
}
