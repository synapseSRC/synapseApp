/** @type {import('next').NextConfig} */
const nextConfig = {
    async redirects() {
        return [
            // Superseded by the marketing pages of the Synapse launch.
            { source: "/pricing", destination: "/download", permanent: true },
            { source: "/enterprise", destination: "/open-source", permanent: true },
            { source: "/features", destination: "/#features", permanent: true },
            { source: "/blog", destination: "/resources/blog", permanent: true },
            // Old feature pages, mapped to the closest replacement.
            { source: "/features/link-shortening", destination: "/features/api", permanent: true },
            { source: "/features/analytics", destination: "/features/sync", permanent: true },
            { source: "/features/password-protection", destination: "/features/messaging", permanent: true },
            { source: "/features/qr-codes", destination: "/download", permanent: true },
        ];
    },
};

export default nextConfig;
