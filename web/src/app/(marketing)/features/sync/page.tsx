import FeaturePage from "@/components/marketing/feature-page";
import { SyncPreview } from "@/components/marketing/feature-previews";
import { generateMetadata } from "@/utils";
import { CloudOffIcon, GaugeIcon, RefreshCwIcon } from "lucide-react";

export const metadata = generateMetadata({
    title: "Offline-first cross-device sync — Synapse",
    description:
        "Synapse clients are offline-first. Write with no connection, merge with CRDTs when you are back, and hand a draft from your phone to your laptop mid-sentence.",
});

const SyncPage = () => (
    <FeaturePage
        badge="Sync"
        title="One conversation, every screen"
        description="A draft written on a train and a reply typed on a laptop an hour later are the same conversation. The merge is settled by causality, not by whoever synced last."
        preview={<SyncPreview />}
        previewLabel="Three devices writing to one local store; pending writes settle on reconnect."
        capabilities={[
            {
                title: "Offline-first, not offline-tolerant",
                description:
                    "Every client writes to a local store first and never blocks the interface on the network. Reading, writing and reacting all work with no connection.",
                icon: CloudOffIcon,
            },
            {
                title: "Instant handoff between devices",
                description:
                    "Unsent drafts belong to the device that wrote them, then transfer cleanly when you pick the thread up somewhere else.",
                icon: RefreshCwIcon,
            },
            {
                title: "Built for bad networks",
                description:
                    "Delta sync, backoff that respects your battery, and history fetched on demand instead of at launch — a cold start does not need a download.",
                icon: GaugeIcon,
            },
        ]}
        cta={{
            title: "Stop waiting on a spinner",
            description:
                "Install on two devices and watch a message you wrote underground arrive everywhere at once.",
        }}
    />
);

export default SyncPage;
