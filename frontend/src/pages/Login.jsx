import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { apiFetch } from "../api";
import { useAuth } from "../context/AuthContext";
import {
    IconMail,
    IconLock,
    IconEye,
    IconEyeOff,
    IconSpinner,
    IconCheckDot,
    IconUsers,
    IconWallet,
    IconChartPie,
} from "../components/icons";

const HIGHLIGHTS = [
    { icon: IconUsers, text: "RH, financeiro e o cadastro de promotores" },
    { icon: IconWallet, text: "Faturamento, despesas e folha integrados" },
    { icon: IconChartPie, text: "Indicadores prontos, sem planilha" },
];

export default function Login() {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [remember, setRemember] = useState(true);
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);

    const [showForgot, setShowForgot] = useState(false);
    const [forgotEmail, setForgotEmail] = useState("");
    const [forgotStatus, setForgotStatus] = useState("idle"); // idle | loading | sent | error
    const [forgotError, setForgotError] = useState("");

    const { login } = useAuth();
    const navigate = useNavigate();

    async function handleSubmit(event) {
        event.preventDefault();
        setError("");
        setLoading(true);

        try {
            const data = await apiFetch("/auth/login", {
                method: "POST",
                body: { email, password, rememberMe: remember },
            });

            login(data.token, data.name, data.jobTittle, remember);
            navigate("/promotores");
        } catch (err) {
            setError(err.message || "Email ou senha inválidos.");
        } finally {
            setLoading(false);
        }
    }

    function openForgot() {
        setForgotEmail(email);
        setForgotStatus("idle");
        setForgotError("");
        setShowForgot(true);
    }

    async function handleForgotSubmit(event) {
        event.preventDefault();
        setForgotError("");
        setForgotStatus("loading");

        try {
            await apiFetch("/auth/forgot-password", {
                method: "POST",
                body: { email: forgotEmail },
            });
            setForgotStatus("sent");
        } catch (err) {
            setForgotError(err.message || "Não foi possível registrar sua solicitação.");
            setForgotStatus("idle");
        }
    }

    return (
        <div className="flex h-screen w-screen overflow-hidden bg-white">
            {/* Painel esquerdo - identidade visual, some em telas pequenas */}
            <div className="relative hidden w-[44%] flex-col justify-between overflow-hidden bg-black p-12 text-white lg:flex">
                <div
                    className="animate-glow pointer-events-none absolute -left-24 -top-24 h-96 w-96 rounded-full bg-orange-500 blur-3xl"
                    style={{ opacity: 0.35 }}
                />
                <div
                    className="animate-glow pointer-events-none absolute -bottom-32 -right-16 h-80 w-80 rounded-full bg-orange-600 blur-3xl"
                    style={{ opacity: 0.25, animationDelay: "2s" }}
                />

                <div className="relative flex items-center gap-2.5">
                    <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-orange-500 text-base font-bold text-black">
                        AP
                    </div>
                    <span className="text-lg font-semibold tracking-wide">At Promo</span>
                </div>

                <div className="relative">
                    <h2 className="text-3xl font-semibold leading-tight text-white">
                        RH, financeiro e o
                        <br />
                        cadastro de promotores,
                        <br />
                        em um só sistema.
                    </h2>
                    <p className="mt-4 max-w-sm text-sm text-neutral-400">
                        O sistema feito sob medida pra nossa operação — acessível de qualquer lugar,
                        pensado pra como a gente realmente trabalha.
                    </p>

                    <div className="mt-10 space-y-4">
                        {HIGHLIGHTS.map(({ icon: Icon, text }) => (
                            <div key={text} className="flex items-center gap-3">
                                <Icon className="h-5 w-5 flex-shrink-0 text-orange-400" />
                                <span className="text-sm text-neutral-300">{text}</span>
                            </div>
                        ))}
                    </div>
                </div>

                <p className="relative text-xs text-neutral-500">
                    © {new Date().getFullYear()} At Promo. Uso interno.
                </p>
            </div>

            {/* Painel direito - formulário */}
            <div className="flex flex-1 items-center justify-center bg-neutral-50 px-6">
                <div className="w-full max-w-sm">
                    <div className="mb-8 lg:hidden">
                        <div className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-xl bg-orange-500 text-lg font-bold text-black">
                            AP
                        </div>
                    </div>

                    <div className="rounded-3xl border border-neutral-200 bg-white p-8 shadow-xl shadow-neutral-200/60">
                        <div className="mb-7">
                            <h1 className="text-2xl font-semibold text-black">Bem-vindo de volta</h1>
                            <p className="mt-1 text-sm text-neutral-500">Entre com sua conta para continuar</p>
                        </div>

                        <form onSubmit={handleSubmit} className="space-y-4">
                            <div>
                                <label className="mb-1.5 block text-sm font-medium text-neutral-700">Email</label>
                                <div className="relative">
                                    <IconMail className="pointer-events-none absolute left-3 top-1/2 h-4.5 w-4.5 -translate-y-1/2 text-neutral-400" />
                                    <input
                                        type="email"
                                        value={email}
                                        onChange={(e) => setEmail(e.target.value)}
                                        required
                                        autoComplete="email"
                                        placeholder="voce@atpromo.com"
                                        className="w-full rounded-xl border border-neutral-300 py-2.5 pl-10 pr-3 text-sm outline-none transition-colors focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                                    />
                                </div>
                            </div>

                            <div>
                                <div className="mb-1.5 flex items-center justify-between">
                                    <label className="block text-sm font-medium text-neutral-700">Senha</label>
                                    <button
                                        type="button"
                                        onClick={openForgot}
                                        className="text-xs font-medium text-orange-600 hover:text-orange-700 hover:underline"
                                    >
                                        Esqueci minha senha
                                    </button>
                                </div>
                                <div className="relative">
                                    <IconLock className="pointer-events-none absolute left-3 top-1/2 h-4.5 w-4.5 -translate-y-1/2 text-neutral-400" />
                                    <input
                                        type={showPassword ? "text" : "password"}
                                        value={password}
                                        onChange={(e) => setPassword(e.target.value)}
                                        required
                                        autoComplete="current-password"
                                        className="w-full rounded-xl border border-neutral-300 py-2.5 pl-10 pr-10 text-sm outline-none transition-colors focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                                    />
                                    <button
                                        type="button"
                                        onClick={() => setShowPassword((v) => !v)}
                                        className="absolute right-3 top-1/2 -translate-y-1/2 text-neutral-400 hover:text-neutral-600"
                                        aria-label={showPassword ? "Ocultar senha" : "Mostrar senha"}
                                    >
                                        {showPassword ? <IconEyeOff className="h-4.5 w-4.5" /> : <IconEye className="h-4.5 w-4.5" />}
                                    </button>
                                </div>
                            </div>

                            <label className="flex cursor-pointer items-center gap-2 text-sm text-neutral-600">
                                <input
                                    type="checkbox"
                                    checked={remember}
                                    onChange={(e) => setRemember(e.target.checked)}
                                    className="h-4 w-4 rounded border-neutral-300 accent-orange-500"
                                />
                                Lembrar de mim neste dispositivo
                            </label>

                            {error && (
                                <div className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">{error}</div>
                            )}

                            <button
                                type="submit"
                                disabled={loading}
                                className="flex w-full items-center justify-center gap-2 rounded-xl bg-orange-500 py-2.5 text-sm font-semibold text-black transition-all hover:bg-orange-600 hover:shadow-lg hover:shadow-orange-200 disabled:opacity-60"
                            >
                                {loading && <IconSpinner className="h-4 w-4 animate-spin" />}
                                {loading ? "Entrando..." : "Entrar"}
                            </button>
                        </form>
                    </div>
                </div>
            </div>

            {/* Modal - esqueci minha senha */}
            {showForgot && (
                <div
                    className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4"
                    onClick={() => setShowForgot(false)}
                >
                    <div
                        className="w-full max-w-sm rounded-2xl bg-white p-6 shadow-2xl"
                        onClick={(e) => e.stopPropagation()}
                    >
                        {forgotStatus === "sent" ? (
                            <div className="text-center">
                                <div className="mx-auto mb-3 flex h-11 w-11 items-center justify-center rounded-full bg-orange-50 text-orange-500">
                                    <IconCheckDot className="h-7 w-7" />
                                </div>
                                <h2 className="text-lg font-semibold text-black">Solicitação registrada</h2>
                                <p className="mt-2 text-sm text-neutral-500">
                                    O administrador do sistema vai entrar em contato com você para redefinir sua senha.
                                </p>
                                <button
                                    type="button"
                                    onClick={() => setShowForgot(false)}
                                    className="mt-5 w-full rounded-xl bg-black py-2.5 text-sm font-semibold text-white hover:bg-neutral-800"
                                >
                                    Entendi
                                </button>
                            </div>
                        ) : (
                            <>
                                <h2 className="text-lg font-semibold text-black">Esqueci minha senha</h2>
                                <p className="mt-1 text-sm text-neutral-500">
                                    Informe seu email. Sua solicitação vai ficar registrada para o administrador
                                    entrar em contato e redefinir sua senha.
                                </p>
                                <form onSubmit={handleForgotSubmit} className="mt-4 space-y-3">
                                    <div className="relative">
                                        <IconMail className="pointer-events-none absolute left-3 top-1/2 h-4.5 w-4.5 -translate-y-1/2 text-neutral-400" />
                                        <input
                                            type="email"
                                            value={forgotEmail}
                                            onChange={(e) => setForgotEmail(e.target.value)}
                                            required
                                            className="w-full rounded-xl border border-neutral-300 py-2.5 pl-10 pr-3 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                                        />
                                    </div>

                                    {forgotError && (
                                        <div className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">{forgotError}</div>
                                    )}

                                    <div className="flex gap-2 pt-1">
                                        <button
                                            type="button"
                                            onClick={() => setShowForgot(false)}
                                            className="flex-1 rounded-xl border border-neutral-300 py-2.5 text-sm font-medium text-neutral-700 hover:bg-neutral-50"
                                        >
                                            Cancelar
                                        </button>
                                        <button
                                            type="submit"
                                            disabled={forgotStatus === "loading"}
                                            className="flex flex-1 items-center justify-center gap-2 rounded-xl bg-orange-500 py-2.5 text-sm font-semibold text-black hover:bg-orange-600 disabled:opacity-60"
                                        >
                                            {forgotStatus === "loading" && <IconSpinner className="h-4 w-4 animate-spin" />}
                                            Enviar
                                        </button>
                                    </div>
                                </form>
                            </>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}
