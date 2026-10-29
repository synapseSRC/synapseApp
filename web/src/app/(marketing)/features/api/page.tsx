import FeaturePage from "@/components/marketing/feature-page";
import { ApiPreview } from "@/components/marketing/feature-previews";
import { generateMetadata } from "@/utils";
import { Code2Icon, TerminalIcon, WebhookIcon } from "lucide-react";

export const metadata = generateMetadata({
    title: "Open API and CLI — Synapse",
    description:
        "Synapse exposes the same REST API and signed webhooks its own clients use, ships a CLI for the terminal, and publishes the protocol and SDKs for TypeScript, Go and Python.",
});

const ApiPage = () => (
    <FeaturePage
        badge="API & CLI"
        title="An API, not an afterthought"
        description="The protocol is public, the OpenAPI spec ships with every release, and the official clients are built on the same surface you get with a token."
        preview={<ApiPreview />}
        previewLabel="REST endpoints and a signed webhook payload from api.synapse.app/v1."
        capabilities={[
            {
                title: "REST API with signed webhooks",
                description:
                    "Tokens scoped per space, cursor pagination, idempotency keys, and webhook deliveries signed with replay protection.",
                icon: WebhookIcon,
            },
            {
                title: "A CLI that fits a pipeline",
                description:
                    "synapse dm, synapse send and synapse watch emit newline-delimited JSON, exit non-zero on failure, and read a token from the environment.",
                icon: TerminalIcon,
            },
            {
                title: "Open protocol and SDKs",
                description:
                    "Wire format, federation handshake and auth flow are documented; TypeScript, Go and Python SDKs are released alongside the server.",
                icon: Code2Icon,
            },
        ]}
        cta={{
            title: "Build on it today",
            description:
                "Install the CLI, mint a space token, and you have a working integration before the coffee goes cold.",
        }}
    />
);

export default ApiPage;
