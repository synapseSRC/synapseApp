export type PlatformId = "android" | "ios" | "windows" | "linux" | "cli";

export type DownloadChannel = {
    label: string;
    href: string;
    detail?: string;
};

export type Platform = {
    id: PlatformId;
    name: string;
    category: "Mobile" | "Desktop" | "Terminal";
    blurb: string;
    requirement: string;
    primary: DownloadChannel;
    alternates: DownloadChannel[];
};

// Store and release links are placeholders — replace them before launch (see README).
export const PLATFORMS: Platform[] = [
    {
        id: "android",
        name: "Android",
        category: "Mobile",
        blurb: "Phone and tablet, with material-you theming and share-to-Synapse.",
        requirement: "Android 8.0 or later",
        primary: { label: "Google Play", href: "#", detail: "Open testing available" },
        alternates: [
            { label: "APK", href: "#", detail: "Signature verified" },
            { label: "F-Droid", href: "#" },
        ],
    },
    {
        id: "ios",
        name: "iOS",
        category: "Mobile",
        blurb: "iPhone and iPad, with widgets, push relay and offline inbox.",
        requirement: "iOS 16 or later",
        primary: { label: "App Store", href: "#" },
        alternates: [{ label: "TestFlight", href: "#", detail: "Weekly builds" }],
    },
    {
        id: "windows",
        name: "Windows",
        category: "Desktop",
        blurb: "Native desktop client with tray quick-switch and global shortcuts.",
        requirement: "Windows 10 or 11, 64-bit",
        primary: { label: "Installer (.exe)", href: "#" },
        alternates: [
            { label: "winget", href: "#", detail: "winget install Synapse.Synapse" },
            { label: "GitHub Releases", href: "#" },
        ],
    },
    {
        id: "linux",
        name: "Linux",
        category: "Desktop",
        blurb: "Wayland and X11, with MPRIS media keys and pipewire screen share.",
        requirement: "Ubuntu 22.04+, Fedora 38+, Debian 12+",
        primary: { label: "Debian package (.deb)", href: "#" },
        alternates: [
            { label: ".AppImage", href: "#" },
            { label: "Flatpak", href: "#", detail: "flathub.org" },
            { label: ".rpm", href: "#" },
        ],
    },
    {
        id: "cli",
        name: "CLI",
        category: "Terminal",
        blurb: "Read, send and script conversations without leaving the shell.",
        requirement: "Node.js 18+ on macOS, Linux or WSL",
        primary: { label: "npm", href: "#", detail: "npm i -g @synapse/cli" },
        alternates: [
            { label: "Homebrew", href: "#", detail: "brew install synapse" },
            { label: "cargo", href: "#" },
        ],
    },
];

export const CLI_INSTALL_COMMANDS = [
    { label: "npm", command: "npm install -g @synapse/cli" },
    { label: "Homebrew", command: "brew install synapse" },
    { label: "Verify", command: "synapse --version" },
];

export const SELF_HOST_COMMANDS = [
    { label: "Clone", command: "git clone https://github.com/synapse/synapse.git" },
    { label: "Run", command: "docker compose up -d" },
    { label: "Bootstrap", command: "synapse init --domain social.example.com" },
];

export const getPlatform = (id: PlatformId) =>
    PLATFORMS.find((platform) => platform.id === id)!;
