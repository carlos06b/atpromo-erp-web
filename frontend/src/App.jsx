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
                    <ProtectedRoute>
                        <Promotores />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/clientes"
                element={
                    <ProtectedRoute>
                        <Clientes />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/faturamento"
                element={
                    <ProtectedRoute>
                        <Faturamento />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/folha-pagamento"
                element={
                    <ProtectedRoute>
                        <FolhaDePagamento />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/despesas"
                element={
                    <ProtectedRoute>
                        <Despesas />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/relatorios"
                element={
                    <ProtectedRoute>
                        <Relatorios />
                    </ProtectedRoute>
                }
            />
            <Route
                path="/solicitacoes"
                element={
                    <ProtectedRoute>
                        <Solicitacoes />
                    </ProtectedRoute>
                }
            />
            <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
    );
}

export default App;