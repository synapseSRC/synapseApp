# Synapse — Landing Page

Marketing site for **Synapse**, an open-source social app for DMs, communities and feeds, with
native clients for Android, iOS, Windows, Linux and the CLI.

## Pages

| Route | Purpose |
| --- | --- |
| `/` | Product overview |
| `/download` | Platform builds, install methods, system requirements, FAQ |
| `/open-source` | Self-hosting, licensing, contributing |
| `/features/messaging` | End-to-end encrypted DMs |
| `/features/communities` | Spaces, channels, roles, moderation |
| `/features/sync` | Offline-first cross-device sync |
| `/features/api` | Open API and CLI |
| `/changelog` | Release notes |
| `/resources/blog` | Articles |
| `/resources/help` | Support |
| `/privacy`, `/terms` | Legal |

The site is entirely static — there is no sign-in, dashboard or billing surface. Every call to
action points at a download.

## Stack

- Next.js 14 (App Router) + React 18 + TypeScript
- Tailwind CSS, shadcn/ui, Magic UI, Aceternity UI primitives
- framer-motion for entrance and hover animation
- Static metadata images: `src/app/icon.svg`, plus PNGs rasterized from `public/assets/og-card.svg`

## Development

```bash
npm install
npm run dev      # http://localhost:3000
```

Environment variables (all optional, see `.env.example`):

| Variable | Default | Used for |
| --- | --- | --- |
| `NEXT_PUBLIC_APP_NAME` | `Synapse` | Copy and metadata |
| `NEXT_PUBLIC_APP_DOMAIN` | `https://synapse.app` | `metadataBase` and canonical URLs |

```bash
npx tsc --noEmit   # type check
npm run lint       # eslint
npm run build      # static production build
```

## Social card

`src/app/opengraph-image.png`, `src/app/twitter-image.png` and `src/app/apple-icon.png` are
rasterized from `public/assets/og-card.svg` and `src/app/icon.svg` with headless Chromium, and
committed, because `next/og` cannot resolve its bundled font and WASM assets while prerendering on
Windows. After editing the SVG:

```bash
msedge --headless=new --hide-scrollbars --window-size=1200,630 \
  --screenshot=src/app/opengraph-image.png public/assets/og-card.svg
```

## Before launch

Store links, repository URLs and the support address are placeholders. They are grouped in two
files so the swap is mechanical:

- `src/utils/constants/site.ts` — `PROJECT_LINKS`, `SUPPORT_EMAIL`
- `src/utils/constants/platforms.ts` — `href` for each platform and channel

The privacy policy and terms are draft copy written for this product; have them reviewed before
publishing.

## License

MIT. See [LICENSE](LICENSE).
