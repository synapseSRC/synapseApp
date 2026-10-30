import LegalLayout, { LegalLink, LegalList, LegalTable, SupportLink } from "@/components/marketing/legal-layout";
import { generateMetadata } from "@/utils";
import { SUPPORT_EMAIL } from "@/utils/constants/site";

export const metadata = generateMetadata({
    title: "Cookie Policy — Synapse",
    description:
        "The strictly necessary cookies Synapse uses to keep you signed in, the third-party requests involved in Google sign-in and default avatars, and how to control them.",
});

const Cookies = () => {
    return (
        <LegalLayout
            title="Cookie Policy"
            updated="Last updated: 31 October 2026"
            intro={
                <>
                    This policy explains how the Synapse web app uses cookies and similar technologies. It
                    is short because the answer is simple: we use a small set of strictly necessary
                    cookies to keep you signed in, and nothing else. There is no analytics, advertising or
                    cross-site tracking on the hosted service.
                </>
            }
            sections={[
                {
                    heading: "Our approach",
                    body: (
                        <>
                            <p>
                                A cookie is a small file a site stores in your browser. Cookies can identify
                                a returning browser, which is useful for sign-in and harmful when used for
                                tracking. We only use them for the useful part.
                            </p>
                            <p className="mt-4">
                                The cookies listed below are strictly necessary to provide a service you
                                asked for, so they are set when you sign in without a consent prompt. We do
                                not use optional cookies, so there is no cookie banner and no preference to
                                manage. If we ever add optional cookies, we will ask for your consent first
                                and update this page.
                            </p>
                        </>
                    ),
                },
                {
                    heading: "Cookies we set",
                    body: (
                        <>
                            <p>
                                Authentication is handled by Supabase, and the cookies are set on
                                synapse.app as first-party cookies. Their names include the Supabase
                                project reference and begin with{" "}
                                <code className="rounded bg-muted/60 px-1.5 py-0.5 text-foreground">sb-</code>.
                            </p>
                            <LegalTable
                                head={["Cookie", "Purpose", "Duration"]}
                                rows={[
                                    [
                                        <code key="auth" className="text-foreground">
                                            sb-&lt;project-ref&gt;-auth-token
                                        </code>,
                                        "Stores your session and refresh token so you stay signed in. Large sessions may be split across numbered chunks such as .0 and .1.",
                                        "Until you sign out, the refresh token expires, or you delete the cookie.",
                                    ],
                                    [
                                        <code key="pkce" className="text-foreground">
                                            sb-&lt;project-ref&gt;-auth-token-code-verifier
                                        </code>,
                                        "Completes the PKCE sign-in flow used by email and Google authentication. It prevents an intercepted login link from being replayed.",
                                        "A few minutes, until the sign-in round trip finishes.",
                                    ],
                                ]}
                            />
                            <p>
                                Both cookies are strictly necessary. Blocking them disables sign-in; deleting
                                them signs you out.
                            </p>
                        </>
                    ),
                },
                {
                    heading: "Local storage and similar technologies",
                    body: (
                        <p>
                            The web app does not use localStorage, sessionStorage or IndexedDB for tracking.
                            Sign-in state lives in the cookies above. The hosted service sets no advertising
                            identifiers and no cross-site pixels. In the native apps, session tokens are kept
                            in the operating system’s secure storage and are not browser cookies.
                        </p>
                    ),
                },
                {
                    heading: "Third-party requests",
                    body: (
                        <>
                            <p>
                                Some features load from another company. These are requests, not our cookies,
                                and the third party’s own policy applies to what it receives:
                            </p>
                            <LegalList
                                items={[
                                    <>
                                        <strong className="text-foreground">Google sign-in.</strong> If you
                                        choose “Continue with Google”, you are taken to Google’s
                                        authentication pages. Google may set cookies on its own domain during
                                        that flow; those are controlled by Google’s privacy policy.
                                    </>,
                                    <>
                                        <strong className="text-foreground">DiceBear avatars.</strong> When a
                                        default initial avatar is displayed, your browser requests it from
                                        api.dicebear.com. The request includes a seed derived from the
                                        username and your IP address. DiceBear does not receive your password,
                                        email address or messages. Uploading your own avatar replaces the
                                        generated one.
                                    </>,
                                    <>
                                        <strong className="text-foreground">Supabase API and storage.</strong>{" "}
                                        Your browser requests your project’s Supabase endpoints to read and
                                        write data and media. Those requests carry your session and may be
                                        logged by the provider for security and operations.
                                    </>,
                                ]}
                            />
                            <p>
                                We do not embed social media widgets, advertising networks or third-party
                                analytics scripts.
                            </p>
                        </>
                    ),
                },
                {
                    heading: "Managing cookies",
                    body: (
                        <>
                            <p>
                                You can view, block and delete cookies in your browser settings. Clearing
                                cookies signs you out of Synapse and any other site you use in that browser.
                                Blocking the <code className="rounded bg-muted/60 px-1.5 py-0.5 text-foreground">sb-</code>{" "}
                                cookies prevents sign-in entirely, because the service has no other way to
                                keep you authenticated.
                            </p>
                            <p className="mt-4">
                                Because we do not set optional cookies, there is nothing else to opt out of.
                                If you use a native client, sign out from the app to revoke the session stored
                                on that device.
                            </p>
                        </>
                    ),
                },
                {
                    heading: "Do Not Track and Global Privacy Control",
                    body: (
                        <p>
                            We do not track you across websites, so there is no cross-site tracking to
                            disable. We do not sell personal data or share it for behavioural advertising,
                            which means there is no sale or sharing to opt out of through Global Privacy
                            Control signals.
                        </p>
                    ),
                },
                {
                    heading: "Changes to this policy",
                    body: (
                        <p>
                            We update this page when the cookies or third-party requests change. The date at
                            the top is revised and the change is recorded in the{" "}
                            <LegalLink href="/changelog">changelog</LegalLink>. If we introduce optional
                            cookies, we will ask for consent before setting them.
                        </p>
                    ),
                },
                {
                    heading: "Contact",
                    body: (
                        <p>
                            Questions about cookies or this policy: <SupportLink email={SUPPORT_EMAIL} />.
                            For a broader description of what we collect, see the{" "}
                            <LegalLink href="/privacy">Privacy Policy</LegalLink>.
                        </p>
                    ),
                },
            ]}
            closing={
                <>
                    By using the hosted service you agree to this policy. If you would rather not receive
                    any cookies, use a browser setting to block them — but you will not be able to sign in.
                </>
            }
        />
    );
};

export default Cookies;
