import { DownloadIcon, MessagesSquareIcon, ServerIcon } from "lucide-react";

export const PROCESS = [
    {
        title: "Install Synapse",
        description:
            "Grab a native client for Android, iOS, Windows, Linux or the terminal. Same account, same protocol, whichever way you like to work.",
        icon: DownloadIcon,
    },
    {
        title: "Join or self-host",
        description:
            "Start on the hosted network in a minute, or run your own node with docker compose and keep your community's data on your own hardware.",
        icon: ServerIcon,
    },
    {
        title: "Make it yours",
        description:
            "Script it with the CLI, build on the open API, fork the clients. Synapse is MIT licensed, so the whole stack is yours to change.",
        icon: MessagesSquareIcon,
    },
] as const;

export type Review = {
    name: string;
    username: string;
    context: string;
    rating: number;
    review: string;
};

// Placeholder quotes for layout. Replace with real community feedback before launch.
export const REVIEWS: Review[] = [
    {
        name: "Nadia K.",
        username: "@nadia",
        context: "runs a self-hosted node for a hackerspace",
        rating: 5,
        review:
            "Moved 400 members off a hosted platform onto our own box in an afternoon. The CLI did most of the work.",
    },
    {
        name: "Tomás R.",
        username: "@tomasr",
        context: "backend engineer",
        rating: 5,
        review:
            "The API is the first thing I checked and it held up. Webhooks are signed, the spec is published, nothing is magic.",
    },
    {
        name: "Priya S.",
        username: "@priya",
        context: "maintains an open-source project",
        rating: 5,
        review:
            "Our release bot lives in a 40-line script now. Threads keep the noise out of the main channel.",
    },
    {
        name: "Ben O.",
        username: "@beno",
        context: "designer, Linux desktop",
        rating: 4,
        review:
            "Flatpak behaves like a real Linux app — shortcuts, media keys, no Electron memory creep.",
    },
    {
        name: "Lucia M.",
        username: "@luciam",
        context: "community manager",
        rating: 5,
        review:
            "Roles and the audit log are what I was missing. Moderation is boring now, which is exactly what I want.",
    },
    {
        name: "Kwame A.",
        username: "@kwame",
        context: "mobile developer",
        rating: 5,
        review:
            "Compose offline on the train, and the drafts are already there when I open the laptop. That handoff is the whole product for me.",
    },
    {
        name: "Ines D.",
        username: "@inesd",
        context: "sysadmin",
        rating: 4,
        review:
            "One container, Postgres, done. Resource usage is low enough that I stopped treating it as a special service.",
    },
    {
        name: "Marek V.",
        username: "@marekv",
        context: "research group lead",
        rating: 5,
        review:
            "Our data stays on campus and encryption is on by default. Legal signed off in one meeting, which has never happened.",
    },
    {
        name: "Sasha P.",
        username: "@sashap",
        context: "indie hacker",
        rating: 5,
        review:
            "Read the protocol in an evening and wrote a working bridge the next day. Forking a client is a normal thing to do here.",
    },
];
