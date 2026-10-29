import {
    HelpCircleIcon,
    MessagesSquareIcon,
    NewspaperIcon,
    RefreshCwIcon,
    TerminalIcon,
    UsersIcon,
} from "lucide-react";

export const NAV_LINKS = [
    {
        title: "Features",
        href: "/features/messaging",
        menu: [
            {
                title: "Messaging",
                tagline: "Encrypted DMs, threads and reactions.",
                href: "/features/messaging",
                icon: MessagesSquareIcon,
            },
            {
                title: "Communities",
                tagline: "Spaces with channels, roles and moderation.",
                href: "/features/communities",
                icon: UsersIcon,
            },
            {
                title: "Sync",
                tagline: "One conversation across every device.",
                href: "/features/sync",
                icon: RefreshCwIcon,
            },
            {
                title: "API & CLI",
                tagline: "Open API, signed webhooks and a real terminal client.",
                href: "/features/api",
                icon: TerminalIcon,
            },
        ],
    },
    {
        title: "Platforms",
        href: "/download",
    },
    {
        title: "Open Source",
        href: "/open-source",
    },
    {
        title: "Resources",
        href: "/resources/blog",
        menu: [
            {
                title: "Blog",
                tagline: "Notes from building the protocol in the open.",
                href: "/resources/blog",
                icon: NewspaperIcon,
            },
            {
                title: "Help",
                tagline: "Install guides, accounts and support.",
                href: "/resources/help",
                icon: HelpCircleIcon,
            },
        ],
    },
    {
        title: "Changelog",
        href: "/changelog",
    },
];
