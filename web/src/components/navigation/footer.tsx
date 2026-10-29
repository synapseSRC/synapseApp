import AnimationContainer from "@/components/global/animation-container";
import { Icons } from "@/components/global/icons";
import { TextHoverEffect } from "@/components/ui/text-hover-effect"
import { APP_NAME, PLATFORMS } from "@/utils";
import { PROJECT_LINKS } from "@/utils/constants/site";
import Link from 'next/link';

const FEATURE_LINKS = [
    { label: "Messaging", href: "/features/messaging" },
    { label: "Communities", href: "/features/communities" },
    { label: "Sync", href: "/features/sync" },
    { label: "API & CLI", href: "/features/api" },
];

const RESOURCE_LINKS = [
    { label: "Blog", href: "/resources/blog" },
    { label: "Help", href: "/resources/help" },
    { label: "Changelog", href: "/changelog" },
];

const PROJECT_LINKS_LIST = [
    { label: "GitHub", href: PROJECT_LINKS.github },
    { label: "Open Source", href: "/open-source" },
    { label: "Privacy Policy", href: "/privacy" },
    { label: "Terms", href: "/terms" },
];

const Column = ({
    title,
    links,
    delay,
}: {
    title: string;
    links: { label: string; href: string }[];
    delay: number;
}) => (
    <AnimationContainer delay={delay}>
        <div className="flex flex-col">
            <h3 className="text-base font-medium text-white">{title}</h3>
            <ul className="mt-4 text-sm text-muted-foreground space-y-2">
                {links.map((link) => {
                    const external = /^https?:/.test(link.href);
                    return (
                        <li key={link.label}>
                            <Link
                                href={link.href}
                                target={external ? "_blank" : undefined}
                                rel={external ? "noreferrer noopener" : undefined}
                                className="hover:text-foreground transition-all duration-300"
                            >
                                {link.label}
                            </Link>
                        </li>
                    );
                })}
            </ul>
        </div>
    </AnimationContainer>
);

const Footer = () => {
    return (
        <footer className="flex flex-col relative items-center justify-center border-t border-border pt-16 pb-8 md:pb-0 px-6 lg:px-8 w-full max-w-6xl mx-auto lg:pt-32 bg-[radial-gradient(35%_128px_at_50%_0%,theme(backgroundColor.white/8%),transparent)]">

            <div className="absolute top-0 left-1/2 right-1/2 -translate-x-1/2 -translate-y-1/2 w-8 h-1.5 bg-foreground rounded-full"></div>

            <AnimationContainer delay={0.1}>
                <div className="flex w-full flex-col items-start justify-between gap-8 lg:flex-row">
                    <div className="flex flex-col items-start justify-start md:max-w-[260px]">
                        <Link href="/" className="flex items-center gap-2">
                            <Icons.mark className="w-5 h-5 text-foreground" />
                            <span className="text-lg font-bold font-heading !leading-none">{APP_NAME}</span>
                        </Link>
                        <p className="text-muted-foreground mt-4 text-sm text-start">
                            Open-source social, everywhere. Your people, your data, your clients.
                        </p>
                        <span className="mt-4 flex items-center gap-2 rounded-full border border-border/70 px-3 py-1 text-xs text-muted-foreground">
                            <Icons.github className="size-3.5" />
                            MIT licensed
                        </span>
                    </div>

                    <div className="grid w-full grid-cols-2 gap-8 sm:grid-cols-4 lg:w-auto lg:gap-12">
                        <Column
                            title="Platforms"
                            delay={0.2}
                            links={PLATFORMS.map((platform) => ({
                                label: platform.name,
                                href: `/download#${platform.id}`,
                            }))}
                        />
                        <Column title="Features" delay={0.3} links={FEATURE_LINKS} />
                        <Column title="Resources" delay={0.4} links={RESOURCE_LINKS} />
                        <Column title="Project" delay={0.5} links={PROJECT_LINKS_LIST} />
                    </div>
                </div>
            </AnimationContainer>

            <div className="mt-12 border-t border-border/40 pt-8 w-full md:flex md:items-center md:justify-between">
                <AnimationContainer delay={0.6}>
                    <p className="text-sm text-muted-foreground">
                        &copy; {new Date().getFullYear()} {APP_NAME} contributors &middot; MIT licensed
                    </p>
                </AnimationContainer>
                <AnimationContainer delay={0.7}>
                    <p className="mt-3 text-sm text-muted-foreground md:mt-0">
                        Made with an open roadmap —{" "}
                        <Link href="/changelog" className="text-foreground hover:text-cyan-300 transition-colors duration-300">
                            see what shipped
                        </Link>
                    </p>
                </AnimationContainer>
            </div>

            <div className="h-[20rem] hidden md:flex items-center justify-center w-full">
                <TextHoverEffect text="SYNAPSE" />
            </div>
        </footer>
    )
}

export default Footer
