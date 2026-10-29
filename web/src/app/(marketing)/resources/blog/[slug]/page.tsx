import { formatBlogDate } from "@/components/blog/blogs";
import AnimationContainer from "@/components/global/animation-container";
import MaxWidthWrapper from "@/components/global/max-width-wrapper";
import FinalCta from "@/components/marketing/final-cta";
import { buttonVariants } from "@/components/ui/button";
import { generateMetadata as baseMetadata } from "@/utils";
import blogs from "@/utils/constants/blogs.json";
import { ArrowLeftIcon, ArrowRightIcon, ClockIcon } from "lucide-react";
import Image from "next/image";
import Link from "next/link";
import { notFound } from "next/navigation";

interface Props {
    params: { slug: string };
}

const getBlog = (slug: string) => blogs.find((blog) => blog.slug === slug);

export const generateStaticParams = () =>
    blogs.map((blog) => ({ slug: blog.slug }));

export const generateMetadata = ({ params }: Props) => {
    const blog = getBlog(params.slug);
    if (!blog) return baseMetadata();
    return baseMetadata({
        title: `${blog.title} — Synapse Blog`,
        description: blog.description,
    });
};

const BlogDetailPage = ({ params }: Props) => {
    const blog = getBlog(params.slug);
    if (!blog) notFound();

    const index = blogs.indexOf(blog);
    const next = blogs[(index + 1) % blogs.length];

    return (
        <div className="overflow-x-hidden">
            <MaxWidthWrapper className="max-w-3xl">
                <AnimationContainer delay={0.05}>
                    <Link
                        href="/resources/blog"
                        className={buttonVariants({
                            variant: "ghost",
                            size: "sm",
                            className: "mt-4 -ml-2 text-muted-foreground hover:text-foreground",
                        })}
                    >
                        <ArrowLeftIcon className="mr-2 size-4" />
                        All posts
                    </Link>
                </AnimationContainer>

                <AnimationContainer delay={0.1} className="w-full">
                    <div className="flex flex-wrap items-center gap-x-2 gap-y-1 text-sm text-muted-foreground">
                        <span className="text-foreground">{blog.author_name}</span>
                        <span aria-hidden>·</span>
                        <span>{blog.author_role}</span>
                        <span aria-hidden>·</span>
                        <span>{formatBlogDate(blog.date_published)}</span>
                        <span aria-hidden>·</span>
                        <span className="flex items-center gap-1">
                            <ClockIcon className="size-3.5" />
                            {blog.reading_time}
                        </span>
                    </div>
                    <h1 className="mt-5 text-3xl font-medium !leading-[1.15] tracking-tight text-balance text-foreground md:text-5xl font-heading">
                        {blog.title}
                    </h1>
                    <p className="mt-5 text-lg text-muted-foreground text-balance">{blog.lede}</p>
                </AnimationContainer>

                <AnimationContainer delay={0.15} className="w-full">
                    <div className="mt-10 overflow-hidden rounded-xl border border-border/60">
                        <Image
                            src={blog.image}
                            alt=""
                            width={1200}
                            height={800}
                            unoptimized
                            priority
                            className="size-full object-cover"
                        />
                    </div>
                </AnimationContainer>

                <AnimationContainer delay={0.2} className="w-full">
                    <article className="flex flex-col gap-10 py-12">
                        {blog.content.map((section) => (
                            <section key={section.heading}>
                                <h2 className="text-xl font-medium text-foreground md:text-2xl font-heading">
                                    {section.heading}
                                </h2>
                                <div className="mt-4 flex flex-col gap-4">
                                    {section.body.map((paragraph) => (
                                        <p
                                            key={paragraph.slice(0, 32)}
                                            className="leading-relaxed text-muted-foreground"
                                        >
                                            {paragraph}
                                        </p>
                                    ))}
                                </div>
                            </section>
                        ))}
                    </article>
                </AnimationContainer>

                <AnimationContainer delay={0.1} className="w-full">
                    <div className="mb-16 flex flex-col gap-4 rounded-xl border border-border/60 bg-foreground/[0.02] p-6 md:flex-row md:items-center md:justify-between">
                        <div>
                            <p className="text-base font-medium text-foreground">
                                Want to try what this post describes?
                            </p>
                            <p className="mt-1 text-sm text-muted-foreground">
                                Install a client, or run a node of your own — both are MIT licensed.
                            </p>
                        </div>
                        <Link
                            href="/download"
                            className={buttonVariants({ variant: "accent", className: "shrink-0 gap-2" })}
                        >
                            Download Synapse
                            <ArrowRightIcon className="size-4" />
                        </Link>
                    </div>
                </AnimationContainer>
            </MaxWidthWrapper>

            <MaxWidthWrapper>
                <AnimationContainer delay={0.1}>
                    <Link
                        href={`/resources/blog/${next.slug}`}
                        className="mx-auto mb-16 flex max-w-3xl flex-col gap-1 rounded-xl border border-border/60 p-6 transition-colors duration-300 hover:border-border hover:bg-foreground/[0.03]"
                    >
                        <span className="text-xs uppercase tracking-wide text-muted-foreground">
                            Next post
                        </span>
                        <span className="flex items-center gap-2 text-lg font-medium text-foreground">
                            {next.title}
                            <ArrowRightIcon className="size-4 shrink-0 text-cyan-300" />
                        </span>
                        <span className="text-sm text-muted-foreground">{next.description}</span>
                    </Link>
                </AnimationContainer>
            </MaxWidthWrapper>

            <FinalCta
                title="Built in public, written up as we go"
                description="New posts land with each release. Subscribe through the repository releases page."
            />
        </div>
    );
};

export default BlogDetailPage;
