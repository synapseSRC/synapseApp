export const APP_NAME = process.env.NEXT_PUBLIC_APP_NAME || "Synapse";

export const APP_DOMAIN = process.env.NEXT_PUBLIC_APP_DOMAIN
    ? `https://${process.env.NEXT_PUBLIC_APP_DOMAIN}`
    : "https://synapse.app";

export const SUPPORT_EMAIL = "support@synapse.app";

// Placeholder repository links — replace with the real URLs before launch.
export const PROJECT_LINKS = {
    github: "#",
    issues: "#",
    discussions: "#",
    releases: "#",
} as const;
