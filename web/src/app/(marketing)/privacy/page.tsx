import LegalLayout, { LegalList, SupportLink } from "@/components/marketing/legal-layout";
import { generateMetadata } from "@/utils";
import { SUPPORT_EMAIL } from "@/utils/constants/site";
import Link from "next/link";

export const metadata = generateMetadata({
    title: "Privacy Policy — Synapse",
    description:
        "What Synapse stores about you on the hosted network, how end-to-end encryption changes that, and what a self-hosted node operator is responsible for.",
});

const Privacy = () => {
    return (
        <LegalLayout
            title="Privacy Policy"
            updated="Last updated: 15th October 2026"
            intro={
                <>
                    This policy explains what Synapse collects when you use the hosted network, why it is
                    collected, and what you can do about it. It also explains where responsibility shifts
                    when you run your own node instead. Synapse is MIT licensed software — the privacy
                    behaviour described here is the behaviour of the published source, not a promise that
                    lives only on this page.
                </>
            }
            sections={[
                {
                    heading: "The hosted network and your own node",
                    body: (
                        <>
                            <p>
                                There are two ways to use Synapse, and they have different operators.
                            </p>
                            <LegalList
                                items={[
                                    <>
                                        <strong className="text-foreground">Hosted network.</strong> We run
                                        the node at synapse.app and act as the data controller for the
                                        information described below.
                                    </>,
                                    <>
                                        <strong className="text-foreground">Self-hosted node.</strong> You, or
                                        whoever runs the server you connect to, is the operator and controller.
                                        The same data categories apply, but their handling, retention and legal
                                        basis are that operator&apos;s responsibility. Publish your own policy
                                        for your node.
                                    </>,
                                ]}
                            />
                            <p>
                                Direct messages are end-to-end encrypted either way, so running or joining a
                                node never requires trusting a server with message plaintext.
                            </p>
                        </>
                    ),
                },
                {
                    heading: "Information we collect",
                    body: (
                        <>
                            <LegalList
                                items={[
                                    <>
                                        <strong className="text-foreground">Account data.</strong> Handle,
                                        display name, email address used to register, and hashed credentials.
                                    </>,
                                    <>
                                        <strong className="text-foreground">Profile data.</strong> Avatar,
                                        bio, pronouns and the device presence you choose to expose.
                                    </>,
                                    <>
                                        <strong className="text-foreground">Content.</strong> Messages,
                                        threads, reactions, attachments, space descriptions and pins you
                                        create. Encrypted message bodies are stored as ciphertext.
                                    </>,
                                    <>
                                        <strong className="text-foreground">Membership data.</strong> Which
                                        spaces you belong to, your role in each, and channel read state.
                                    </>,
                                    <>
                                        <strong className="text-foreground">Sync and device metadata.</strong>{" "}
                                        Registered device names, client versions, push notification tokens, and
                                        the sync cursor for each device.
                                    </>,
                                    <>
                                        <strong className="text-foreground">Diagnostics.</strong> Crash reports
                                        and performance traces, sent only when you submit them. The hosted
                                        network serves no advertising and runs no third-party tracking pixels.
                                    </>,
                                ]}
                            />
                            <p>
                                We do not collect your contacts, and we do not build a behavioural profile of
                                you.
                            </p>
                        </>
                    ),
                },
                {
                    heading: "Messages and end-to-end encryption",
                    body: (
                        <>
                            <p>
                                Direct messages are encrypted on your device with keys that are generated
                                locally and never uploaded in plaintext. A node stores the resulting
                                ciphertext plus the routing metadata it needs to deliver a message: sender,
                                recipient, space, and timestamp. Group messages in spaces are encrypted in
                                transit and, where a space enables it, end-to-end as well.
                            </p>
                            <p className="mt-4">
                                Encryption protects content. It does not hide the fact that two accounts
                                exchanged messages, which is why metadata minimisation and retention limits
                                matter as much as the cryptography.
                            </p>
                        </>
                    ),
                },
                {
                    heading: "How we use information",
                    body: (
                        <LegalList
                            items={[
                                "To deliver messages, threads and notifications to your devices.",
                                "To keep sync consistent when you are offline or on several devices.",
                                "To operate spaces: membership, roles, and the moderation audit log.",
                                "To keep the software secure, including diagnosing crashes you choose to report.",
                                "To answer support requests you send us.",
                            ]}
                        />
                    ),
                },
                {
                    heading: "How information is shared",
                    body: (
                        <>
                            <p>
                                We do not sell personal information and we do not share it with advertisers.
                                Information is disclosed only in these situations:
                            </p>
                            <LegalList
                                items={[
                                    <>
                                        <strong className="text-foreground">Federation.</strong> Content you
                                        post in a space that other nodes can read is delivered to those nodes
                                        and becomes subject to their operators&apos; policies.
                                    </>,
                                    <>
                                        <strong className="text-foreground">Infrastructure providers.</strong>{" "}
                                        Hosting, object storage and email delivery, under processing
                                        agreements.
                                    </>,
                                    <>
                                        <strong className="text-foreground">Legal requests.</strong> Where we
                                        are required to respond to a valid, binding request from an authority,
                                        limited to the data a node actually holds — which for encrypted
                                        messages means ciphertext.
                                    </>,
                                    <>
                                        <strong className="text-foreground">A transfer of the project.</strong>{" "}
                                        If the hosted network changes hands, this policy travels with the
                                        data and users are notified.
                                    </>,
                                ]}
                            />
                        </>
                    ),
                },
                {
                    heading: "Retention",
                    body: (
                        <p>
                            Account, content and membership data are kept while your account exists. Message
                            ciphertext is retained so that new devices can catch up, subject to the retention
                            window configured on the node. Push tokens are replaced as soon as a device
                            unregisters. Crash diagnostics are aggregated after 30 days. Deleting your account
                            removes your profile, content and membership within 30 days, except where a copy
                            must persist in another node&apos;s federated history or in a legal hold.
                        </p>
                    ),
                },
                {
                    heading: "Security",
                    body: (
                        <p>
                            Traffic is encrypted in transit, credentials are stored using a memory-hard hash,
                            and access to production systems is logged and limited to maintainers with a
                            documented reason. Reproducible builds and signed releases let you verify that the
                            client you installed matches the published source. No system is completely secure;
                            if we learn of a breach affecting your data we will tell you and any affected node
                            operators without unreasonable delay.
                        </p>
                    ),
                },
                {
                    heading: "Your rights and choices",
                    body: (
                        <>
                            <p>Wherever you live, you can:</p>
                            <LegalList
                                items={[
                                    "Export your account, messages and media as machine-readable files from the client settings.",
                                    "Correct your profile and account details at any time.",
                                    "Delete your account, including its content, from every node you joined.",
                                    "Revoke individual device sessions and rotate your encryption keys.",
                                    "Object to processing, or request restriction, by contacting us.",
                                ]}
                            />
                            <p>
                                If you self-host, these operations are available to you as the operator through
                                the same node API, and to your members through their client.
                            </p>
                        </>
                    ),
                },
                {
                    heading: "International transfers",
                    body: (
                        <p>
                            The hosted network runs in the European Union and the United States. Where data is
                            transferred outside your region we rely on standard contractual clauses, and
                            federation means content you publish in a space may reach nodes in other
                            jurisdictions. If cross-border processing is a constraint for your community, run a
                            node inside the jurisdiction you need.
                        </p>
                    ),
                },
                {
                    heading: "Children",
                    body: (
                        <p>
                            Synapse is not directed at children under 16 and we do not knowingly collect their
                            data on the hosted network. Communities who run a node for younger members set
                            their own age assurance and moderation rules; the software supports that with role
                            limits and private spaces. If you believe a child has created an account on the
                            hosted network, contact us and we will delete it.
                        </p>
                    ),
                },
                {
                    heading: "Open source and this policy",
                    body: (
                        <>
                            <p>
                                Because the clients and the node are published under the MIT license, you can
                                verify every claim on this page against the code. If a release changes
                                data-handling behaviour, the changelog entry and the diff are both public.
                                Report anything that looks different from what is described here to{" "}
                                <SupportLink email={SUPPORT_EMAIL} />.
                            </p>
                            <p className="mt-4">
                                Our marketing site itself uses no analytics and sets no non-essential cookies.
                            </p>
                        </>
                    ),
                },
                {
                    heading: "Changes to this policy",
                    body: (
                        <p>
                            We update this policy when the software changes in a way that affects data
                            handling. The date at the top of the page is revised, the diff is published
                            alongside the release notes at{" "}
                            <Link href="/changelog" className="text-foreground underline underline-offset-4">
                                the changelog
                            </Link>
                            , and material changes are announced in the Synapse space and by email to
                            affected accounts.
                        </p>
                    ),
                },
                {
                    heading: "Contact",
                    body: (
                        <p>
                            Questions, requests or complaints about privacy: write to{" "}
                            <SupportLink email={SUPPORT_EMAIL} />. You also have the right to complain to your
                            local data protection authority. If you need a data processing agreement to run a
                            community on the hosted network, ask for one at the same address.
                        </p>
                    ),
                },
            ]}
            closing={
                <>
                    By using the hosted network you agree to this policy. If you disagree, run your own node
                    instead — the software is yours to operate, and that is the point.
                </>
            }
        />
    );
};

export default Privacy;
