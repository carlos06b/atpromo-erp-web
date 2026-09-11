import { createContext, useContext, useState } from "react";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
    const [token, setToken] = useState(() => localStorage.getItem("token"));
    const [userName, setUserName] = useState(() => localStorage.getItem("userName") || "");
    const [jobTittle, setJobTittle] = useState(() => localStorage.getItem("userJobTittle") || "");

    function login(newToken, name, newJobTittle) {
        localStorage.setItem("token", newToken);
        setToken(newToken);

        if (name) {
            localStorage.setItem("userName", name);
            setUserName(name);
        }

        if (newJobTittle) {
            localStorage.setItem("userJobTittle", newJobTittle);
            setJobTittle(newJobTittle);
        }
    }

    function logout() {
        localStorage.removeItem("token");
        localStorage.removeItem("userName");
        localStorage.removeItem("userJobTittle");
        setToken(null);
        setUserName("");
        setJobTittle("");
    }

    return (
        <AuthContext.Provider value={{ token, userName, jobTittle, login, logout }}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    return useContext(AuthContext);
}