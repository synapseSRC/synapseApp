import { PlatformId } from "@/utils/constants/platforms";
import { cn } from "@/utils";
import {
    AppWindowIcon,
    LucideIcon,
    LaptopIcon,
    SmartphoneIcon,
    TerminalIcon,
} from "lucide-react";

type PlatformGlyphConfig = {
    icon: LucideIcon;
    ring: string;
    text: string;
};

const GLYPHS: Record<PlatformId, PlatformGlyphConfig> = {
    android: { icon: SmartphoneIcon, ring: "border-emerald-400/30", text: "text-emerald-300" },
    ios: { icon: SmartphoneIcon, ring: "border-sky-400/30", text: "text-sky-300" },
    windows: { icon: AppWindowIcon, ring: "border-cyan-400/30", text: "text-cyan-300" },
    linux: { icon: LaptopIcon, ring: "border-amber-400/30", text: "text-amber-300" },
    cli: { icon: TerminalIcon, ring: "border-violet-400/30", text: "text-violet-300" },
};

export const PlatformGlyph = ({
    id,
    className,
    iconClassName,
}: {
    id: PlatformId;
    className?: string;
    iconClassName?: string;
}) => {
    const { icon: Icon, ring, text } = GLYPHS[id];

    return (
        <span
            className={cn(
                "flex size-11 shrink-0 items-center justify-center rounded-xl border bg-foreground/[0.04]",
                ring,
                className,
            )}
        >
            <Icon className={cn("size-5", text, iconClassName)} strokeWidth={1.75} />
        </span>
    );
};
