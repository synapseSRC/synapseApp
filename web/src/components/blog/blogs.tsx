import AnimationContainer from "@/components/global/animation-container";
import { Card, CardContent } from "@/components/ui/card";
import MagicCard from "@/components/ui/magic-card";
import { ArrowRightIcon, ClockIcon } from "lucide-react";
import Image from "next/image";
import Link from "next/link";
import blogs from "@/utils/constants/blogs.json";

export const formatBlogDate = (iso: string) =>
    new Date(iso).toLocaleDateString("en-US", {
        month: "long",
        day: "numeric",
        year: "numeric",
    });

const Blogs = () => {
    return (
        <div className="grid grid-cols-1 gap-6 px-4 md:grid-cols-2 md:px-0 lg:grid-cols-3">
            {blogs.map((blog, id) => (
                <AnimationContainer delay={0.1 * id} key={blog.slug} className="h-full">
                    <MagicCard className="relative h-full p-0 md:p-0">
                        <Link
                            href={`/resources/blog/${blog.slug}`}
                            className="absolute inset-0 z-10"
                        >
                            <span className="sr-only">Read {blog.title}</span>
                        </Link>
                        <Card className="group h-full border-0">
                            <CardContent className="flex h-full flex-col p-4 lg:p-5">
                                <div className="h-44 overflow-hidden rounded-lg border border-border/50 lg:h-52">
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
                                <div className="mt-4 flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-muted-foreground">
                                    <span>{blog.author_name}</span>
                                    <span aria-hidden>·</span>
                                    <span>{formatBlogDate(blog.date_published)}</span>
                                    <span aria-hidden>·</span>
                                    <span className="flex items-center gap-1">
                                        <ClockIcon className="size-3.5" />
                                        {blog.reading_time}
                                    </span>
                                </div>
                                <h3 className="mt-2 text-lg font-semibold text-foreground/80 transition-colors duration-300 group-hover:text-foreground">
                                    {blog.title}
                                </h3>
                                <p className="mt-2 text-sm text-muted-foreground">
                                    {blog.description}
                                </p>
                                <p className="mt-auto flex items-center gap-1.5 pt-6 text-sm text-cyan-300 transition-transform duration-300 group-hover:translate-x-0.5">
                                    Read the post
                                    <ArrowRightIcon className="size-4" />
                                </p>
                            </CardContent>
                        </Card>
                    </MagicCard>
                </AnimationContainer>
            ))}
        </div>
    );
};

export default Blogs;
