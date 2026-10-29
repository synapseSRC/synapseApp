"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, type FormEvent, type ReactNode } from "react";
import { Loader2Icon } from "lucide-react";
import { toast } from "sonner";

import { buttonVariants } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { getSupabaseBrowserClient } from "@/lib/supabase/client";
import {
    describeAuthError,
    ensureProfileRow,
    initialsAvatarUrl,
    normalizeUsername,
} from "@/lib/synapse/auth";

const GoogleGlyph = () => (
    <svg viewBox="0 0 24 24" className="size-4" aria-hidden="true">
        <path
            fill="currentColor"
            d="M23.52 12.27c0-.85-.08-1.67-.22-2.45H12v4.63h6.46a5.53 5.53 0 0 1-2.4 3.62v3h3.88c2.27-2.09 3.58-5.17 3.58-8.8Z"
        />
        <path
            fill="currentColor"
            d="M12 24c3.24 0 5.96-1.07 7.94-2.93l-3.88-3a7.24 7.24 0 0 1-4.06 1.15 7.3 7.3 0 0 1-6.83-4.8H3.11v3.09A11.99 11.99 0 0 0 12 24Z"
            opacity=".6"
        />
        <path fill="currentColor" d="M5.17 14.42a7.35 7.35 0 0 1 0-4.84V6.49H3.11a12.02 12.02 0 0 0 0 10.82l2.06-2.89Z" opacity=".8" />
        <path
            fill="currentColor"
            d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.2 15.23 0 12 0 7.31 0 3.2 2.69 3.11 6.49l2.06 3.09A7.3 7.3 0 0 1 12 4.75Z"
        />
    </svg>
);

const shell = "w-full max-w-md rounded-2xl border border-neutral-800 bg-neutral-950/60 p-8 backdrop-blur-xl";
const field = "h-11 rounded-lg border-neutral-800 bg-neutral-900/60 focus-visible:ring-2 focus-visible:ring-violet-500/40";
const submit = buttonVariants({ variant: "accent", size: "lg", className: "w-full gap-2 rounded-lg" });

const Notice = ({ children }: { children: ReactNode }) => (
    <p className="rounded-lg border border-violet-500/30 bg-violet-500/10 px-4 py-3 text-sm text-violet-200">{children}</p>
);

export const Failure = ({ message }: { message: string }) => (
    <p role="alert" className="rounded-lg border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-200">
        {message}
    </p>
);

export const AuthCard = ({
    title,
    subtitle,
    children,
}: {
    title: string;
    subtitle: string;
    children: ReactNode;
}) => (
    <div className={shell}>
        <h1 className="font-heading text-3xl font-semibold tracking-tight">{title}</h1>
        <p className="mt-2 text-sm text-neutral-400">{subtitle}</p>
        <div className="mt-8 space-y-4">{children}</div>
    </div>
);

const signInWithGoogle = async (next: string) => {
    const supabase = getSupabaseBrowserClient();
    const { error } = await supabase.auth.signInWithOAuth({
        provider: "google",
        options: { redirectTo: `${window.location.origin}/auth/callback?next=${encodeURIComponent(next)}` },
    });
    if (error) throw error;
};

export const OAuthDivider = ({ next }: { next?: string }) => {
    const [pending, setPending] = useState(false);

    const handle = async () => {
        setPending(true);
        try {
            await signInWithGoogle(next ?? "/home");
        } catch (error) {
            setPending(false);
            toast.error(describeAuthError(error));
        }
    };

    return (
        <>
            <div className="flex items-center gap-3 py-1">
                <span className="h-px flex-1 bg-neutral-800" />
                <span className="text-xs uppercase tracking-widest text-neutral-500">or</span>
                <span className="h-px flex-1 bg-neutral-800" />
            </div>
            <button
                type="button"
                onClick={handle}
                disabled={pending}
                className={buttonVariants({ variant: "outline", size: "lg", className: "w-full gap-2 rounded-lg" })}
            >
                {pending ? <Loader2Icon className="size-4 animate-spin" /> : <GoogleGlyph />}
                Continue with Google
            </button>
        </>
    );
};

