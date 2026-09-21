// Conjunto de ícones simples (stroke-based, estilo Feather/Lucide) usados no
// login e no menu lateral. Mantidos aqui pra não duplicar SVG em vários arquivos.

const base = {
    viewBox: "0 0 24 24",
    fill: "none",
    stroke: "currentColor",
    strokeWidth: 1.75,
    strokeLinecap: "round",
    strokeLinejoin: "round",
};

export function IconMail({ className }) {
    return (
        <svg {...base} className={className}>
            <rect x="3" y="5" width="18" height="14" rx="2.5" />
            <path d="m3.5 6.5 8.5 6 8.5-6" />
        </svg>
    );
}

export function IconLock({ className }) {
    return (
        <svg {...base} className={className}>
            <rect x="4.5" y="10.5" width="15" height="10" rx="2.2" />
            <path d="M8 10.5V7.5a4 4 0 0 1 8 0v3" />
        </svg>
    );
}

export function IconEye({ className }) {
    return (
        <svg {...base} className={className}>
            <path d="M2 12s3.6-6.5 10-6.5S22 12 22 12s-3.6 6.5-10 6.5S2 12 2 12Z" />
            <circle cx="12" cy="12" r="2.6" />
        </svg>
    );
}

export function IconEyeOff({ className }) {
    return (
        <svg {...base} className={className}>
            <path d="M3 3l18 18" />
            <path d="M10.6 5.7A10.6 10.6 0 0 1 12 5.5c6.4 0 10 6.5 10 6.5a15.6 15.6 0 0 1-3.4 4.2M6.7 6.8C4 8.6 2 12 2 12s3.6 6.5 10 6.5a9.9 9.9 0 0 0 3.4-.6" />
            <path d="M9.5 10a2.6 2.6 0 0 0 3.6 3.6" />
        </svg>
    );
}

export function IconSpinner({ className }) {
    return (
        <svg viewBox="0 0 24 24" fill="none" className={className}>
            <circle cx="12" cy="12" r="9" stroke="currentColor" strokeWidth="2.5" opacity=".25" />
            <path d="M21 12a9 9 0 0 0-9-9" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" />
        </svg>
    );
}

export function IconCheckDot({ className }) {
    return (
        <svg viewBox="0 0 24 24" fill="none" className={className}>
            <circle cx="12" cy="12" r="10" fill="currentColor" opacity=".18" />
            <path d="m8 12.5 2.6 2.6L16.5 9" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
    );
}

export function IconUsers({ className }) {
    return (
        <svg {...base} className={className}>
            <circle cx="9" cy="8" r="3.2" />
            <path d="M2.8 20v-1.2A4.5 4.5 0 0 1 7.3 14.3h3.4a4.5 4.5 0 0 1 4.5 4.5V20" />
            <path d="M16 8.2a3.2 3.2 0 0 1 0 6.2" opacity=".6" />
            <path d="M18.5 14.5a4.3 4.3 0 0 1 2.7 4v1.5" opacity=".6" />
        </svg>
    );
}

export function IconBriefcase({ className }) {
    return (
        <svg {...base} className={className}>
            <rect x="3" y="7.5" width="18" height="12" rx="2.2" />
            <path d="M8.5 7.5V6a2.2 2.2 0 0 1 2.2-2.2h2.6A2.2 2.2 0 0 1 15.5 6v1.5" />
            <path d="M3 12.5h18" />
        </svg>
    );
}

export function IconWallet({ className }) {
    return (
        <svg {...base} className={className}>
            <path d="M3.5 7.5A2.5 2.5 0 0 1 6 5h11.5a1 1 0 0 1 1 1v2" />
            <rect x="3.5" y="7.5" width="17" height="12" rx="2.3" />
            <path d="M16 13.2h2.6" />
        </svg>
    );
}

export function IconCreditCard({ className }) {
    return (
        <svg {...base} className={className}>
            <rect x="2.5" y="5.5" width="19" height="13" rx="2.2" />
            <path d="M2.5 9.8h19" />
            <path d="M6 14.3h4" />
        </svg>
    );
}

export function IconReceipt({ className }) {
    return (
        <svg {...base} className={className}>
            <path d="M6 3h12v18l-2.5-1.6L13 21l-2.5-1.6L8 21l-2-1.6Z" />
            <path d="M9 8h6M9 12h6" />
        </svg>
    );
}

export function IconChartBar({ className }) {
    return (
        <svg {...base} className={className}>
            <path d="M4 20V10M11 20V4M18 20v-7" />
            <path d="M2.5 20h19" />
        </svg>
    );
}

export function IconChartPie({ className }) {
    return (
        <svg {...base} className={className}>
            <path d="M12 3.5A8.5 8.5 0 1 0 20.5 12H12Z" />
            <path d="M15.5 3.9A8.5 8.5 0 0 1 20.1 8.5H12Z" opacity=".6" />
        </svg>
    );
}

export function IconShirt({ className }) {
    return (
        <svg {...base} className={className}>
            <path d="M8 4 4 7l2 3 2-1.3V20h8V8.7L18 10l2-3-4-3-1.3 1.6a3 3 0 0 1-5.4 0Z" />
        </svg>
    );
}

export function IconBox({ className }) {
    return (
        <svg {...base} className={className}>
            <path d="M3.5 7.5 12 3l8.5 4.5-8.5 4.5-8.5-4.5Z" />
            <path d="M3.5 7.5v9L12 21l8.5-4.5v-9" />
            <path d="M12 12v9" />
        </svg>
    );
}

export function IconStore({ className }) {
    return (
        <svg {...base} className={className}>
            <path d="M3.5 9.5 4.5 4.5h15l1 5" />
            <path d="M3.5 9.5a2.5 2.5 0 0 0 5 0 2.5 2.5 0 0 0 5 0 2.5 2.5 0 0 0 5 0 2.5 2.5 0 0 0 2-2.5" opacity=".7" />
            <path d="M5 9.8V19.5h14V9.8" />
            <path d="M9.5 19.5v-5.5a1 1 0 0 1 1-1h3a1 1 0 0 1 1 1v5.5" />
        </svg>
    );
}

export function IconFileSpreadsheet({ className }) {
    return (
        <svg {...base} className={className}>
            <path d="M6 3h9l4 4v13a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1Z" />
            <path d="M14.5 3.2V8h4.8" opacity=".6" />
            <path d="M8 12.5h8M8 16h8M8 12.5v6.5M12 12.5v6.5M16 12.5v6.5" opacity=".7" />
        </svg>
    );
}

export function IconKey({ className }) {
    return (
        <svg {...base} className={className}>
            <circle cx="7.5" cy="14.5" r="3.5" />
            <path d="m10.5 12 8-8" />
            <path d="M16 6l2 2" />
            <path d="M18.5 3.5l2 2" />
        </svg>
    );
}

export function IconClipboardList({ className }) {
    return (
        <svg {...base} className={className}>
            <rect x="5" y="4.5" width="14" height="16" rx="2" />
            <path d="M9 4.2h6a1 1 0 0 1 1 1V6H8v-.8a1 1 0 0 1 1-1Z" />
            <path d="M8.5 11h7M8.5 14.5h7M8.5 18h4.5" />
        </svg>
    );
}

export function IconLogOut({ className }) {
    return (
        <svg {...base} className={className}>
            <path d="M9 20H5.5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2H9" />
            <path d="M16 16.5 21 12l-5-4.5" />
            <path d="M21 12H9" />
        </svg>
    );
}
