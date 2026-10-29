export const APP_NAME = process.env.NEXT_PUBLIC_APP_NAME || "Synapse";

export const APP_DOMAIN = process.env.NEXT_PUBLIC_APP_DOMAIN
    ? `https://${process.env.NEXT_PUBLIC_APP_DOMAIN}`
    : "https://synapse.app";

export const SUPPORT_EMAIL = "support@synapse.app";

export const GITHUB_REPOSITORY = {
    owner: "synapseSRC",
    name: "synapseApp",
} as const;

const REPOSITORY_URL = `https://github.com/${GITHUB_REPOSITORY.owner}/${GITHUB_REPOSITORY.name}`;

export const PROJECT_LINKS = {
    github: REPOSITORY_URL,
    issues: `${REPOSITORY_URL}/issues`,
    // GitHub Discussions are not enabled on the repository yet.
    discussions: "#",
    releases: `${REPOSITORY_URL}/releases`,
} as const;