export const LoginForm = ({ next = "/home" }: { next?: string }) => {
    const router = useRouter();
    const [pending, setPending] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const submitForm = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        const data = new FormData(event.currentTarget);
        const email = String(data.get("email") ?? "").trim();
        const password = String(data.get("password") ?? "");

        setPending(true);
        setError(null);

        const supabase = getSupabaseBrowserClient();
        const { data: session, error: signInError } = await supabase.auth.signInWithPassword({ email, password });

        if (signInError || !session.user) {
            setError(describeAuthError(signInError ?? new Error("No session returned.")));
            setPending(false);
            return;
        }

        const profile = await ensureProfileRow(supabase, session.user);
        if (!profile.ok) toast.warning("Signed in, but your profile row could not be created.", { description: profile.message });

        router.replace(next);
        router.refresh();
    };

    return (
        <form onSubmit={submitForm} className="space-y-4">
            {error ? <Failure message={error} /> : null}
            <div className="space-y-2">
                <Label htmlFor="email">Email</Label>
                <Input id="email" name="email" type="email" required autoComplete="email" placeholder="you@example.com" className={field} />
            </div>
            <div className="space-y-2">
                <div className="flex items-center justify-between">
                    <Label htmlFor="password">Password</Label>
                    <Link href="/forgot-password" className="text-xs text-neutral-400 hover:text-white">
                        Forgot password?
                    </Link>
                </div>
                <Input id="password" name="password" type="password" required autoComplete="current-password" className={field} />
            </div>
            <button type="submit" disabled={pending} className={submit}>
                {pending ? <Loader2Icon className="size-4 animate-spin" /> : null}
                Sign in
            </button>
            <OAuthDivider next={next} />
            <p className="pt-2 text-sm text-neutral-400">
                New to Synapse?{" "}
                <Link href="/signup" className="text-white underline underline-offset-4">
                    Create an account
                </Link>
            </p>
        </form>
    );
};

export const SignUpForm = ({ next = "/home" }: { next?: string }) => {
    const router = useRouter();
    const [pending, setPending] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [notice, setNotice] = useState<string | null>(null);

    const submitForm = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        const data = new FormData(event.currentTarget);
        const email = String(data.get("email") ?? "").trim();
        const password = String(data.get("password") ?? "");
        const username = normalizeUsername(String(data.get("username") ?? ""));

        setPending(true);
        setError(null);
        setNotice(null);

        const supabase = getSupabaseBrowserClient();
        const { data: result, error: signUpError } = await supabase.auth.signUp({
            email,
            password,
            options: {
                data: { username, avatar_url: initialsAvatarUrl(username) },
                emailRedirectTo: `${window.location.origin}/auth/callback?next=${encodeURIComponent(next)}`,
            },
        });

        if (signUpError) {
            setError(describeAuthError(signUpError));
            setPending(false);
            return;
        }

        if (result.user && result.session) {
            const profile = await ensureProfileRow(supabase, result.user);
            if (!profile.ok) {
                toast.warning("Account created, but your profile row could not be saved.", { description: profile.message });
            }
            router.replace(next);
            router.refresh();
            return;
        }

        setNotice(`Check your inbox for a confirmation link sent to ${email}.`);
        setPending(false);
    };

    return (
        <form onSubmit={submitForm} className="space-y-4">
            {error ? <Failure message={error} /> : null}
            {notice ? <Notice>{notice}</Notice> : null}
            <div className="space-y-2">
                <Label htmlFor="username">Username</Label>
                <Input
                    id="username"
                    name="username"
                    required
                    minLength={3}
                    maxLength={30}
                    pattern="[a-z0-9._-]+"
                    autoComplete="username"
                    placeholder="ashik"
                    className={field}
                />
                <p className="text-xs text-neutral-500">Lowercase letters, numbers, dots, dashes and underscores.</p>
            </div>
            <div className="space-y-2">
                <Label htmlFor="email">Email</Label>
                <Input id="email" name="email" type="email" required autoComplete="email" placeholder="you@example.com" className={field} />
            </div>
            <div className="space-y-2">
                <Label htmlFor="password">Password</Label>
                <Input id="password" name="password" type="password" required minLength={8} autoComplete="new-password" className={field} />
            </div>
            <button type="submit" disabled={pending} className={submit}>
                {pending ? <Loader2Icon className="size-4 animate-spin" /> : null}
                Create account
            </button>
            <OAuthDivider next={next} />
            <p className="pt-2 text-sm text-neutral-400">
                Already have an account?{" "}
                <Link href="/login" className="text-white underline underline-offset-4">
                    Sign in
                </Link>
            </p>
        </form>
    );
};

