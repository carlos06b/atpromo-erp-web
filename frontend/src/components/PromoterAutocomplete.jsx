import { useEffect, useRef, useState } from "react";

export default function PromoterAutocomplete({ promoters, value, onChange, placeholder }) {
    const [query, setQuery] = useState("");
    const [open, setOpen] = useState(false);
    const containerRef = useRef(null);

    const selected = promoters.find((p) => String(p.id) === String(value));

    useEffect(() => {
        setQuery(selected ? selected.name : "");
    }, [value, promoters]);

    useEffect(() => {
        function handleClickOutside(event) {
            if (containerRef.current && !containerRef.current.contains(event.target)) {
                setOpen(false);
                setQuery(selected ? selected.name : "");
            }
        }
        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    }, [selected]);

    const term = query.trim().toLowerCase();
    const filtered =
        term === "" ? promoters : promoters.filter((p) => (p.name || "").toLowerCase().includes(term));

    function handleSelect(promoter) {
        onChange(String(promoter.id));
        setQuery(promoter.name);
        setOpen(false);
    }

    function handleClear() {
        onChange("");
        setQuery("");
        setOpen(true);
    }

    return (
        <div ref={containerRef} className="relative">
            <div className="relative">
                <input
                    type="text"
                    value={query}
                    onChange={(e) => {
                        setQuery(e.target.value);
                        setOpen(true);
                    }}
                    onFocus={() => setOpen(true)}
                    placeholder={placeholder || "Buscar promotor pelo nome..."}
                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 pr-8 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-100"
                />
                {query && (
                    <button
                        type="button"
                        onClick={handleClear}
                        aria-label="Limpar"
                        className="absolute right-2 top-1/2 -translate-y-1/2 text-neutral-400 hover:text-neutral-600"
                    >
                        ✕
                    </button>
                )}
            </div>

            {open && (
                <div className="absolute z-10 mt-1 max-h-56 w-full overflow-auto rounded-lg border border-neutral-200 bg-white shadow-lg">
                    {filtered.length === 0 ? (
                        <div className="px-3 py-2 text-sm text-neutral-400">Nenhum promotor encontrado.</div>
                    ) : (
                        filtered.map((p) => (
                            <button
                                key={p.id}
                                type="button"
                                onClick={() => handleSelect(p)}
                                className={`block w-full px-3 py-2 text-left text-sm hover:bg-orange-50 ${
                                    String(p.id) === String(value) ? "bg-orange-100 font-medium text-orange-700" : "text-neutral-700"
                                }`}
                            >
                                {p.name}
                            </button>
                        ))
                    )}
                </div>
            )}
        </div>
    );
}