import { createContext, useContext, useState } from "react";

const AuthContext = createContext(null);

function readStored(key) {
    return localStorage.getItem(key) || sessionStorage.getItem(key) || "";
}

export function AuthProvider({ children }) {
    const [token, setToken] = useState(() => readStored("token") || null);
    const [userName, setUserName] = useState(() => readStored("userName"));
    const [jobTittle, setJobTittle] = useState(() => readStored("userJobTittle"));

    // remember = true -> guarda em localStorage (sobrevive a fechar o navegador)
    // remember = false -> guarda em sessionStorage (some ao fechar a aba/navegador)
    function login(newToken, name, newJobTittle, remember = true) {
        const storage = remember ? localStorage : sessionStorage;
        const other = remember ? sessionStorage : localStorage;

        storage.setItem("token", newToken);
        other.removeItem("token");
        setToken(newToken);

        if (name) {
            storage.setItem("userName", name);
            other.removeItem("userName");
            setUserName(name);
        }

        if (newJobTittle) {
            storage.setItem("userJobTittle", newJobTittle);
            other.removeItem("userJobTittle");
            setJobTittle(newJobTittle);
        }
    }

    function logout() {
        for (const store of [localStorage, sessionStorage]) {
            store.removeItem("token");
            store.removeItem("userName");
            store.removeItem("userJobTittle");
        }
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
