export type LegalDocId = "terms" | "privacy" | "cookies";

export const LEGAL_DOCS: { id: LegalDocId; label: string; href: string }[] = [
    { id: "terms", label: "Terms of Service", href: "/terms" },
    { id: "privacy", label: "Privacy Policy", href: "/privacy" },
    { id: "cookies", label: "Cookie Policy", href: "/cookies" },
];
