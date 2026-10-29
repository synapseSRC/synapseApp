import FeaturePage from "@/components/marketing/feature-page";
import { CommunitiesPreview } from "@/components/marketing/feature-previews";
import { generateMetadata } from "@/utils";
import { HashIcon, ShieldCheckIcon, UsersIcon } from "lucide-react";

export const metadata = generateMetadata({
    title: "Communities and spaces — Synapse",
    description:
        "Synapse spaces give communities organized channels, granular roles and permissions, and a moderation toolkit with a full audit log.",
});

const CommunitiesPage = () => (
    <FeaturePage
        badge="Communities"
        title="Spaces built for real communities"
        description="A hackerspace, a study group, a company — Spaces hold channels, roles and history so a community can run itself without a spreadsheet."
        preview={<CommunitiesPreview />}
        previewLabel="A space with channels, role tiers and the moderation audit log."
        capabilities={[
            {
                title: "Channels that stay organized",
                description:
                    "Nest channels inside a space, pin what matters, and let a thread hold the noise. Member counts and last activity are visible from the list.",
                icon: HashIcon,
            },
            {
                title: "Roles and permissions you can reason about",
                description:
                    "Admin, moderator, member, guest — plus per-channel overrides. Permission checks run the same way on the node and in every client.",
                icon: UsersIcon,
            },
            {
                title: "A moderation toolkit with a paper trail",
                description:
                    "Reports, holds, timeouts and role changes land in an append-only audit log, so a decision can be reviewed after the fact.",
                icon: ShieldCheckIcon,
            },
        ]}
        cta={{
            title: "Move your community somewhere it can run itself",
            description:
                "Import a channel list, set the roles, and hand the keys to your moderators — self-hosted or on the hosted network.",
        }}
    />
);

export default CommunitiesPage;
