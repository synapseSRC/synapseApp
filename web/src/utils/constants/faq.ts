export const FAQ = [
    {
        id: "item-1",
        question: "Is Synapse free?",
        answer: "Yes. Every client is MIT licensed, and the hosted network is free to join with no seat limits and no ads. Self-hosting costs whatever your own hardware costs.",
    },
    {
        id: "item-2",
        question: "Which platforms are supported?",
        answer: "Android 8.0 and later, iOS 16 and later, Windows 10 and 11 (64-bit), Linux as .deb, .rpm, AppImage and Flatpak, plus a CLI for macOS, Linux and WSL that needs Node.js 18 or newer.",
    },
    {
        id: "item-3",
        question: "Do I have to trust someone else's server?",
        answer: "No. You can run your own node with docker compose and point your clients at it. The protocol is the same either way, so a self-hosted node still talks to the wider network.",
    },
    {
        id: "item-4",
        question: "Are my messages encrypted?",
        answer: "Direct messages are end-to-end encrypted by default, with keys generated on your device. A server stores ciphertext and the routing metadata it needs to deliver messages.",
    },
    {
        id: "item-5",
        question: "Does it work with a bad connection?",
        answer: "The clients are offline-first. You can read, write and react with no network, and everything merges when the connection comes back — including drafts started on one device and finished on another.",
    },
    {
        id: "item-6",
        question: "How do updates reach me?",
        answer: "Desktop and mobile clients update through their store, and every build is published on the releases page. Self-hosted deployments are pinned by version, so an update never surprises a running node.",
    },
    {
        id: "item-7",
        question: "Can I automate it?",
        answer: "Yes. Install the CLI with npm or Homebrew, create a token, and use the same REST API and signed webhooks the official clients use. SDKs for TypeScript, Go and Python ship with each release.",
    },
    {
        id: "item-8",
        question: "How do I report a bug or contribute?",
        answer: "Open an issue on the repository, or start a thread in the discussions area for protocol questions. Good-first-issues are labelled, and changes to the wire format go through a public RFC.",
    },
];
