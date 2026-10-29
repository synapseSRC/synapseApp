import { PlatformGlyph } from "@/components/marketing/platform-icons";
import { PLATFORMS } from "@/utils";
import Link from "next/link";

/** Replaces the old third-party logo strip with the five supported platforms. */
const PlatformStrip = () => {
    return (
        <div className="flex flex-col items-center gap-8 py-14">
            <h2 className="text-center text-sm font-medium uppercase tracking-wide text-neutral-400 font-heading">
                One account. Five ways in.
            </h2>
            <ul className="flex flex-wrap items-center justify-center gap-3 md:gap-4">
                {PLATFORMS.map((platform) => (
                    <li key={platform.id}>
                        <Link
                            href={`/download#${platform.id}`}
                            className="flex items-center gap-2.5 rounded-full border border-border/60 bg-foreground/[0.03] px-4 py-2 text-sm text-neutral-300 transition-colors duration-300 hover:border-border hover:bg-foreground/[0.06] hover:text-foreground"
                        >
                            <PlatformGlyph id={platform.id} className="size-7 border-0 bg-transparent" iconClassName="size-4" />
                            {platform.name}
                        </Link>
                    </li>
                ))}
            </ul>
        </div>
    );
};

export default PlatformStrip;
