import { Metadata } from "next";

import { APP_DOMAIN, APP_NAME } from "@/utils/constants/site";

export const generateMetadata = ({
    title = `${APP_NAME} — Open-source social for every device`,
    description = `${APP_NAME} is the open-source social app for DMs, communities and feeds. Native clients for Android, iOS, Windows, Linux and the CLI — with an open API and self-hosting built in.`,
    image = "/opengraph-image.png",
}: {
    title?: string;
    description?: string;
    image?: string;
} = {}): Metadata => ({
    metadataBase: new URL(APP_DOMAIN),
    title,
    description,
    applicationName: APP_NAME,
    openGraph: {
        title,
        description,
        siteName: APP_NAME,
        type: "website",
        locale: "en_US",
        url: APP_DOMAIN,
        images: [image],
    },
    twitter: {
        card: "summary_large_image",
        title,
        description,
        images: [image],
    },
});
