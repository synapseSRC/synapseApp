"use client";

/**
 * Last-resort boundary: it replaces the root layout, so globals.css is not applied here and the
 * markup has to stand on its own.
 */
const GlobalError = ({ reset }: { error: Error & { digest?: string }; reset: () => void }) => (
    <html lang="en">
        <body
            style={{
                minHeight: "100vh",
                display: "grid",
                placeItems: "center",
                fontFamily: "system-ui, -apple-system, 'Segoe UI', sans-serif",
                margin: 0,
            }}
        >
            <div style={{ textAlign: "center", padding: "2rem" }}>
                <h1 style={{ fontSize: "1.25rem", fontWeight: 600 }}>Something went wrong</h1>
                <p style={{ marginTop: "0.5rem", fontSize: "0.875rem", opacity: 0.7 }}>
                    The application failed to load. Please try again.
                </p>
                <button
                    type="button"
                    onClick={reset}
                    style={{
                        marginTop: "1.25rem",
                        padding: "0.6rem 1.2rem",
                        borderRadius: "0.5rem",
                        border: "1px solid currentColor",
                        background: "transparent",
                        color: "inherit",
                        cursor: "pointer",
                    }}
                >
                    Try again
                </button>
            </div>
        </body>
    </html>
);

export default GlobalError;
