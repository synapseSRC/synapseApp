import LegalLayout, { LegalList, SupportLink } from "@/components/marketing/legal-layout";
import { generateMetadata } from "@/utils";
import { PROJECT_LINKS, SUPPORT_EMAIL } from "@/utils/constants/site";
import Link from "next/link";

export const metadata = generateMetadata({
    title: "Terms of Service — Synapse",
    description:
        "The terms for using the Synapse hosted network and the Synapse software: accounts, content ownership, acceptable use, licensing and disclaimers.",
});

const Terms = () => {
    return (
        <LegalLayout
            title="Terms of Service"
            updated="Last updated: 15th October 2026"
            intro={
                <>
                    These terms cover two different things: using the hosted Synapse network that we operate,
                    and using the Synapse software itself. The software is MIT licensed and needs no agreement
                    from you to run; the hosted network is a service with rules. Read the section that applies
                    to you.
                </>
            }
            sections={[
                {
                    heading: "Acceptance of these terms",
                    body: (
                        <p>
                            By creating an account on the hosted network you agree to these terms. If you do
                            not agree, do not use the hosted network. Running your own node, contributing code,
                            or building your own client on the published protocol is governed by the MIT
                            license and not by these terms.
                        </p>
                    ),
                },
                {
                    heading: "The hosted network and self-hosted nodes",
                    body: (
                        <>
                            <p>
                                We operate synapse.app as a public service under these terms and the{" "}
                                <Link href="/privacy" className="text-foreground underline underline-offset-4">
                                    privacy policy
                                </Link>
                                . Anyone can operate a node for their own community; when you connect to a node
                                that is not ours, that operator&apos;s rules apply and ours do not. We are not
                                responsible for content, moderation or availability on third-party nodes.
                            </p>
                            <p className="mt-4">
                                Federation means your messages travel to nodes we do not control. Publish
                                accordingly.
                            </p>
                        </>
                    ),
                },
                {
                    heading: "Accounts",
                    body: (
                        <LegalList
                            items={[
                                "You must be at least 16, or the age of digital consent in your country if that is higher.",
                                "Keep your credentials safe and use a second factor where the client supports it. You are responsible for activity on your account.",
                                "One account per person. Automated accounts and bots are welcome when they are labelled as such and use the API within its rate limits.",
                                "Give us a working email address so we can reach you about security issues affecting your data.",
                            ]}
                        />
                    ),
                },
                {
                    heading: "Acceptable use",
                    body: (
                        <>
                            <p>You agree not to:</p>
                            <LegalList
                                items={[
                                    "Use Synapse for anything unlawful, or to infringe someone else's rights.",
                                    "Send spam, harvest credentials, or run unofficial automation that degrades the service for others.",
                                    "Attempt to break end-to-end encryption, extract keys from another person&apos;s device, or defeat access controls.",
                                    "Reverse-engineer our proprietary hosting arrangements; testing the published software is encouraged, within coordinated disclosure.",
                                    "Impersonate other people or communities in ways that mislead their members.",
                                    "Overload the API, node or federation handshakes beyond documented limits.",
                                ]}
                            />
                            <p className="mt-4">
                                Space owners and moderators set the rules for their own communities. Their
                                decisions bind their members; our decisions bind the hosted network as a whole.
                            </p>
                        </>
                    ),
                },
                {
                    heading: "Your content",
                    body: (
                        <>
                            <p>
                                You keep ownership of everything you post. You grant us only the limited
                                licence needed to do the job you asked for: storing it, delivering it to the
                                devices and nodes entitled to receive it, and showing it inside the clients.
                                That licence ends when you delete the content or your account.
                            </p>
                            <p className="mt-4">
                                Because direct messages are end-to-end encrypted, we cannot read, review,
                                moderate or hand over their plaintext, and we will not build a capability to do
                                so. Content posted in a federated space may be retained by other operators
                                after you delete it there.
                            </p>
                            <p className="mt-4">
                                You are responsible for having the rights to what you upload, and for the
                                conduct of anyone using your node.
                            </p>
                        </>
                    ),
                },
                {
                    heading: "The Synapse license and trademarks",
                    body: (
                        <>
                            <p>
                                Synapse clients, node software, protocol specification and SDKs are released
                                under the MIT license. You may use, copy, modify, redistribute and fork them,
                                including commercially, subject to that license.
                            </p>
                            <p className="mt-4">
                                The name &quot;Synapse&quot;, the logo and the project&apos;s repository URLs are not part
                                of that grant. Use them only to describe the real project accurately — do not
                                name a fork in a way that implies it is ours, and do not imply our endorsement
                                of your node or client. Forks are welcome; ask for a distinct name.
                            </p>
                        </>
                    ),
                },
                {
                    heading: "Moderation, suspension and termination",
                    body: (
                        <p>
                            We moderate the hosted network for legal compliance, abuse of the service, and
                            protection of members. Actions are proportionate: content removal, role removal,
                            temporary limitation, suspension, then termination. Moderation decisions on the
                            hosted network can be appealed to{" "}
                            <SupportLink email={SUPPORT_EMAIL} /> and we respond with the reason and the
                            evidence we can share. You may terminate your account at any time; deletion works
                            as described in the privacy policy. If we shut down or hand over the hosted
                            network, we give notice and keep export available for a reasonable period so
                            communities can migrate to a node of their own.
                        </p>
                    ),
                },
                {
                    heading: "Disclaimers",
                    body: (
                        <p>
                            The software is provided &quot;as is&quot;, without warranty of any kind, to the extent the
                            MIT license allows. The hosted network is offered without an uptime guarantee.
                            Security is a serious design goal, not a promise that no vulnerability will ever
                            exist; report what you find and we will fix it.
                        </p>
                    ),
                },
                {
                    heading: "Limitation of liability",
                    body: (
                        <p>
                            To the maximum extent permitted by law, we are not liable for indirect, incidental
                            or consequential loss, or for lost data, arising from use of the software or the
                            hosted network, and our aggregate liability for any claim is limited to the greater
                            of one hundred euros and the fees you have paid us in the preceding twelve months.
                            Nothing here limits liability that cannot be limited, including for wilful
                            misconduct or our own data protection obligations.
                        </p>
                    ),
                },
                {
                    heading: "Changes to these terms",
                    body: (
                        <p>
                            We may revise these terms as the software and the service evolve. Material changes
                            are announced at least 30 days before they take effect, in the Synapse space, by
                            email, and in the{" "}
                            <Link href="/changelog" className="text-foreground underline underline-offset-4">
                                changelog
                            </Link>
                            . Continuing to use the hosted network afterwards means you accept them. Source
                            already released under MIT is never retroactively re-licensed.
                        </p>
                    ),
                },
                {
                    heading: "Governing law",
                    body: (
                        <p>
                            These terms are governed by the laws of the jurisdiction in which the hosted
                            network is operated, without regard to conflict-of-law rules, and do not deprive
                            you of protections required by the law of your country of residence.
                            <span className="text-foreground">
                            {" "}
                            Replace this placeholder with the operating entity and jurisdiction before launch.
                            </span>
                        </p>
                    ),
                },
                {
                    heading: "Contact",
                    body: (
                        <p>
                            Service of legal notices, abuse reports and questions about these terms:{" "}
                            <SupportLink email={SUPPORT_EMAIL} />. Technical issues are better raised at{" "}
                            <Link
                                href={PROJECT_LINKS.issues}
                                className="text-foreground underline underline-offset-4"
                            >
                                the repository issue tracker
                            </Link>
                            .
                        </p>
                    ),
                },
            ]}
            closing={
                <>
                    By using the hosted network you confirm that you have read and accept these terms. Running
                    Synapse somewhere else needs no agreement from us at all.
                </>
            }
        />
    );
};

export default Terms;
