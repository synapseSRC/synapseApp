import FeaturePage from "@/components/marketing/feature-page";
import { MessagingPreview } from "@/components/marketing/feature-previews";
import { generateMetadata } from "@/utils";
import { LockIcon, MessagesSquareIcon, SearchIcon } from "lucide-react";

export const metadata = generateMetadata({
    title: "Encrypted direct messages — Synapse",
    description:
        "Direct messages on Synapse are end-to-end encrypted by default, with keys generated on your device. Threads, reactions and search across one synced inbox.",
});

const MessagingPage = () => (
    <FeaturePage
        badge="Messaging"
        title="DMs that stay between you"
        description="One inbox for every conversation, encrypted by default and identical on all five clients — including the terminal."
        preview={<MessagingPreview />}
        previewLabel="Encrypted DM thread, synced across a phone, a desktop and the CLI."
        capabilities={[
            {
                title: "End-to-end encryption, on by default",
                description:
                    "Keys are generated on your device and never leave it. The node stores ciphertext plus the routing metadata it needs to deliver — nothing else.",
                icon: LockIcon,
            },
            {
                title: "Threads, reactions and full-text search",
                description:
                    "Reply inside a message instead of flooding the channel, react to keep the thread short, and search locally over the history you already have.",
                icon: MessagesSquareIcon,
            },
            {
                title: "One inbox across five clients",
                description:
                    "Read on Android, answer from the CLI, finish on Windows. Read receipts, typing state and drafts share the same store.",
                icon: SearchIcon,
            },
        ]}
        cta={{
            title: "Message like it is private",
            description:
                "Install a client and your first encrypted DM takes about a minute — no account approval needed.",
        }}
    />
);

export default MessagingPage;
