import Footer from "@/components/navigation/footer";
import Navbar from "@/components/navigation/navbar";
import { buttonVariants } from "@/components/ui/button";
import Link from "next/link";

const NotFound = () => {
    return (
        <main className="relative flex flex-col items-center justify-center px-4">
            <Navbar />

            <div className="mx-auto flex h-screen w-full flex-col items-center justify-center">
                <span className="rounded-md bg-gradient-to-br from-violet-500 to-cyan-400 px-3.5 py-1 text-sm font-medium text-neutral-50 not-found">
                    404
                </span>
                <h1 className="mt-5 text-center text-3xl font-bold text-neutral-50 md:text-5xl font-heading">
                    This page has no route
                </h1>
                <p className="mx-auto mt-5 max-w-xl text-center text-base font-medium text-neutral-400">
                    The address you followed is not part of Synapse. Try the{" "}
                    <Link href="/download" className="text-foreground underline underline-offset-4">
                        downloads
                    </Link>
                    , the{" "}
                    <Link href="/open-source" className="text-foreground underline underline-offset-4">
                        open-source guide
                    </Link>
                    , or{" "}
                    <Link href="/resources/help" className="text-foreground underline underline-offset-4">
                        ask us
                    </Link>
                    .
                </p>
                <div className="mt-8 flex flex-wrap items-center justify-center gap-3">
                    <Link href="/" className={buttonVariants({ variant: "accent" })}>
                        Back to the homepage
                    </Link>
                    <Link href="/changelog" className={buttonVariants({ variant: "outline" })}>
                        Read the changelog
                    </Link>
                </div>
            </div>

            <Footer />
        </main>
    );
};

export default NotFound;
