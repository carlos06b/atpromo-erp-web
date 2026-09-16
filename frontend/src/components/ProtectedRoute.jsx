import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { canAccess, defaultPageFor } from "../access";

export default function ProtectedRoute({ children, page }) {
    const { token, jobTittle } = useAuth();

    if (!token) {
        return <Navigate to="/login" replace />;
    }

    if (page && !canAccess(jobTittle, page)) {
        return <Navigate to={defaultPageFor(jobTittle)} replace />;
    }

    return children;
}
