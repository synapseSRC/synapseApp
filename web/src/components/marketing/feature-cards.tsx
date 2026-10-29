import {
    ApiVisual,
    CommunitiesVisual,
    MessagingVisual,
    SyncVisual,
} from "@/components/marketing/bento-visuals";
import { LockIcon, RefreshCwIcon, TerminalIcon, UsersIcon } from "lucide-react";

export const FEATURE_CARDS = [
    {
        Icon: UsersIcon,
        name: "Communities & Spaces",
        description:
            "Channels that stay organized, with roles, permissions and an audit log for every moderation action.",
        href: "/features/communities",
        cta: "Explore communities",
        className: "col-span-3 lg:col-span-2",
        background: <CommunitiesVisual />,
    },
    {
        Icon: TerminalIcon,
        name: "Open API & CLI",
        description:
            "Send, watch and script conversations from the terminal with the same API the clients use.",
        href: "/features/api",
        cta: "Read the API docs",
        className: "col-span-3 lg:col-span-1",
        background: <ApiVisual />,
    },
    {
        Icon: RefreshCwIcon,
        name: "Sync that survives",
        description:
            "Offline-first storage, so a draft started on your phone finishes on your laptop.",
        href: "/features/sync",
        cta: "How sync works",
        className: "col-span-3 lg:col-span-1",
        background: <SyncVisual />,
    },
    {
        Icon: LockIcon,
        name: "Encrypted messaging",
        description:
            "Direct messages are end-to-end encrypted by default. Keys are generated and kept on your device; the node only ever handles ciphertext.",
        href: "/features/messaging",
        cta: "See the threat model",
        className: "col-span-3 lg:col-span-2",
        background: <MessagingVisual />,
    },
];
