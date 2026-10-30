import LegalLayout, {
    LegalLink,
    LegalList,
    LegalTable,
    SupportLink,
} from "@/components/marketing/legal-layout";
import { generateMetadata } from "@/utils";
import { LEGAL_ENTITY, SUPPORT_EMAIL } from "@/utils/constants/site";

export const metadata = generateMetadata({
    title: "Privacy Policy — Synapse",
    description:
        "What the Synapse web app and native clients collect, how messages are encrypted, who processes your data, and how to access, export or delete it.",
});

const Privacy = () => {
    return (
        <LegalLayout
            title="Privacy Policy"
            updated="Last updated: 31 October 2026"
            intro={
                <>
                    This policy explains what the Synapse hosted service collects when you use the web app
                    at synapse.app or a native client against the same account, why we collect it, and the
                    choices you have. It also marks where the clients differ: direct messages sent from a
                    native app are end-to-end encrypted, while the web client is not. Synapse is MIT
                    licensed, so the behaviour described here can be checked against the published source.
                </>
            }
            sections={[
                {
                    heading: "Who is responsible for your data",
                    body: (
                        <>
                            <p>
                                The hosted service at synapse.app is operated by {LEGAL_ENTITY}. For accounts
                                on the hosted service, that operator is the data controller for the personal
                                data described in this policy.
                            </p>
                            <p className="mt-4">
                                If you use a Synapse deployment run by someone else, that operator controls
                                the data on their deployment and their own policy applies. We do not receive
                                data from an independent deployment unless you create an account with us.
                            </p>
                            <p className="mt-4">
                                Privacy questions and requests: <SupportLink email={SUPPORT_EMAIL} />. If you
                                need a postal address for a formal notice, ask at the same address.
                            </p>
                        </>
                    ),
                },
                {
                    heading: "What we collect",
                    body: (
                        <>
                            <p>We collect the following categories of data:</p>
                            <LegalList
                                items={[
                                    <>
                                        <strong className="text-foreground">Account data.</strong> Email
                                        address, username, display name, and a hashed password. If you sign in
                                        with Google, we receive your Google account identifier and the name,
                                        email address and avatar Google shares with us. We also store your
                                        email-confirmation state and session records.
                                    </>,
                                    <>
                                        <strong className="text-foreground">Profile data.</strong> Anything
                                        you add to your profile: avatar, cover image, bio, pronouns, location,
                                        website, work, education, relationship status, public email and linked
                                        accounts. You choose which of these to provide and whether your
                                        account is private.
                                    </>,
                                    <>
                                        <strong className="text-foreground">Content.</strong> Posts, replies,
                                        quotes, polls, reactions, messages, attachments, community and chat
                                        names and descriptions, and reports you file. If you attach a place to
                                        a post, we store the place name, address and coordinates.
                                    </>,
                                    <>
                                        <strong className="text-foreground">Technical data.</strong> IP
                                        address, request timestamps and paths, browser and client version,
                                        session cookies, and — for native apps — device names, device
                                        identifiers and push notification tokens.
                                    </>,
                                    <>
                                        <strong className="text-foreground">Diagnostics.</strong> Error and
                                        crash reports you choose to submit, plus server logs needed to run and
                                        secure the service.
                                    </>,
                                ]}
                            />
                            <p>We do not collect:</p>
                            <LegalList
                                items={[
                                    "Your phone contacts or address book.",
                                    "Precise location in the background. Location is stored only when you attach a place to a post.",
                                    "Advertising identifiers, cross-site browsing history, or a behavioural advertising profile.",
                                    "Payment or financial data. The hosted service does not take payments.",
                                ]}
                            />
                        </>
                    ),
                },
                {
                    heading: "Messages and encryption",
                    body: (
                        <>
                            <p>
                                Direct messages composed in the native Android, iOS, Windows, Linux and CLI
                                clients are end-to-end encrypted. The keys are generated and kept on your
                                device. The service stores an encrypted envelope plus the routing metadata it
                                needs to deliver the message: sender, recipient, conversation, timestamp,
                                delivery and read state, and the sending device. We cannot read the message
                                text, and we do not build a capability to do so.
                            </p>
                            <p className="mt-4">
                                End-to-end encryption covers the message text and the payload that references
                                an attachment. The attachment itself is stored in our media storage. Some
                                media buckets are publicly readable and others use signed links, so the file
                                is protected by access control rather than by the message encryption. Treat
                                any file you send as potentially shareable by anyone who obtains its link.
                            </p>
                            <p className="mt-4">
                                The web client does not implement the native clients’ end-to-end encryption.
                                Treat any message sent from the web as readable by the service, and use a
                                native client when you need the end-to-end guarantee. Posts and other content
                                are encrypted in transit but, like any hosted social content, are readable by
                                the service.
                            </p>
                            <p className="mt-4">
                                Encryption protects content, not the fact that two accounts exchanged
                                messages. Metadata such as who messaged whom and when remains visible to the
                                service.
                            </p>
                        </>
                    ),
                },
                {
                    heading: "What other people can see",
                    body: (
                        <>
                            <LegalList
                                items={[
                                    <>
                                        <strong className="text-foreground">Public profiles and posts.</strong>{" "}
                                        Your username, display name, avatar, bio and the profile fields you
                                        fill in are visible to anyone, and public profiles and posts may be
                                        indexed by search engines. A public email address you add is public.
                                    </>,
                                    <>
                                        <strong className="text-foreground">Private accounts.</strong> Posts
                                        are limited to approved followers, but your basic profile information
                                        and avatar remain visible so people can find and follow you.
                                    </>,
                                    <>
                                        <strong className="text-foreground">Messages.</strong> Visible to the
                                        participants in the conversation and, for encrypted messages, to the
                                        service only as ciphertext. Attachments are stored in our media
                                        storage; depending on the bucket, a file may be publicly readable or
                                        served through a signed link. Treat media you send as potentially
                                        public.
                                    </>,
                                    <>
                                        <strong className="text-foreground">Communities.</strong> Content you
                                        post in a space is visible to its members. Owners and moderators can
                                        see membership and moderation data for their space.
                                    </>,
                                ]}
                            />
                            <p>
                                We do not publish your email address, and we do not show your private
                                settings to other members.
                            </p>
                        </>
                    ),
                },
                {
                    heading: "How we use data and our legal bases",
                    body: (
                        <>
                            <LegalList
                                items={[
                                    "To create and operate your account, deliver your feed and messages, and keep your devices in sync (performance of our contract with you).",
                                    "To authenticate sessions, secure the service, prevent abuse and debug failures (our legitimate interest in running a safe service).",
                                    "To moderate content, enforce our terms and handle reports (legitimate interest and, where required, legal obligation).",
                                    "To send service messages such as email confirmation, password resets and security notices (contract and legitimate interest).",
                                    "To comply with valid legal requests and applicable law (legal obligation).",
                                    "To store optional profile fields, location attachments and diagnostics you choose to submit (your consent, which you can withdraw by removing the data).",
                                ]}
                            />
                            <p className="mt-4">
                                We do not sell personal data, share it with advertisers or data brokers, or use
                                it for cross-context behavioural advertising. If you are in the EEA, UK or
                                Switzerland, the bases above are the legal bases under the GDPR and UK GDPR.
                            </p>
                        </>
                    ),
                },
                {
                    heading: "Cookies and similar technologies",
                    body: (
                        <>
                            <p>
                                We set first-party cookies whose names begin with <code className="text-foreground">sb-</code>{" "}
                                to keep you signed in. They hold your session and refresh tokens and are
                                strictly necessary; the service cannot work without them. We do not set
                                analytics, advertising or cross-site tracking cookies.
                            </p>
                            <p className="mt-4">
                                Two third-party requests are worth calling out. Signing in with Google takes
                                you to Google’s pages, where Google may set its own cookies. Default avatars
                                are generated by DiceBear, which receives a seed derived from your username
                                and your IP address when the image is requested.
                            </p>
                            <p className="mt-4">
                                The full list, with durations and controls, is in the{" "}
                                <LegalLink href="/cookies">Cookie Policy</LegalLink>.
                            </p>
                        </>
                    ),
                },
                {
                    heading: "Service providers and sharing",
                    body: (
                        <>
                            <p>
                                We use a small number of providers to run the service. They process data on
                                our instructions and under their own security and data-processing terms.
                            </p>
                            <LegalTable
                                head={["Provider", "Purpose", "Data involved"]}
                                rows={[
                                    [
                                        "Supabase",
                                        "Authentication, database, file storage and account emails",
                                        "Account, profile, content, messages as stored, media and session data",
                                    ],
                                    [
                                        "Google",
                                        "Optional sign-in with Google",
                                        "Google account identifier and the profile Google shares",
                                    ],
                                    [
                                        "DiceBear",
                                        "Generating default initial avatars",
                                        "Username-derived seed and IP address",
                                    ],
                                    [
                                        "Hosting and CDN provider",
                                        "Serving the web app and API",
                                        "IP address and request metadata",
                                    ],
                                ]}
                            />
                            <p>We also disclose data in these limited situations:</p>
                            <LegalList
                                items={[
                                    <>
                                        <strong className="text-foreground">Legal requests.</strong> Where we
                                        are required to respond to a valid, binding request from an authority.
                                        For end-to-end encrypted messages, all we can produce is ciphertext and
                                        metadata. We notify affected users unless the law forbids it.
                                    </>,
                                    <>
                                        <strong className="text-foreground">Safety and security.</strong> When
                                        disclosure is necessary to address fraud, abuse, or an imminent threat
                                        to someone’s safety.
                                    </>,
                                    <>
                                        <strong className="text-foreground">A transfer of the service.</strong>{" "}
                                        If the hosted service changes hands, data may transfer with it. This
                                        policy travels with the data and users are notified.
                                    </>,
                                    <>
                                        <strong className="text-foreground">Other users.</strong> Only the
                                        content you choose to publish or send, as described above.
                                    </>,
                                ]}
                            />
                        </>
                    ),
                },
                {
                    heading: "International transfers",
                    body: (
                        <p>
                            Our providers may store or process data in countries other than yours, including
                            the United States. Where the law requires it, we rely on the providers’
                            safeguards, such as the European Commission’s standard contractual clauses and
                            the UK addendum. Contact us if you need details of the safeguards for a specific
                            provider.
                        </p>
                    ),
                },
                {
                    heading: "How long we keep data",
                    body: (
                        <>
                            <LegalList
                                items={[
                                    "Account, profile, content and membership data are kept while your account exists.",
                                    "When you delete your account, we remove your profile and content from live systems within 30 days and purge backups within 90 days, except where a legal hold or security investigation requires longer retention.",
                                    "Server logs are kept for up to 30 days. Submitted crash diagnostics are deleted or aggregated after 30 days.",
                                    "Push tokens are kept until the app unregisters the device or the token expires.",
                                    "Moderation and abuse records may be kept for as long as needed to resolve a report and meet legal obligations.",
                                    "Encrypted messages are kept until you or the participants delete them, subject to the retention window configured for the conversation. Because they are end-to-end encrypted, we cannot recover their contents.",
                                ]}
                            />
                            <p className="mt-4">
                                Deleted content may persist in other people’s copies or screenshots, in
                                backups until they rotate, and in records we must keep by law.
                            </p>
                        </>
                    ),
                },
                {
                    heading: "Security",
                    body: (
                        <>
                            <p>
                                Traffic is encrypted with TLS. Passwords are hashed and salted by our
                                authentication provider; we never store them in plain text. Database access is
                                governed by row-level security, media storage access is controlled per bucket
                                with public buckets used for media meant to be shareable and signed links for
                                private ones, and production access is limited to maintainers with a
                                documented reason and is logged.
                            </p>
                            <p className="mt-4">
                                Native clients provide end-to-end encryption for direct messages; the web
                                client does not. No system is completely secure. If we learn of a breach that
                                affects your data, we will notify you and the relevant authorities without
                                unreasonable delay, as the law requires.
                            </p>
                        </>
                    ),
                },
                {
                    heading: "Your rights and choices",
                    body: (
                        <>
                            <p>Wherever you live, you can:</p>
                            <LegalList
                                items={[
                                    "Access a copy of the personal data we hold about you.",
                                    "Correct your profile and account details from the profile editor.",
                                    "Delete your account and its content by contacting us. Native clients may also offer in-app deletion.",
                                    "Export your posts, profile and media by contacting us.",
                                    "Object to or request restriction of processing, and withdraw consent for optional data you provided.",
                                    "Complain to your local data protection authority.",
                                ]}
                            />
                            <p>
                                Email <SupportLink email={SUPPORT_EMAIL} /> to exercise a right. We may ask
                                you to verify that the account is yours, and we respond within 30 days. If
                                your data is held by a self-hosted deployment, contact that operator instead.
                            </p>
                            <p className="mt-4">
                                California residents: we do not sell personal information or share it for
                                cross-context behavioural advertising. You have the rights to know, delete and
                                correct personal information, to use an authorised agent, and not to be
                                discriminated against for exercising those rights.
                            </p>
                        </>
                    ),
                },
                {
                    heading: "Children",
                    body: (
                        <p>
                            The hosted service is not directed at children under 16, and we do not knowingly
                            collect their data. If you believe a child under 16 has created an account,
                            contact us and we will delete it. Operators of self-hosted deployments set their
                            own age requirements and moderation rules.
                        </p>
                    ),
                },
                {
                    heading: "Changes to this policy",
                    body: (
                        <p>
                            We update this policy when the service or the law changes. The date at the top of
                            the page is revised, and the change is recorded in the{" "}
                            <LegalLink href="/changelog">changelog</LegalLink>. Material changes are announced
                            by email to affected accounts and in the Synapse space before they take effect.
                        </p>
                    ),
                },
                {
                    heading: "Contact",
                    body: (
                        <p>
                            Questions, requests or complaints about privacy: write to{" "}
                            <SupportLink email={SUPPORT_EMAIL} />. If you need a data processing agreement
                            for a community you run on the hosted service, ask for one at the same address.
                            You also have the right to complain to your local data protection authority.
                        </p>
                    ),
                },
            ]}
            closing={
                <>
                    If you do not agree with this policy, do not use the hosted service. The software is
                    MIT licensed and yours to run elsewhere under your own policy.
                </>
            }
        />
    );
};

export default Privacy;
