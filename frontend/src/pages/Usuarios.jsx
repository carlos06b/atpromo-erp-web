import { useCallback, useEffect, useMemo, useState } from "react";
import Layout from "../components/Layout";
import ConfirmDeleteDialog from "../components/ConfirmDeleteDialog";
import { apiFetch } from "../api";
import { useAuth } from "../context/AuthContext";
import { IconLock, IconUsers } from "../components/icons";

const ROLE_OPTIONS = [
    { value: "RH", label: "RH" },
    { value: "FINANCEIRO", label: "Financeiro" },
    { value: "SUPERVISOR", label: "Supervisor" },
    { value: "ADMIN", label: "Admin" },
];

const EMPTY_FORM = { name: "", email: "", password: "", confirmPassword: "", jobTittle: "RH" };

// Decodifica só a parte do meio do JWT (payload) pra saber o email de quem está
// logado, sem precisar guardar isso à parte. É só pra esconder o botão de excluir
// na própria linha do usuário logado - a proteção de verdade é sempre no backend.
function decodeJwtEmail(token) {
    try {
        const payload = token.split(".")[1];
        const normalized = payload.replace(/-/g, "+").replace(/_/g, "/");
        const decoded = JSON.parse(atob(normalized));
        return decoded.sub || null;
    } catch {
        return null;
    }
}

function roleBadgeClasses(role) {
    switch (role) {
        case "RH":
            return "bg-blue-50 text-blue-700";
        case "FINANCEIRO":
            return "bg-emerald-50 text-emerald-700";
        case "SUPERVISOR":
            return "bg-purple-50 text-purple-700";
        default:
            return "bg-orange-50 text-orange-700";
    }
}

