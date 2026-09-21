import { Routes, Route, Navigate } from "react-router-dom";
import Login from "./pages/Login";
import Promotores from "./pages/Promotores";
import Lojas from "./pages/Lojas";
import Clientes from "./pages/Clientes";
import Descritivos from "./pages/Descritivos";
import Faturamento from "./pages/Faturamento";
import FolhaDePagamento from "./pages/FolhaDePagamento";
import Despesas from "./pages/Despesas";
import Relatorios from "./pages/Relatorios";
import IndicadoresRH from "./pages/IndicadoresRH";
import Uniformes from "./pages/Uniformes";
import Estoque from "./pages/Estoque";
import Solicitacoes from "./pages/Solicitacoes";
import RedefinicoesSenha from "./pages/RedefinicoesSenha";
import Usuarios from "./pages/Usuarios";
import ProtectedRoute from "./components/ProtectedRoute";

function App() {
    return (
        <Routes>
            <Route path="/login" element={<Login />} />
            <Route
                path="/promotores"
                element={
                    <ProtectedRoute page="promotores">
                        <Promotores />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/lojas"
                element={
                    <ProtectedRoute page="lojas">
                        <Lojas />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/clientes"
                element={
                    <ProtectedRoute page="clientes">
                        <Clientes />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/descritivos"
                element={
                    <ProtectedRoute page="descritivos">
                        <Descritivos />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/faturamento"
                element={
                    <ProtectedRoute page="faturamento">
                        <Faturamento />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/folha-pagamento"
                element={
                    <ProtectedRoute page="folha-pagamento">
                        <FolhaDePagamento />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/despesas"
                element={
                    <ProtectedRoute page="despesas">
                        <Despesas />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/relatorios"
                element={
                    <ProtectedRoute page="relatorios">
                        <Relatorios />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/indicadores-rh"
                element={
                    <ProtectedRoute page="indicadores-rh">
                        <IndicadoresRH />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/uniformes"
                element={
                    <ProtectedRoute page="uniformes">
                        <Uniformes />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/estoque"
                element={
                    <ProtectedRoute page="estoque">
                        <Estoque />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/solicitacoes"
                element={
                    <ProtectedRoute page="solicitacoes">
                        <Solicitacoes />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/redefinicoes-senha"
                element={
                    <ProtectedRoute page="redefinicoes-senha">
                        <RedefinicoesSenha />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/usuarios"
                element={
                    <ProtectedRoute page="usuarios">
                        <Usuarios />
                    </ProtectedRoute>
                }
            />
            <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
    );
}

export default App;
