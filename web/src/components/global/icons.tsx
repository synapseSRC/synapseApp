import { LucideProps } from "lucide-react";

export const Icons = {
    /** Rounded gradient tile with the Synapse mark — use for the navbar, footer and auth surfaces. */
    logo: (props: LucideProps) => (
        <svg
            {...props}
            width="32"
            height="32"
            viewBox="0 0 32 32"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
        >
            <rect width="32" height="32" rx="9" fill="url(#synapse-tile)" />
            <path
                d="M10.5 21.5C16 21.5 16 10.5 21.5 10.5"
                stroke="white"
                strokeWidth="2.2"
                strokeLinecap="round"
            />
            <circle cx="10.5" cy="21.5" r="3.1" fill="white" />
            <circle cx="21.5" cy="10.5" r="3.1" fill="white" />
            <defs>
                <linearGradient
                    id="synapse-tile"
                    x1="0"
                    y1="0"
                    x2="32"
                    y2="32"
                    gradientUnits="userSpaceOnUse"
                >
                    <stop stopColor="#7C3AED" />
                    <stop offset="1" stopColor="#22D3EE" />
                </linearGradient>
            </defs>
        </svg>
    ),
    /** Bare mark, inherits the surrounding text colour. */
    mark: (props: LucideProps) => (
        <svg
            {...props}
            width="24"
            height="24"
            viewBox="0 0 32 32"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
        >
            <path
                d="M10.5 21.5C16 21.5 16 10.5 21.5 10.5"
                stroke="currentColor"
                strokeWidth="2.2"
                strokeLinecap="round"
            />
            <circle cx="10.5" cy="21.5" r="3.4" fill="currentColor" />
            <circle cx="21.5" cy="10.5" r="3.4" fill="currentColor" />
        </svg>
    ),
    github: (props: LucideProps) => (
        <svg
            {...props}
            viewBox="0 0 24 24"
            fill="currentColor"
            xmlns="http://www.w3.org/2000/svg"
        >
            <path d="M12 .5C5.65.5.5 5.66.5 12.02c0 5.08 3.29 9.39 7.86 10.91.58.1.79-.25.79-.55 0-.27-.01-1.17-.02-2.12-3.2.69-3.88-1.55-3.88-1.55-.52-1.33-1.28-1.69-1.28-1.69-1.05-.72.08-.71.08-.71 1.16.08 1.77 1.19 1.77 1.19 1.03 1.77 2.7 1.26 3.36.96.1-.75.4-1.26.73-1.55-2.55-.29-5.24-1.28-5.24-5.7 0-1.26.45-2.29 1.18-3.1-.12-.29-.51-1.46.11-3.05 0 0 .96-.31 3.15 1.18a10.9 10.9 0 0 1 2.87-.39c.97 0 1.95.13 2.87.39 2.18-1.49 3.14-1.18 3.14-1.18.63 1.59.24 2.76.12 3.05.74.81 1.18 1.84 1.18 3.1 0 4.43-2.7 5.41-5.26 5.69.41.36.78 1.06.78 2.14 0 1.55-.01 2.79-.01 3.17 0 .31.2.66.8.55A11.53 11.53 0 0 0 23.5 12.02C23.5 5.66 18.35.5 12 .5Z" />
        </svg>
    ),
};