export const ForgotPasswordForm = () => {
    const [pending, setPending] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [sent, setSent] = useState(false);

    const submitForm = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        const email = String(new FormData(event.currentTarget).get("email") ?? "").trim();

        setPending(true);
        setError(null);

        const { error: resetError } = await getSupabaseBrowserClient().auth.resetPasswordForEmail(email, {
            redirectTo: `${window.location.origin}/auth/callback?type=recovery`,
        });

        setPending(false);

        if (resetError) {
            setError(describeAuthError(resetError));
            return;
        }

        setSent(true);
    };

    if (sent) {
        return (
            <div className="space-y-4">
                <Notice>If an account exists for that address, a reset link is on its way.</Notice>
                <Link href="/login" className={buttonVariants({ variant: "outline", size: "lg", className: "w-full rounded-lg" })}>
                    Back to sign in
                </Link>
            </div>
        );
    }

    return (
        <form onSubmit={submitForm} className="space-y-4">
            {error ? <Failure message={error} /> : null}
            <div className="space-y-2">
                <Label htmlFor="email">Email</Label>
                <Input id="email" name="email" type="email" required autoComplete="email" placeholder="you@example.com" className={field} />
            </div>
            <button type="submit" disabled={pending} className={submit}>
                {pending ? <Loader2Icon className="size-4 animate-spin" /> : null}
                Send reset link
            </button>
            <p className="pt-2 text-sm text-neutral-400">
                Remembered it?{" "}
                <Link href="/login" className="text-white underline underline-offset-4">
                    Sign in
                </Link>
            </p>
        </form>
    );
};

export const ResetPasswordForm = () => {
    const router = useRouter();
    const [pending, setPending] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const submitForm = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        const data = new FormData(event.currentTarget);
        const password = String(data.get("password") ?? "");
        const confirm = String(data.get("confirm") ?? "");

        if (password !== confirm) {
            setError("Both fields must match.");
            return;
        }

        setPending(true);
        setError(null);

        const supabase = getSupabaseBrowserClient();
        const { data: result, error: updateError } = await supabase.auth.updateUser({ password });

        if (updateError || !result.user) {
            setError(describeAuthError(updateError ?? new Error("No session returned.")));
            setPending(false);
            return;
        }

        await ensureProfileRow(supabase, result.user);
        router.replace("/home");
        router.refresh();
    };

    return (
        <form onSubmit={submitForm} className="space-y-4">
            {error ? <Failure message={error} /> : null}
            <div className="space-y-2">
                <Label htmlFor="password">New password</Label>
                <Input id="password" name="password" type="password" required minLength={8} autoComplete="new-password" className={field} />
            </div>
            <div className="space-y-2">
                <Label htmlFor="confirm">Confirm new password</Label>
                <Input id="confirm" name="confirm" type="password" required minLength={8} autoComplete="new-password" className={field} />
            </div>
            <button type="submit" disabled={pending} className={submit}>
                {pending ? <Loader2Icon className="size-4 animate-spin" /> : null}
                Update password
            </button>
        </form>
    );
};
