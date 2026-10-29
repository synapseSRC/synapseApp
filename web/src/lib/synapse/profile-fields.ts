import type { OwnProfileRow } from "./types";

export const PROFILE_LIMITS = {
    displayName: 50,
    bio: 300,
    website: 200,
    location: 80,
    pronouns: 40,
    work: 100,
    education: 100,
    relationship: 40,
} as const;

export interface ProfileFormValues {
    username: string;
    display_name: string;
    bio: string;
    website: string;
    location: string;
    pronouns: string;
    work: string;
    education: string;
    relationship_status: string;
    is_private: boolean;
    avatar: string | null;
}

export const toFormValues = (profile: OwnProfileRow): ProfileFormValues => ({
    username: profile.username ?? "",
    display_name: profile.display_name ?? "",
    bio: profile.bio ?? "",
    website: profile.website ?? "",
    location: profile.location ?? "",
    pronouns: profile.pronouns ?? "",
    work: profile.work ?? "",
    education: profile.education ?? "",
    relationship_status: profile.relationship_status ?? "",
    is_private: Boolean(profile.is_private),
    avatar: profile.avatar,
});

export const normalizeWebsite = (raw: string): { value: string | null; error?: string } => {
    const trimmed = raw.trim();

    if (!trimmed) return { value: null };
    if (trimmed.length > PROFILE_LIMITS.website) {
        return { value: null, error: `Website must be ${PROFILE_LIMITS.website} characters or fewer.` };
    }

    const candidate = /^[a-z][a-z0-9+.-]*:/i.test(trimmed) ? trimmed : `https://${trimmed}`;

    try {
        const url = new URL(candidate);

        if (url.protocol !== "http:" && url.protocol !== "https:") {
            return { value: null, error: "Website must start with http:// or https://." };
        }
        if (!url.hostname.includes(".")) {
            return { value: null, error: "Enter a valid website address." };
        }

        return { value: url.toString() };
    } catch {
        return { value: null, error: "Enter a valid website address." };
    }
};

export const safeHttpUrl = (raw: string | null | undefined): string | null => {
    if (!raw) return null;

    try {
        const url = new URL(raw);
        return url.protocol === "http:" || url.protocol === "https:" ? url.toString() : null;
    } catch {
        return null;
    }
};

export const displayUrl = (raw: string): string => {
    try {
        const url = new URL(raw);
        const path = `${url.pathname}${url.search}`.replace(/\/$/, "");

        return `${url.hostname.replace(/^www\./, "")}${path}`;
    } catch {
        return raw;
    }
};
