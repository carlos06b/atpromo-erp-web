import { useState } from "react";
import { apiFetch } from "../api";
import { useAuth } from "../context/AuthContext";

export default function ConfirmDeleteDialog({ open, onClose, onConfirmed, itemLabel }) {
    const { token } = useAuth();
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const [checking, setChecking] = useState(false);

    if (!open) return null;

    async function handleConfirm(event) {
        event.preventDefault();
        setError("");
        setChecking(true);

        try {
            await apiFetch("/account/verify-password", {
                method: "POST",
                body: { password },
                token,
            });
            setPassword("");
            onConfirmed();
        } catch (err) {
            setError("Senha incorreta. Tente novamente.");
        } finally {
            setChecking(false);
        }
    }

    function handleClose() {
        setPassword("");
        setError("");
        onClose();
    }

    return (
        <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black/60 p-4">
            <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-2xl">
                <h2 className="mb-2 text-lg font-semibold text-black">Confirmar exclusão</h2>

                <div className="mb-4 rounded-lg border border-orange-200 bg-orange-50 px-3 py-2 text-sm text-orange-800">
                    Atenção: essa ação apaga <strong>{itemLabel}</strong> permanentemente do banco de dados. Use
                    apenas se foi um cadastro feito por engano — registros já usados em relatórios, faturamentos
                    ou folha de pagamento podem ficar incompletos ou incorretos depois disso.
                </div>

                <form onSubmit={handleConfirm} className="space-y-4">
                    <div>
                        <label className="mb-1 block text-sm font-medium text-neutral-700">
                            Digite sua senha para confirmar
                        </label>
                        <input
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                            autoFocus
                            className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                        />
                    </div>

                    {error && (
                        <div className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">{error}</div>
                    )}

                    <div className="flex justify-end gap-3 pt-2">
                        <button
                            type="button"
                            onClick={handleClose}
                            className="rounded-lg px-4 py-2 text-sm font-medium text-neutral-600 hover:bg-neutral-100"
                        >
                            Cancelar
                        </button>
                        <button
                            type="submit"
                            disabled={checking}
                            className="rounded-lg bg-red-600 px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-red-700 disabled:opacity-60"
                        >
                            {checking ? "Verificando..." : "Excluir definitivamente"}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}