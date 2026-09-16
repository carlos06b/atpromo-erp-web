import { Routes, Route, Navigate } from "react-router-dom";
import Login from "./pages/Login";
import Promotores from "./pages/Promotores";
import Clientes from "./pages/Clientes";
import Faturamento from "./pages/Faturamento";
import FolhaDePagamento from "./pages/FolhaDePagamento";
import Despesas from "./pages/Despesas";
import Relatorios from "./pages/Relatorios";
import Solicitacoes from "./pages/Solicitacoes";
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
                path="/clientes"
                element={
                    <ProtectedRoute page="clientes">
                        <Clientes />
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
                path="/solicitacoes"
                element={
                    <ProtectedRoute page="solicitacoes">
                        <Solicitacoes />
                    </ProtectedRoute>
                }
            />
            <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
    );
}

export default App;