export default function Usuarios() {
    const { token } = useAuth();
    const currentEmail = useMemo(() => decodeJwtEmail(token), [token]);

    const [users, setUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    const [showModal, setShowModal] = useState(false);
    const [editingUser, setEditingUser] = useState(null);
    const [form, setForm] = useState(EMPTY_FORM);
    const [formError, setFormError] = useState("");
    const [saving, setSaving] = useState(false);

    const [userToDelete, setUserToDelete] = useState(null);
    const [deleteError, setDeleteError] = useState("");

    const loadUsers = useCallback(async () => {
        setLoading(true);
        setError("");
        try {
            const data = await apiFetch("/users", { token });
            setUsers(data || []);
        } catch (err) {
            setError(err.message || "Não foi possível carregar os usuários.");
        } finally {
            setLoading(false);
        }
    }, [token]);

    useEffect(() => {
        loadUsers();
    }, [loadUsers]);

    function openCreate() {
        setEditingUser(null);
        setForm(EMPTY_FORM);
        setFormError("");
        setShowModal(true);
    }

    function openEdit(user) {
        setEditingUser(user);
        setForm({
            name: user.name || "",
            email: user.email || "",
            password: "",
            confirmPassword: "",
            jobTittle: user.jobTittle || "RH",
        });
        setFormError("");
        setShowModal(true);
    }

    function closeModal() {
        setShowModal(false);
        setEditingUser(null);
        setForm(EMPTY_FORM);
        setFormError("");
    }

    async function handleSubmit(event) {
        event.preventDefault();
        setFormError("");

        const isCreating = !editingUser;
        const trimmedName = form.name.trim();
        const trimmedEmail = form.email.trim();

        if (!trimmedName || !trimmedEmail) {
            setFormError("Preencha nome e email.");
            return;
        }

        if ((isCreating || form.password) && form.password.length < 8) {
            setFormError("A senha deve ter pelo menos 8 caracteres.");
            return;
        }

        if (form.password !== form.confirmPassword) {
            setFormError("As senhas não conferem.");
            return;
        }

        setSaving(true);
        try {
            const payload = {
                name: trimmedName,
                email: trimmedEmail,
                password: form.password,
                jobTittle: form.jobTittle,
            };

            if (isCreating) {
                await apiFetch("/users", { method: "POST", body: payload, token });
            } else {
                await apiFetch(`/users/${editingUser.id}`, { method: "PUT", body: payload, token });
            }

            closeModal();
            loadUsers();
        } catch (err) {
            setFormError(err.message || "Não foi possível salvar o usuário.");
        } finally {
            setSaving(false);
        }
    }

    async function handleDeleteConfirmed() {
        if (!userToDelete) return;
        setDeleteError("");
        try {
            await apiFetch(`/users/${userToDelete.id}`, { method: "DELETE", token });
            setUserToDelete(null);
            loadUsers();
        } catch (err) {
            setDeleteError(err.message || "Não foi possível excluir o usuário.");
            setUserToDelete(null);
        }
    }

    return (
        <Layout title="Usuários">
            <div className="mb-6 flex items-start justify-between gap-4">
                <div className="flex items-start gap-3 rounded-xl border border-neutral-200 bg-white p-4">
                    <div className="flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-full bg-orange-50 text-orange-500">
                        <IconLock className="h-5 w-5" />
                    </div>
                    <p className="text-sm text-neutral-600">
                        Só quem é Admin acessa essa tela. Aqui você cria, edita o perfil e apaga usuários do
                        sistema. As senhas nunca aparecem depois de cadastradas — só é possível definir uma nova.
                    </p>
                </div>

                <button
                    onClick={openCreate}
                    className="flex-shrink-0 rounded-xl bg-orange-500 px-4 py-2.5 text-sm font-semibold text-black transition-colors hover:bg-orange-600"
                >
                    + Novo usuário
                </button>
            </div>

            {error && (
                <div className="mb-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">{error}</div>
            )}
            {deleteError && (
                <div className="mb-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">{deleteError}</div>
            )}

            <div className="overflow-hidden rounded-xl border border-neutral-200 bg-white">
                {loading ? (
                    <div className="p-10 text-center text-sm text-neutral-500">Carregando...</div>
                ) : users.length === 0 ? (
                    <div className="flex flex-col items-center gap-2 p-10 text-center text-sm text-neutral-500">
                        <IconUsers className="h-6 w-6 text-neutral-300" />
                        Nenhum usuário cadastrado ainda.
                    </div>
                ) : (
                    <table className="w-full text-left text-sm">
                        <thead className="border-b border-neutral-200 bg-neutral-50 text-xs uppercase tracking-wide text-neutral-500">
                            <tr>
                                <th className="px-5 py-3 font-medium">Nome</th>
                                <th className="px-5 py-3 font-medium">Email</th>
                                <th className="px-5 py-3 font-medium">Perfil</th>
                                <th className="px-5 py-3 font-medium text-right">Ações</th>
                            </tr>
                        </thead>
                        <tbody>
                            {users.map((user) => {
                                const isSelf = currentEmail && user.email === currentEmail;
                                return (
                                    <tr key={user.id} className="border-b border-neutral-100 last:border-0">
                                        <td className="px-5 py-3 text-neutral-800">
                                            {user.name}
                                            {isSelf && (
                                                <span className="ml-2 rounded-full bg-neutral-100 px-2 py-0.5 text-xs text-neutral-500">
                                                    você
                                                </span>
                                            )}
                                        </td>
                                        <td className="px-5 py-3 text-neutral-500">{user.email}</td>
                                        <td className="px-5 py-3">
                                            <span className={`rounded-full px-2.5 py-1 text-xs font-medium ${roleBadgeClasses(user.jobTittle)}`}>
                                                {user.jobTittle}
                                            </span>
                                        </td>
                                        <td className="px-5 py-3 text-right">
                                            <div className="flex justify-end gap-2">
                                                <button
                                                    onClick={() => openEdit(user)}
                                                    className="rounded-lg border border-neutral-300 px-3 py-1.5 text-xs font-medium text-neutral-700 hover:bg-neutral-50"
                                                >
                                                    Editar
                                                </button>
                                                <button
                                                    onClick={() => setUserToDelete(user)}
                                                    disabled={isSelf}
                                                    title={isSelf ? "Você não pode excluir o próprio usuário" : undefined}
                                                    className="rounded-lg border border-red-200 px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-40"
                                                >
                                                    Excluir
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>
                )}
            </div>

            {showModal && (
                <div
                    className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4"
                    onClick={closeModal}
                >
                    <div
                        className="w-full max-w-md rounded-2xl bg-white p-6 shadow-2xl"
                        onClick={(e) => e.stopPropagation()}
                    >
                        <h2 className="mb-4 text-lg font-semibold text-black">
                            {editingUser ? "Editar usuário" : "Novo usuário"}
                        </h2>

                        <form onSubmit={handleSubmit} className="space-y-4">
                            <div>
                                <label className="mb-1 block text-sm font-medium text-neutral-700">Nome</label>
                                <input
                                    type="text"
                                    value={form.name}
                                    onChange={(e) => setForm({ ...form, name: e.target.value })}
                                    required
                                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                                />
                            </div>

                            <div>
                                <label className="mb-1 block text-sm font-medium text-neutral-700">Email</label>
                                <input
                                    type="email"
                                    value={form.email}
                                    onChange={(e) => setForm({ ...form, email: e.target.value })}
                                    required
                                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                                />
                            </div>

                            <div>
                                <label className="mb-1 block text-sm font-medium text-neutral-700">Perfil</label>
                                <select
                                    value={form.jobTittle}
                                    onChange={(e) => setForm({ ...form, jobTittle: e.target.value })}
                                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                                >
                                    {ROLE_OPTIONS.map((role) => (
                                        <option key={role.value} value={role.value}>
                                            {role.label}
                                        </option>
                                    ))}
                                </select>
                            </div>

                            <div>
                                <label className="mb-1 block text-sm font-medium text-neutral-700">
                                    {editingUser ? "Nova senha (deixe em branco pra manter a atual)" : "Senha"}
                                </label>
                                <input
                                    type="password"
                                    value={form.password}
                                    onChange={(e) => setForm({ ...form, password: e.target.value })}
                                    required={!editingUser}
                                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                                />
                            </div>

                            <div>
                                <label className="mb-1 block text-sm font-medium text-neutral-700">Confirmar senha</label>
                                <input
                                    type="password"
                                    value={form.confirmPassword}
                                    onChange={(e) => setForm({ ...form, confirmPassword: e.target.value })}
                                    required={!editingUser || form.password.length > 0}
                                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                                />
                            </div>

                            {formError && (
                                <div className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">{formError}</div>
                            )}

                            <div className="flex gap-2 pt-1">
                                <button
                                    type="button"
                                    onClick={closeModal}
                                    className="flex-1 rounded-xl border border-neutral-300 py-2.5 text-sm font-medium text-neutral-700 hover:bg-neutral-50"
                                >
                                    Cancelar
                                </button>
                                <button
                                    type="submit"
                                    disabled={saving}
                                    className="flex-1 rounded-xl bg-orange-500 py-2.5 text-sm font-semibold text-black hover:bg-orange-600 disabled:opacity-60"
                                >
                                    {saving ? "Salvando..." : "Salvar"}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            <ConfirmDeleteDialog
                open={!!userToDelete}
                onClose={() => setUserToDelete(null)}
                onConfirmed={handleDeleteConfirmed}
                itemLabel={userToDelete ? `o usuário "${userToDelete.name}"` : ""}
            />
        </Layout>
    );
}
