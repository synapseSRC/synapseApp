import { GITHUB_REPOSITORY } from "@/utils/constants/site";

import { parseReleaseNotes, type ReleaseGroup } from "./release-notes";

export type ChangelogRelease = {
    tag: string;
    title: string;
    url: string;
    publishedAt: string | null;
    groups: ReleaseGroup[];
};

export type ReleaseFeed =
    | { status: "ok"; releases: ChangelogRelease[] }
    | { status: "error"; rateLimited: boolean };

type GitHubRelease = {
    tag_name?: string | null;
    name?: string | null;
    body?: string | null;
    html_url?: string | null;
    published_at?: string | null;
    created_at?: string | null;
    draft?: boolean | null;
    prerelease?: boolean | null;
};

const ENDPOINT = `https://api.github.com/repos/${GITHUB_REPOSITORY.owner}/${GITHUB_REPOSITORY.name}/releases?per_page=10`;
const USER_AGENT = "Synapse-Web";
const REQUEST_TIMEOUT_MS = 10_000;
const CACHE_SECONDS = 6 * 60 * 60;
const RELEASE_PATH_PREFIX =
    `/${GITHUB_REPOSITORY.owner}/${GITHUB_REPOSITORY.name}/releases/`.toLowerCase();

/**
 * Published releases from GitHub, newest first. Requests are anonymous unless `GITHUB_TOKEN` is
 * set, and reuse the Android cache window of six hours. Failures are returned as data so a GitHub
 * outage renders an honest notice instead of breaking the page.
 */
export async function getReleaseFeed(): Promise<ReleaseFeed> {
    try {
        const token = process.env.GITHUB_TOKEN;
        const response = await fetch(ENDPOINT, {
            headers: {
                Accept: "application/vnd.github+json",
                "User-Agent": USER_AGENT,
                ...(token ? { Authorization: `Bearer ${token}` } : {}),
            },
            next: { revalidate: CACHE_SECONDS },
            signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS),
        });
        if (!response.ok) {
            return { status: "error", rateLimited: isRateLimited(response) };
        }
        const payload: unknown = await response.json();
        if (!Array.isArray(payload)) {
            return { status: "error", rateLimited: false };
        }
        return { status: "ok", releases: toReleases(payload as GitHubRelease[]) };
    } catch {
        return { status: "error", rateLimited: false };
    }
}

function isRateLimited(response: Response): boolean {
    if (response.status === 429) return true;
    if (response.status !== 403) return false;
    return response.headers.get("x-ratelimit-remaining")?.trim() === "0";
}

/** Published releases only, newest first, one entry per tag. */
function toReleases(entries: GitHubRelease[]): ChangelogRelease[] {
    const seenTags = new Set<string>();
    return entries
        .filter((entry) => !entry.draft && !entry.prerelease)
        .map(toRelease)
        .filter((release): release is ChangelogRelease => release !== null)
        .filter((release) => {
            const key = release.tag.toLowerCase();
            if (seenTags.has(key)) return false;
            seenTags.add(key);
            return true;
        })
        .sort((left, right) => {
            const leftTime = timeOf(left.publishedAt);
            const rightTime = timeOf(right.publishedAt);
            if (leftTime !== rightTime) return rightTime - leftTime;
            return compareVersions(right.tag, left.tag);
        });
}

function toRelease(entry: GitHubRelease): ChangelogRelease | null {
    const tag = typeof entry.tag_name === "string" ? entry.tag_name.trim() : "";
    const url = typeof entry.html_url === "string" ? entry.html_url.trim() : "";
    if (!tag || !isReleasePage(url)) return null;
    const name = typeof entry.name === "string" ? entry.name.trim() : "";
    return {
        tag,
        title: name || tag,
        url,
        publishedAt: parseDate(entry.published_at) ?? parseDate(entry.created_at),
        groups: parseReleaseNotes(typeof entry.body === "string" ? entry.body : null),
    };
}

/** Release pages live at `github.com/<owner>/<repo>/releases/tag/<tag>`; nothing else is linked. */
function isReleasePage(value: string): boolean {
    try {
        const url = new URL(value);
        return (
            url.protocol === "https:" &&
            url.hostname.toLowerCase() === "github.com" &&
            (url.port === "" || url.port === "443") &&
            !url.username &&
            !url.password &&
            url.pathname.toLowerCase().startsWith(RELEASE_PATH_PREFIX)
        );
    } catch {
        return false;
    }
}

function parseDate(value: string | null | undefined): string | null {
    const raw = typeof value === "string" ? value.trim() : "";
    if (!raw) return null;
    const millis = Date.parse(raw);
    return Number.isNaN(millis) ? null : new Date(millis).toISOString();
}

function timeOf(iso: string | null): number {
    return iso ? Date.parse(iso) : Number.NEGATIVE_INFINITY;
}

/** Compares tags the way versions are, so `v1.10.0` sorts after `v1.9.2`. */
function compareVersions(left: string, right: string): number {
    const a = versionSegments(left);
    const b = versionSegments(right);
    for (let index = 0; index < Math.max(a.length, b.length); index += 1) {
        const difference = (a[index] ?? 0) - (b[index] ?? 0);
        if (difference !== 0) return difference;
    }
    return 0;
}

function versionSegments(tag: string): number[] {
    return tag
        .trim()
        .replace(/^v/i, "")
        .split(/[-+]/, 1)[0]
        .split(".")
        .map((segment) => (/^\d+$/.test(segment.trim()) ? Number.parseInt(segment.trim(), 10) : 0));
}
