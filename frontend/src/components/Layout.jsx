import { NavLink } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { canAccess } from "../access";
import {
    IconUsers,
    IconBriefcase,
    IconWallet,
    IconCreditCard,
    IconReceipt,
    IconChartBar,
    IconChartPie,
    IconShirt,
    IconBox,
    IconClipboardList,
    IconLock,
    IconKey,
    IconStore,
    IconFileSpreadsheet,
    IconLogOut,
} from "./icons";

const navItems = [
    { to: "/promotores", page: "promotores", label: "Promotores", icon: IconUsers },
    { to: "/lojas", page: "lojas", label: "Lojas", icon: IconStore },
    { to: "/clientes", page: "clientes", label: "Clientes", icon: IconBriefcase },
    { to: "/descritivos", page: "descritivos", label: "Descritivos", icon: IconFileSpreadsheet },
    { to: "/faturamento", page: "faturamento", label: "Faturamento", icon: IconWallet },
    { to: "/folha-pagamento", page: "folha-pagamento", label: "Folha de pagamento", icon: IconCreditCard },
    { to: "/despesas", page: "despesas", label: "Despesas", icon: IconReceipt },
    { to: "/relatorios", page: "relatorios", label: "Relatórios", icon: IconChartBar },
    { to: "/indicadores-rh", page: "indicadores-rh", label: "Indicadores RH", icon: IconChartPie },
    { to: "/uniformes", page: "uniformes", label: "Uniformes e materiais", icon: IconShirt },
    { to: "/estoque", page: "estoque", label: "Estoque", icon: IconBox },
    { to: "/solicitacoes", page: "solicitacoes", label: "Solicitações", icon: IconClipboardList },
    { to: "/redefinicoes-senha", page: "redefinicoes-senha", label: "Redefinições de senha", icon: IconLock },
    { to: "/usuarios", page: "usuarios", label: "Usuários", icon: IconKey },
];

function getInitials(name) {
    if (!name) return "?";
    const parts = name.trim().split(/\s+/);
    if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

function todayLabel() {
    const formatted = new Date().toLocaleDateString("pt-BR", {
        weekday: "long",
        day: "2-digit",
        month: "long",
    });
    return formatted.charAt(0).toUpperCase() + formatted.slice(1);
}

export default function Layout({ title, children }) {
    const { logout, userName, jobTittle } = useAuth();
    const visibleNavItems = navItems.filter((item) => canAccess(jobTittle, item.page));

    return (
        <div className="flex h-screen w-screen bg-neutral-100 text-neutral-800">
            <aside className="relative flex w-64 flex-shrink-0 flex-col overflow-hidden bg-black text-white">
                <div
                    className="pointer-events-none absolute -left-16 -top-20 h-56 w-56 rounded-full bg-orange-500 blur-3xl"
                    style={{ opacity: 0.18 }}
                />

                <div className="relative flex items-center gap-2 border-b border-neutral-800 px-6 py-5">
                    <div className="flex h-8 w-8 items-center justify-center rounded-md bg-orange-500 text-sm font-bold text-black">
                        AP
                    </div>
                    <span className="text-lg font-semibold tracking-wide">At Promo</span>
                </div>

                <nav className="relative flex-1 space-y-1 overflow-y-auto px-3 py-4">
                    {visibleNavItems.map((item) => {
                        const Icon = item.icon;
                        return (
                            <NavLink
                                key={item.to}
                                to={item.to}
                                className={({ isActive }) =>
                                    `flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                                        isActive
                                            ? "bg-orange-500 text-black"
                                            : "text-neutral-300 hover:bg-neutral-800 hover:text-orange-400"
                                    }`
                                }
                            >
                                <Icon className="h-4.5 w-4.5 flex-shrink-0" />
                                <span className="truncate">{item.label}</span>
                            </NavLink>
                        );
                    })}
                </nav>

                <div className="relative space-y-3 border-t border-neutral-800 p-3">
                    <div className="flex items-center gap-3 px-1">
                        <div className="flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-full bg-orange-500 text-sm font-bold text-black ring-2 ring-orange-500/30">
                            {getInitials(userName)}
                        </div>
                        <div className="min-w-0">
                            <p className="truncate text-sm font-medium text-white">{userName || "Usuário"}</p>
                            {jobTittle && <p className="truncate text-xs text-neutral-400">{jobTittle}</p>}
                        </div>
                    </div>

                    <button
                        onClick={logout}
                        className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm font-medium text-neutral-300 hover:bg-neutral-800 hover:text-orange-400"
                    >
                        <IconLogOut className="h-4.5 w-4.5" />
                        Sair
                    </button>
                </div>
            </aside>

            <div className="flex flex-1 flex-col overflow-hidden">
                <header className="flex items-center justify-between border-b border-neutral-200 bg-white px-8 py-4 shadow-sm">
                    <h1 className="text-xl font-semibold text-black">{title}</h1>
                    <span className="text-sm text-neutral-400">{todayLabel()}</span>
                </header>

                <main className="flex-1 overflow-auto p-8">{children}</main>
            </div>
        </div>
    );
}
