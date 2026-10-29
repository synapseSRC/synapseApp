import MaxWidthWrapper from "@/components/global/max-width-wrapper";
import Blogs from "@/components/blog/blogs";
import FinalCta from "@/components/marketing/final-cta";
import { PageHeader } from "@/components/marketing/page-header";
import { generateMetadata } from "@/utils";

export const metadata = generateMetadata({
    title: "Blog — Synapse",
    description:
        "Notes from building an open-source social platform: protocol decisions, self-hosting, offline-first sync and automation with the CLI.",
});

const BlogPage = () => {
    return (
        <div className="overflow-x-hidden">
            <MaxWidthWrapper>
                <PageHeader
                    badge="Blog"
                    title="Notes from building it in the open"
                    description="Design decisions, postmortems and guides from the people who write the protocol, the node and the clients."
                />
                <div className="pb-16">
                    <Blogs />
                </div>
            </MaxWidthWrapper>

            <FinalCta
                title="Read the code, too"
                description="Every post here describes something you can open in an editor and check."
            />
        </div>
    );
};

export default BlogPage;
