import { useMemo, useState } from "react";
import { Calendar, dateFnsLocalizer } from "react-big-calendar";
import { format, parse, startOfWeek, getDay } from "date-fns";
import { ptBR } from "date-fns/locale";
import "react-big-calendar/lib/css/react-big-calendar.css";
import "./FaturamentoCalendar.css";

const locales = { "pt-BR": ptBR };

const localizer = dateFnsLocalizer({
    format,
    parse,
    startOfWeek,
    getDay,
    locales,
});

const KIND_LABELS = {
    vencimento: "Vencimento",
    emissao: "Emitido",
    pagamento: "Pago",
};

const STATUS_LABELS = {
    PENDENTE: "Pendente",
    FATURADO: "Faturado",
    PAGO: "Pago",
    CANCELADO: "Cancelado",
};

const STATUS_COLORS = {
    PENDENTE: "#f97316",
    FATURADO: "#2563eb",
    PAGO: "#16a34a",
    CANCELADO: "#dc2626",
};

const VIEW_LABELS = {
    month: "Mês",
    week: "Semana",
    day: "Dia",
};

const CALENDAR_MESSAGES = {
    date: "Data",
    time: "Hora",
    event: "Faturamento",
    allDay: "Dia inteiro",
    noEventsInRange: "Nenhum faturamento nesse período.",
    showMore: (total) => `+${total} mais`,
};

function parseIsoDate(value) {
    if (!value) return null;
    const [year, month, day] = value.split("-").map(Number);
    return new Date(year, month - 1, day);
}

function buildEvents(invoices, clientsById, activeKinds) {
    const fieldByKind = {
        vencimento: "dueDate",
        emissao: "issueDate",
        pagamento: "paymentDate",
    };

    const events = [];

    invoices.forEach((invoice) => {
        const client = clientsById[invoice.clientId];
        const clientName = client ? client.name : `Cliente #${invoice.clientId}`;
        const valor =
            invoice.amount != null
                ? Number(invoice.amount).toLocaleString("pt-BR", { style: "currency", currency: "BRL" })
                : "";

        activeKinds.forEach((kind) => {
            const date = parseIsoDate(invoice[fieldByKind[kind]]);
            if (!date) return;

            events.push({
                id: `${invoice.id}-${kind}`,
                title: `${KIND_LABELS[kind]} · ${clientName}${valor ? " · " + valor : ""}`,
                start: date,
                end: date,
                allDay: true,
                status: invoice.status,
                invoice,
            });
        });
    });

    return events;
}

function CalendarToolbar({ label, views, view, onNavigate, onView }) {
    return (
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3 border-b border-neutral-200 pb-4">
            <div className="flex items-center gap-2">
                <button
                    type="button"
                    onClick={() => onNavigate("TODAY")}
                    className="rounded-lg border border-neutral-300 px-3 py-1.5 text-sm font-medium text-neutral-700 transition-colors hover:border-orange-500 hover:text-orange-600"
                >
                    Hoje
                </button>

                <div className="flex items-center gap-1">
                    <button
                        type="button"
                        onClick={() => onNavigate("PREV")}
                        aria-label="Período anterior"
                        className="flex h-8 w-8 items-center justify-center rounded-lg border border-neutral-300 text-neutral-700 transition-colors hover:border-orange-500 hover:text-orange-600"
                    >
                        ‹
                    </button>
                    <button
                        type="button"
                        onClick={() => onNavigate("NEXT")}
                        aria-label="Próximo período"
                        className="flex h-8 w-8 items-center justify-center rounded-lg border border-neutral-300 text-neutral-700 transition-colors hover:border-orange-500 hover:text-orange-600"
                    >
                        ›
                    </button>
                </div>

                <span className="ml-1 text-base font-semibold capitalize text-black">{label}</span>
            </div>

            <div className="flex rounded-lg border border-neutral-300 bg-white p-1">
                {views.map((v) => (
                    <button
                        key={v}
                        type="button"
                        onClick={() => onView(v)}
                        className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
                            view === v ? "bg-orange-500 text-black" : "text-neutral-600 hover:text-orange-600"
                        }`}
                    >
                        {VIEW_LABELS[v] || v}
                    </button>
                ))}
            </div>
        </div>
    );
}

export default function FaturamentoCalendar({ invoices, clientsById, onSelectInvoice }) {
    const [activeKinds, setActiveKinds] = useState(["vencimento", "emissao", "pagamento"]);
    const [date, setDate] = useState(new Date());
    const [view, setView] = useState("month");

    const events = useMemo(
        () => buildEvents(invoices, clientsById, activeKinds),
        [invoices, clientsById, activeKinds]
    );

    function toggleKind(kind) {
        setActiveKinds((prev) =>
            prev.includes(kind) ? prev.filter((k) => k !== kind) : [...prev, kind]
        );
    }

    return (
        <div className="rounded-xl border border-neutral-200 bg-white p-4 shadow-sm">
            <div className="mb-4 flex flex-wrap items-center gap-x-6 gap-y-3">
                <div className="flex flex-wrap items-center gap-2">
                    <span className="mr-1 text-sm font-medium text-neutral-600">Mostrar:</span>
                    {Object.entries(KIND_LABELS).map(([kind, label]) => {
                        const active = activeKinds.includes(kind);
                        return (
                            <button
                                key={kind}
                                type="button"
                                onClick={() => toggleKind(kind)}
                                className={`rounded-full border px-3 py-1 text-xs font-medium transition-colors ${
                                    active
                                        ? "border-orange-500 bg-orange-500 text-black"
                                        : "border-neutral-300 bg-white text-neutral-500 hover:border-orange-300 hover:text-orange-600"
                                }`}
                            >
                                {label}
                            </button>
                        );
                    })}
                </div>

                <div className="ml-auto flex flex-wrap items-center gap-3 text-xs text-neutral-500">
                    {Object.entries(STATUS_LABELS).map(([status, label]) => (
                        <span key={status} className="flex items-center gap-1.5">
              <span
                  className="inline-block h-2.5 w-2.5 rounded-full"
                  style={{ backgroundColor: STATUS_COLORS[status] }}
              />
                            {label}
            </span>
                    ))}
                </div>
            </div>

            <div className="fat-calendar" style={{ height: 700 }}>
                <Calendar
                    localizer={localizer}
                    culture="pt-BR"
                    events={events}
                    startAccessor="start"
                    endAccessor="end"
                    views={["month", "week", "day"]}
                    date={date}
                    view={view}
                    onNavigate={(newDate) => setDate(newDate)}
                    onView={(newView) => setView(newView)}
                    messages={CALENDAR_MESSAGES}
                    popup
                    components={{ toolbar: CalendarToolbar }}
                    eventPropGetter={(event) => ({
                        style: {
                            backgroundColor: STATUS_COLORS[event.status] || "#a3a3a3",
                            borderColor: "transparent",
                            color: event.status === "PENDENTE" ? "#111111" : "#ffffff",
                            borderRadius: "6px",
                            fontSize: "0.75rem",
                            padding: "1px 6px",
                        },
                    })}
                    onSelectEvent={(event) => onSelectInvoice(event.invoice)}
                />
            </div>
        </div>
    );
}