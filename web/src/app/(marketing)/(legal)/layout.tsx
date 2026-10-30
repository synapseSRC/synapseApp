import MaxWidthWrapper from "@/components/global/max-width-wrapper";
import { LegalTabs } from "@/components/marketing/legal-tabs";

const LegalDocsLayout = ({ children }: { children: React.ReactNode }) => (
    <MaxWidthWrapper className="mx-auto max-w-3xl px-8 pb-32 md:max-w-3xl">
        <div className="w-full">
            <LegalTabs />
            {children}
        </div>
    </MaxWidthWrapper>
);

export default LegalDocsLayout;
