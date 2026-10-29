/**
 * Reduces a GitHub release body to heading-grouped change lines, mirroring the Android
 * `ReleaseNotesParser` so both clients present a release identically.
 *
 * Only the markdown a changelog realistically uses is understood: ATX headings, bullet and ordered
 * lists, task-list checkboxes, quotes, table rows and soft-wrapped paragraphs. Inline `**bold**` and
 * `` `code` `` survive because the page renders them; images, HTML tags, reference links and code
 * fences are dropped, and a link is kept only when it points at a plain http(s) address.
 */

export type ChangeKind = "added" | "improved" | "fixed" | "changed";

export type ReleaseGroup = {
    kind: ChangeKind;
    /** The author's heading, verbatim; empty when the notes start before any heading. */
    title: string;
    items: string[];
};

const HTML_COMMENT = /<!--[\s\S]*?-->/g;
const HTML_TAG = /<\/?[a-zA-Z][^>]*>/g;
const IMAGE = /!\[[^\]]*\]\([^)]*\)/g;
const REFERENCE_LINK = /\[([^\]]+)\]\[[^\]]*\]/g;
const INLINE_LINK = /\[([^\]]+)\]\(([^)]*)\)/g;
const AUTOLINK = /<((?:https?|ftp):\/\/[^>\s]+)>/gi;
const HEADING = /^\s{0,3}#{1,6}\s*(.+)$/;
const BOLD_HEADING = /^\s*\*\*(.+?)\*\*\s*[:：]?\s*$/;
const BULLET = /^\s*[-*+]\s*(.*)$/;
const ORDERED = /^\s*\d+[.)]\s*(.*)$/;
const QUOTE = /^\s*>+\s*(.*)$/;
const CHECKBOX = /^\[[ xX]\]\s*(.*)$/;
const CODE_FENCE = /^\s*(?:```|~~~)/;
const HORIZONTAL_RULE = /^\s*(?:[-*_]\s*){3,}$/;
const TABLE_ROW = /^\s*\|(.+)\|\s*$/;
const TABLE_DELIMITER = /^:?-+:?$/;

const FIX_KEYWORDS = ["fix", "bug", "resolv", "patch", "regress", "crash", "hotfix"];
const FEATURE_KEYWORDS = ["feature", "what's new", "whats new", "added", "addition", "launch"];
const IMPROVEMENT_KEYWORDS = [
    "improv",
    "enhanc",
    "optimi",
    "performance",
    "polish",
    "refactor",
    "change",
    "update",
];

/** Key of the untitled group that collects notes written before any heading. */
const UNTITLED_KEY = "other:";
const HEADING_MAX_LENGTH = 80;
const LETTER_OR_DIGIT = new RegExp("[\\p{L}\\p{N}]", "u");

type Heading = { title: string; kind: ChangeKind };

/** Structured notes for a release body; empty when there is nothing presentable. */
export function parseReleaseNotes(body: string | null | undefined): ReleaseGroup[] {
    if (!body || !body.trim()) return [];
    const text = body.replace(HTML_COMMENT, "").replace(/\r\n/g, "\n").replace(/\r/g, "\n");

    const groups = new Map<string, ReleaseGroup>();
    let activeKey: string | null = null;
    let paragraph = "";
    let insideCodeFence = false;

    const append = (item: string) => {
        if (!item.trim()) return;
        if (activeKey === null) activeKey = UNTITLED_KEY;
        let group = groups.get(activeKey);
        if (!group) {
            group = { kind: kindOf(activeKey), title: titleOf(activeKey), items: [] };
            groups.set(activeKey, group);
        }
        group.items.push(item);
    };

    const flushParagraph = () => {
        if (!paragraph) return;
        append(inline(paragraph));
        paragraph = "";
    };

    for (const rawLine of text.split("\n")) {
        const line = rawLine.trimEnd();
        if (CODE_FENCE.test(line)) {
            flushParagraph();
            insideCodeFence = !insideCodeFence;
            continue;
        }
        if (insideCodeFence) continue;

        if (!line.trim() || HORIZONTAL_RULE.test(line)) {
            flushParagraph();
            continue;
        }

        const heading = matchHeading(line);
        if (heading) {
            flushParagraph();
            const key =
                heading.kind === "changed"
                    ? `${UNTITLED_KEY}${heading.title.toLowerCase()}`
                    : heading.kind;
            activeKey = key;
            if (!groups.has(key)) {
                groups.set(key, { kind: heading.kind, title: heading.title, items: [] });
            }
            continue;
        }

        const listItem = matchListItem(line);
        if (listItem !== null) {
            flushParagraph();
            append(inline(listItem));
            continue;
        }

        const row = matchTableRow(line);
        if (row !== null) {
            flushParagraph();
            append(row);
            continue;
        }

        const content = (QUOTE.exec(line)?.[1] ?? line).trim();
        paragraph = paragraph ? `${paragraph} ${content}` : content;
    }
    flushParagraph();

    return Array.from(groups.values()).filter((group) => group.items.length > 0);
}

function matchHeading(line: string): Heading | null {
    const raw = HEADING.exec(line)?.[1] ?? BOLD_HEADING.exec(line)?.[1];
    if (!raw) return null;
    const title = cleanHeading(raw);
    if (!title) return null;
    return { title, kind: classify(title) };
}

/** Text of a list line with its bullet and checkbox markers removed, or `null` for other lines. */
function matchListItem(line: string): string | null {
    const withoutQuote = QUOTE.exec(line)?.[1] ?? line;
    const marker = BULLET.exec(withoutQuote)?.[1] ?? ORDERED.exec(withoutQuote)?.[1];
    if (marker === undefined) return null;
    return CHECKBOX.exec(marker.trim())?.[1] ?? marker;
}

/** A markdown table row flattened to one text line, or `null` for other lines. */
function matchTableRow(line: string): string | null {
    const raw = TABLE_ROW.exec(line)?.[1];
    if (raw === undefined) return null;
    const cells = raw
        .split("|")
        .map((cell) => cell.trim())
        .filter((cell) => cell.length > 0);
    if (cells.length === 0 || cells.every((cell) => TABLE_DELIMITER.test(cell))) return "";
    return cells.map(inline).join(" | ");
}

function cleanHeading(value: string): string {
    const text = inline(value);
    let start = 0;
    let end = text.length;
    while (start < end && !LETTER_OR_DIGIT.test(text[start])) start += 1;
    while (end > start && !LETTER_OR_DIGIT.test(text[end - 1])) end -= 1;
    return text.slice(start, end).replace(/\s{2,}/g, " ").slice(0, HEADING_MAX_LENGTH);
}

/** Collapses a line to the safe inline subset the page understands. */
function inline(value: string): string {
    return value
        .replace(HTML_TAG, " ")
        .replace(IMAGE, " ")
        .replace(AUTOLINK, (_match, url: string) => safeLink(url, url))
        .replace(REFERENCE_LINK, (_match, label: string) => label)
        .replace(INLINE_LINK, (_match, label: string, destination: string) =>
            safeLink(label, destination),
        )
        .replace(/\\\|/g, "|")
        .replace(/^[ \t]+/, "")
        .replace(/\s{2,}/g, " ")
        .trim();
}

/** Keeps markdown link syntax only for safe destinations, otherwise the label alone. */
function safeLink(label: string, destination: string): string {
    return isWebLink(destination) ? `[${label}](${destination})` : inline(label);
}

function isWebLink(value: string): boolean {
    try {
        const url = new URL(value.trim());
        return (
            (url.protocol === "https:" || url.protocol === "http:") &&
            url.hostname.length > 0 &&
            !url.username &&
            !url.password
        );
    } catch {
        return false;
    }
}

function classify(title: string): ChangeKind {
    const normalized = title.toLowerCase();
    if (FIX_KEYWORDS.some((keyword) => normalized.includes(keyword))) return "fixed";
    if (FEATURE_KEYWORDS.some((keyword) => normalized.includes(keyword))) return "added";
    if (IMPROVEMENT_KEYWORDS.some((keyword) => normalized.includes(keyword))) return "improved";
    return "changed";
}

function kindOf(key: string): ChangeKind {
    return key === "added" || key === "improved" || key === "fixed" ? key : "changed";
}

function titleOf(key: string): string {
    return key.startsWith(UNTITLED_KEY) ? key.slice(UNTITLED_KEY.length) : "";
}
