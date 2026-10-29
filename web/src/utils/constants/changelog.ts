export type ChangeKind = "added" | "improved" | "fixed";

export type Change = {
    kind: ChangeKind;
    description: string;
};

export type Release = {
    version: string;
    date: string;
    summary: string;
    changes: Change[];
};

export const RELEASES: Release[] = [
    {
        version: "1.0.0",
        date: "October 14, 2026",
        summary:
            "The first release we are willing to put our name on. Stable protocol, stable API, stable sync.",
        changes: [
            { kind: "added", description: "End-to-end encrypted DMs with per-device key backup." },
            { kind: "added", description: "Spaces: hierarchical channels, roles and audit log." },
            { kind: "added", description: "`synapse dm`, `synapse send` and `synapse watch` in the CLI." },
            { kind: "improved", description: "Cold start on Android is 2.4x faster; the feed renders before sync completes." },
            { kind: "fixed", description: "Drafts could be duplicated when composing offline on flaky connections." },
        ],
    },
    {
        version: "0.9.0",
        date: "September 9, 2026",
        summary: "Sync rewrite and the first public preview of the desktop clients.",
        changes: [
            { kind: "added", description: "Offline-first message store with CRDT merge across devices." },
            { kind: "added", description: "Windows and Linux preview builds." },
            { kind: "improved", description: "Webhooks are now signed; replay protection uses a 5-minute window." },
            { kind: "fixed", description: "Read receipts could race ahead of the message they referred to." },
        ],
    },
    {
        version: "0.8.0",
        date: "July 28, 2026",
        summary: "Protocol freeze for v1, plus the self-hosting toolchain.",
        changes: [
            { kind: "added", description: "`synapse init` provisions a single-node server with TLS." },
            { kind: "added", description: "OpenAPI spec for the REST surface, published with every release." },
            { kind: "improved", description: "Federation handshakes reduced from four round trips to one." },
            { kind: "fixed", description: "Membership export truncated spaces above 10,000 members." },
        ],
    },
];
