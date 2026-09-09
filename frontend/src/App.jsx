import { Routes, Route, Navigate } from "react-router-dom";
import Login from "./pages/Login";
import Promotores from "./pages/Promotores";
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
            <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
    );
}

export default App;