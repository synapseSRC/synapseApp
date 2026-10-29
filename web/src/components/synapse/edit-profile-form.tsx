"use client";

import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState, type ChangeEvent, type FormEvent } from "react";
import { ImagePlusIcon, Loader2Icon } from "lucide-react";
import { toast } from "sonner";

import { buttonVariants } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { Textarea } from "@/components/ui/textarea";
import { getSupabaseBrowserClient } from "@/lib/supabase/client";
import { normalizeWebsite, PROFILE_LIMITS, type ProfileFormValues } from "@/lib/synapse/profile-fields";
import { uploadProfileImage, validateProfileImage } from "@/lib/synapse/profile-upload";

const panel = "rounded-2xl border border-neutral-800 bg-neutral-950/60 p-5 backdrop-blur-xl";
const field = "h-11 rounded-lg border-neutral-800 bg-neutral-900/60 focus-visible:ring-2 focus-visible:ring-violet-500/40";
const textarea = "min-h-[110px] resize-y rounded-lg border-neutral-800 bg-neutral-900/60 focus-visible:ring-2 focus-visible:ring-violet-500/40";

type ImageKind = "avatar" | "cover";

export const EditProfileForm = ({ initial, coverUrl }: { initial: ProfileFormValues; coverUrl: string | null }) => {
    const router = useRouter();
    const [pending, setPending] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [isPrivate, setIsPrivate] = useState(initial.is_private);
    const [bio, setBio] = useState(initial.bio);
    const [avatarFile, setAvatarFile] = useState<File | null>(null);
    const [coverFile, setCoverFile] = useState<File | null>(null);
    const [avatarPreview, setAvatarPreview] = useState<string | null>(initial.avatar);
    const [coverPreview, setCoverPreview] = useState<string | null>(coverUrl);
    const avatarInput = useRef<HTMLInputElement>(null);
    const coverInput = useRef<HTMLInputElement>(null);
    const objectUrls = useRef<Record<ImageKind, string | null>>({ avatar: null, cover: null });

    useEffect(
        () => () => {
            for (const url of Object.values(objectUrls.current)) {
                if (url) URL.revokeObjectURL(url);
            }
        },
        [],
    );

    const pickImage = (kind: ImageKind, file: File | undefined) => {
        if (!file) return;

        const problem = validateProfileImage(file);

        if (problem) {
            setError(problem);
            return;
        }

        setError(null);

        const previous = objectUrls.current[kind];
        if (previous) URL.revokeObjectURL(previous);

        const url = URL.createObjectURL(file);
        objectUrls.current[kind] = url;

        if (kind === "avatar") {
            setAvatarFile(file);
            setAvatarPreview(url);
        } else {
            setCoverFile(file);
            setCoverPreview(url);
        }
    };

    const submitForm = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        if (pending) return;
        setError(null);

        const data = new FormData(event.currentTarget);
        const text = (key: string) => String(data.get(key) ?? "").trim();

        const displayName = text("display_name");
        const location = text("location");
        const pronouns = text("pronouns");
        const work = text("work");
        const education = text("education");
        const relationship = text("relationship_status");

        const checks: [string, string, number][] = [
            ["Display name", displayName, PROFILE_LIMITS.displayName],
            ["Bio", bio.trim(), PROFILE_LIMITS.bio],
            ["Location", location, PROFILE_LIMITS.location],
            ["Pronouns", pronouns, PROFILE_LIMITS.pronouns],
            ["Work", work, PROFILE_LIMITS.work],
            ["Education", education, PROFILE_LIMITS.education],
            ["Relationship status", relationship, PROFILE_LIMITS.relationship],
        ];
        const overLimit = checks.find(([, value, limit]) => value.length > limit);

        if (overLimit) {
            setError(`${overLimit[0]} must be ${overLimit[2]} characters or fewer.`);
            return;
        }

        const website = normalizeWebsite(text("website"));

        if (website.error) {
            setError(website.error);
            return;
        }

        for (const file of [avatarFile, coverFile]) {
            if (!file) continue;
            const problem = validateProfileImage(file);
            if (problem) {
                setError(problem);
                return;
            }
        }

        setPending(true);

        const supabase = getSupabaseBrowserClient();
        const { data: auth, error: authError } = await supabase.auth.getUser();

        if (authError || !auth.user) {
            setError("Your session expired. Sign in again and retry.");
            setPending(false);
            return;
        }

        let avatarUrl: string | null = null;
        let coverUrl: string | null = null;

        try {
            if (avatarFile) avatarUrl = await uploadProfileImage(supabase, auth.user.id, "avatar", avatarFile);
            if (coverFile) coverUrl = await uploadProfileImage(supabase, auth.user.id, "cover", coverFile);
        } catch (uploadError) {
            setError(uploadError instanceof Error ? uploadError.message : "Image upload failed.");
            setPending(false);
            return;
        }

        const patch: Record<string, unknown> = {
            display_name: displayName || null,
            bio: bio.trim() || null,
            website: website.value,
            location: location || null,
            pronouns: pronouns || null,
            work: work || null,
            education: education || null,
            relationship_status: relationship || null,
            is_private: isPrivate,
            updated_at: new Date().toISOString(),
        };

        if (avatarUrl) patch.avatar = avatarUrl;
        if (coverUrl) patch.profile_cover_image = coverUrl;

        const { data: updated, error: updateError } = await supabase
            .from("users")
            .update(patch)
            .eq("uid", auth.user.id)
            .select("uid");

        if (updateError) {
            setError(`Could not save your profile: ${updateError.message}`);
            setPending(false);
            return;
        }

        if (!updated || updated.length === 0) {
            setError("Your profile could not be updated. Sign out, sign in again, and retry.");
            setPending(false);
            return;
        }

        if (avatarUrl) {
            const { error: metadataError } = await supabase.auth.updateUser({ data: { avatar_url: avatarUrl } });

            if (metadataError) {
                toast.warning("Profile saved, but avatar metadata was not updated.", { description: metadataError.message });
            }
        }

        toast.success("Profile updated");
        router.replace("/profile");
        router.refresh();
    };

    return (
        <form onSubmit={submitForm} className="space-y-4">
            {error ? (
                <p role="alert" className="rounded-lg border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-200">
                    {error}
                </p>
            ) : null}

            <section className={panel}>
                <div className="flex flex-wrap items-center justify-between gap-2">
                    <h2 className="font-heading text-sm font-semibold">Profile photos</h2>
                    <span className="text-xs text-neutral-500">JPEG, PNG, WebP or GIF · 5 MB max</span>
                </div>

                <div className="relative mt-4 h-28 overflow-hidden rounded-xl border border-neutral-800">
                    {coverPreview ? (
                        <Image src={coverPreview} alt="" fill unoptimized className="object-cover" />
                    ) : (
                        <div className="absolute inset-0 grid place-items-center bg-gradient-to-r from-violet-700/60 via-neutral-900 to-cyan-600/50 text-xs text-neutral-400">
                            No cover image
                        </div>
                    )}
                </div>

                <div className="mt-4 flex flex-wrap items-end justify-between gap-4">
                    <div className="flex items-center gap-3">
                        {avatarPreview ? (
                            <Image
                                src={avatarPreview}
                                alt=""
                                width={64}
                                height={64}
                                unoptimized
                                className="size-16 rounded-full border border-neutral-800 object-cover"
                            />
                        ) : (
                            <span className="grid size-16 place-items-center rounded-full bg-gradient-to-r from-violet-600 to-cyan-500 text-lg font-semibold text-white">
                                {(initial.display_name || initial.username || "S").slice(0, 1).toUpperCase()}
                            </span>
                        )}
                        <div>
                            <p className="text-sm font-medium">Avatar</p>
                            <p className="text-xs text-neutral-500">Shown next to your posts.</p>
                        </div>
                    </div>

                    <div className="flex flex-wrap gap-2">
                        <button
                            type="button"
                            onClick={() => avatarInput.current?.click()}
                            className={buttonVariants({ variant: "outline", size: "sm", className: "gap-2" })}
                        >
                            <ImagePlusIcon className="size-4" />
                            Change avatar
                        </button>
                        <button
                            type="button"
                            onClick={() => coverInput.current?.click()}
                            className={buttonVariants({ variant: "outline", size: "sm", className: "gap-2" })}
                        >
                            <ImagePlusIcon className="size-4" />
                            Change cover
                        </button>
                    </div>
                </div>
                <p className="mt-3 text-xs text-neutral-500">Images upload when you save.</p>

                <input
                    ref={avatarInput}
                    type="file"
                    accept="image/jpeg,image/png,image/webp,image/gif"
                    className="sr-only"
                    onChange={(event: ChangeEvent<HTMLInputElement>) => pickImage("avatar", event.target.files?.[0])}
                />
                <input
                    ref={coverInput}
                    type="file"
                    accept="image/jpeg,image/png,image/webp,image/gif"
                    className="sr-only"
                    onChange={(event: ChangeEvent<HTMLInputElement>) => pickImage("cover", event.target.files?.[0])}
                />
            </section>

            <section className={panel}>
                <h2 className="font-heading text-sm font-semibold">About you</h2>
                <p className="mt-1 text-xs text-neutral-500">Username @{initial.username} can&apos;t be changed.</p>

                <div className="mt-4 space-y-4">
                    <div className="space-y-2">
                        <Label htmlFor="display_name">Display name</Label>
                        <Input
                            id="display_name"
                            name="display_name"
                            defaultValue={initial.display_name}
                            maxLength={PROFILE_LIMITS.displayName}
                            placeholder="How your name appears"
                            className={field}
                        />
                    </div>

                    <div className="space-y-2">
                        <div className="flex items-center justify-between">
                            <Label htmlFor="bio">Bio</Label>
                            <span className="text-xs text-neutral-500">
                                {bio.length}/{PROFILE_LIMITS.bio}
                            </span>
                        </div>
                        <Textarea
                            id="bio"
                            name="bio"
                            value={bio}
                            onChange={(event) => setBio(event.target.value)}
                            maxLength={PROFILE_LIMITS.bio}
                            rows={4}
                            placeholder="A short intro"
                            className={textarea}
                        />
                    </div>

                    <div className="grid gap-4 sm:grid-cols-2">
                        <div className="space-y-2">
                            <Label htmlFor="website">Website</Label>
                            <Input
                                id="website"
                                name="website"
                                inputMode="url"
                                defaultValue={initial.website}
                                maxLength={PROFILE_LIMITS.website}
                                placeholder="https://example.com"
                                className={field}
                            />
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="location">Location</Label>
                            <Input
                                id="location"
                                name="location"
                                defaultValue={initial.location}
                                maxLength={PROFILE_LIMITS.location}
                                placeholder="City, country"
                                className={field}
                            />
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="pronouns">Pronouns</Label>
                            <Input
                                id="pronouns"
                                name="pronouns"
                                defaultValue={initial.pronouns}
                                maxLength={PROFILE_LIMITS.pronouns}
                                placeholder="e.g. they/them"
                                className={field}
                            />
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="relationship_status">Relationship status</Label>
                            <Input
                                id="relationship_status"
                                name="relationship_status"
                                defaultValue={initial.relationship_status}
                                maxLength={PROFILE_LIMITS.relationship}
                                placeholder="Optional"
                                className={field}
                            />
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="work">Work</Label>
                            <Input
                                id="work"
                                name="work"
                                defaultValue={initial.work}
                                maxLength={PROFILE_LIMITS.work}
                                placeholder="Company or role"
                                className={field}
                            />
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="education">Education</Label>
                            <Input
                                id="education"
                                name="education"
                                defaultValue={initial.education}
                                maxLength={PROFILE_LIMITS.education}
                                placeholder="School or program"
                                className={field}
                            />
                        </div>
                    </div>
                </div>
            </section>

            <section className={panel}>
                <div className="flex items-start justify-between gap-4">
                    <div>
                        <h2 className="font-heading text-sm font-semibold">Private account</h2>
                        <p className="mt-1 max-w-md text-xs leading-relaxed text-neutral-400">
                            Adds a private badge and hides your cover image from your public profile. Post visibility keeps
                            following your existing privacy rules.
                        </p>
                    </div>
                    <Switch id="is_private" checked={isPrivate} onCheckedChange={setIsPrivate} />
                </div>
            </section>

            <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
                <Link
                    href="/profile"
                    className={buttonVariants({ variant: "outline", size: "lg", className: "rounded-lg" })}
                >
                    Cancel
                </Link>
                <button
                    type="submit"
                    disabled={pending}
                    className={buttonVariants({ variant: "accent", size: "lg", className: "gap-2 rounded-lg" })}
                >
                    {pending ? <Loader2Icon className="size-4 animate-spin" /> : null}
                    {pending ? "Saving…" : "Save changes"}
                </button>
            </div>
        </form>
    );
};
