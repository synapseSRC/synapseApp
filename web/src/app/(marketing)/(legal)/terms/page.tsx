import LegalLayout, { LegalLink, LegalList, SupportLink } from "@/components/marketing/legal-layout";
import { generateMetadata } from "@/utils";
import { LEGAL_ENTITY, LEGAL_JURISDICTION, PROJECT_LINKS, SUPPORT_EMAIL } from "@/utils/constants/site";

export const metadata = generateMetadata({
    title: "Terms of Service — Synapse",
    description:
        "The terms for using the Synapse hosted service and the Synapse software: accounts, content ownership, acceptable use, moderation, licensing and liability.",
});

const Terms = () => {
    return (
        <LegalLayout
            title="Terms of Service"
            updated="Last updated: 31 October 2026"
            intro={
                <>
                    These terms cover two different things: using the hosted Synapse service that we
                    operate, and using the Synapse software itself. The software is MIT licensed and needs
                    no agreement from you to run; the hosted service is subject to the rules below.
                </>
            }
            sections={[
                {
                    heading: "Acceptance of these terms",
                    body: (
                        <p>
                            By creating an account or using the hosted service, you agree to these terms and
                            to the <LegalLink href="/privacy">Privacy Policy</LegalLink>. If you do not
                            agree, do not use the hosted service. Running the software yourself, contributing
                            code, or building a client on the published protocol is governed by the MIT
                            license and not by these terms.
                        </p>
                    ),
                },
                {
                    heading: "The hosted service and self-hosted deployments",
                    body: (
                        <>
                            <p>
                                The hosted service is operated by {LEGAL_ENTITY}. It includes the web app at
                                synapse.app and the native clients when they are signed in to a hosted
                                account. It is provided under these terms and the{" "}
                                <LegalLink href="/privacy">Privacy Policy</LegalLink>.
                            </p>
                            <p className="mt-4">
                                Anyone may run the MIT-licensed software for their own community. If you
                                connect to a deployment that we do not operate, that operator’s terms and
                                policy apply, and we are not responsible for its content, moderation,
                                security or availability.
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
                                "Provide a working email address and keep your credentials safe. You are responsible for activity on your account.",
                                "One account per person. Automated accounts and bots are welcome when they are labelled as such and stay within documented API rate limits.",
                                "Do not create accounts to evade a suspension, impersonate someone else, or manipulate followers, reactions or reports.",
                                "Tell us promptly if you believe your account has been compromised.",
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
                                    "Use the service for anything unlawful, or to infringe another person’s rights, including intellectual property rights.",
                                    "Post or share child sexual abuse material, content that incites violence, or content that harasses, threatens or exposes another person.",
                                    "Send spam, harvest credentials, scrape the service at scale, or run automation that degrades it for others.",
                                    "Attempt to break end-to-end encryption, extract keys from another person’s device, or access an account or system without permission.",
                                    "Probe or test the service in ways that disrupt it. Report vulnerabilities through coordinated disclosure to the contact below.",
                                    "Impersonate other people or communities in ways that mislead their members.",
                                    "Circumvent rate limits, bans, access controls or storage quotas.",
                                ]}
                            />
                            <p className="mt-4">
                                Space owners and moderators set the rules for their own communities. Their
                                decisions bind their members; our decisions bind the hosted service as a
                                whole.
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
                                licence needed to operate the service: storing, copying, transmitting and
                                displaying your content to the people and clients entitled to receive it, and
                                maintaining backups. That licence ends when you delete the content or your
                                account, except for copies in backups that rotate on the schedule in the
                                Privacy Policy.
                            </p>
                            <p className="mt-4">
                                Direct messages composed in native clients are end-to-end encrypted. We cannot
                                read, review, moderate or hand over their plaintext, and we will not build a
                                capability to do so. The web client does not implement that encryption, so do
                                not send content through the web that you need the service to be unable to
                                read.
                            </p>
                            <p className="mt-4">
                                You are responsible for having the rights to what you upload, for the content
                                you publish, and for the conduct of anyone you invite to a space you own.
                            </p>
                        </>
                    ),
                },
                {
                    heading: "Privacy and cookies",
                    body: (
                        <p>
                            The <LegalLink href="/privacy">Privacy Policy</LegalLink> explains what data we
                            collect and how to exercise your rights. The{" "}
                            <LegalLink href="/cookies">Cookie Policy</LegalLink> lists the strictly necessary
                            cookies we set and the third-party requests involved in sign-in and default
                            avatars.
                        </p>
                    ),
                },
                {
                    heading: "Copyright complaints",
                    body: (
                        <p>
                            If you believe content on the hosted service infringes your copyright, email{" "}
                            <SupportLink email={SUPPORT_EMAIL} /> with the work you own, the link to the
                            content, your contact details, a statement that you have a good-faith belief the
                            use is unauthorised, a statement that the information is accurate and that you
                            are the owner or authorised to act for them, and your signature. We may remove
                            content while we investigate. Accounts that repeatedly infringe may be
                            suspended or terminated. If your content was removed by mistake, contact us and
                            we will review it.
                        </p>
                    ),
                },
                {
                    heading: "Moderation, suspension and termination",
                    body: (
                        <p>
                            We moderate the hosted service for legal compliance, abuse and the protection of
                            members. Actions are proportionate: content removal, role removal, temporary
                            limitation, suspension, then termination. Where we can, we tell you the reason
                            and the evidence we can share, and decisions can be appealed to{" "}
                            <SupportLink email={SUPPORT_EMAIL} />. You may close your account at any time;
                            deletion works as described in the Privacy Policy. If we shut down the hosted
                            service, we give notice and keep export available for a reasonable period so
                            communities can move to a deployment of their own.
                        </p>
                    ),
                },
                {
                    heading: "The Synapse license and trademarks",
                    body: (
                        <>
                            <p>
                                Synapse clients, server software, protocol specification and SDKs are
                                released under the MIT license. You may use, copy, modify, redistribute and
                                fork them, including commercially, subject to that license.
                            </p>
                            <p className="mt-4">
                                The name “Synapse”, the logo and the project’s repository URLs are not part of
                                that grant. Use them only to describe the real project accurately — do not
                                name a fork in a way that implies it is ours, and do not imply our endorsement
                                of your deployment or client. Forks are welcome; ask for a distinct name.
                            </p>
                        </>
                    ),
                },
                {
                    heading: "Disclaimers",
                    body: (
                        <p>
                            The software is provided “as is”, without warranty of any kind, to the extent the
                            MIT license allows. The hosted service is offered without an uptime guarantee,
                            and features may change or be withdrawn. Security is a serious design goal, not a
                            promise that no vulnerability will ever exist; report what you find and we will
                            fix it. Nothing in these terms excludes warranties or rights that cannot be
                            excluded by law.
                        </p>
                    ),
                },
                {
                    heading: "Limitation of liability",
                    body: (
                        <p>
                            To the maximum extent permitted by law, we are not liable for indirect,
                            incidental or consequential loss, or for lost data, arising from use of the
                            software or the hosted service, and our aggregate liability for any claim is
                            limited to the greater of one hundred euros and the fees you have paid us in the
                            preceding twelve months. Nothing here limits liability that cannot be limited,
                            including for wilful misconduct, death or personal injury caused by negligence,
                            or our own data protection obligations.
                        </p>
                    ),
                },
                {
                    heading: "Indemnity",
                    body: (
                        <p>
                            To the extent permitted by law, you agree to indemnify us against third-party
                            claims and costs arising from content you publish or your breach of these terms.
                            If you are a consumer, this section applies only so far as mandatory law allows.
                        </p>
                    ),
                },
                {
                    heading: "Changes to these terms",
                    body: (
                        <p>
                            We may revise these terms as the service evolves. Material changes are announced
                            at least 30 days before they take effect, by email and in the{" "}
                            <LegalLink href="/changelog">changelog</LegalLink>. Continuing to use the hosted
                            service afterwards means you accept them. Source already released under MIT is
                            never retroactively re-licensed.
                        </p>
                    ),
                },
                {
                    heading: "Governing law and disputes",
                    body: (
                        <p>
                            These terms are governed by the laws of {LEGAL_JURISDICTION}, without regard to
                            conflict-of-law rules. Courts in that jurisdiction have exclusive jurisdiction,
                            except where mandatory law gives you the right to bring a claim in your country
                            of residence. Before starting a formal claim, write to{" "}
                            <SupportLink email={SUPPORT_EMAIL} /> so we can try to resolve it. Nothing in
                            these terms deprives you of protections you are entitled to under the mandatory
                            law of your country of residence.
                        </p>
                    ),
                },
                {
                    heading: "Contact",
                    body: (
                        <p>
                            Service of legal notices, abuse reports and questions about these terms:{" "}
                            <SupportLink email={SUPPORT_EMAIL} />. Technical issues are better raised at{" "}
                            <LegalLink href={PROJECT_LINKS.issues}>the repository issue tracker</LegalLink>.
                        </p>
                    ),
                },
            ]}
            closing={
                <>
                    By using the hosted service you confirm that you have read and accept these terms.
                    Running Synapse somewhere else needs no agreement from us at all.
                </>
            }
        />
    );
};

export default Terms;
