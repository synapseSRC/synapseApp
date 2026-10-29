"use client";
import React, { useState } from "react";
import { motion } from "framer-motion";

export const TextHoverEffect = ({
    text,
}: {
    text: string;
    automatic?: boolean;
}) => {
    const [hovered, setHovered] = useState(false);
    const [maskPosition, setMaskPosition] = useState({ cx: "50%", cy: "50%" });

    const handleMouseMove = (event: React.MouseEvent<SVGSVGElement>) => {
        const { left, top, width, height } =
            event.currentTarget.getBoundingClientRect();
        if (width === 0 || height === 0) return;

        const percentage = (value: number, size: number, origin: number) =>
            `${Math.min(100, Math.max(0, ((value - origin) / size) * 100))}%`;

        setMaskPosition({
            cx: percentage(event.clientX, width, left),
            cy: percentage(event.clientY, height, top),
        });
    };

    return (
        <svg
            width="100%"
            height="100%"
            viewBox="0 0 300 100"
            xmlns="http://www.w3.org/2000/svg"
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
            onMouseMove={handleMouseMove}
            className="select-none"
        >
            <defs>
                <linearGradient
                    id="textGradient"
                    gradientUnits="userSpaceOnUse"
                    cx="50%"
                    cy="50%"
                    r="25%"
                >
                    {hovered && (
                        <>
                            <stop offset="0%" stopColor={"var(--violet-500)"} />
                            <stop offset="25%" stopColor={"var(--purple-400)"} />
                            <stop offset="50%" stopColor={"var(--sky-400)"} />
                            <stop offset="75%" stopColor={"var(--cyan-400)"} />
                            <stop offset="100%" stopColor={"var(--teal-300)"} />
                        </>
                    )}
                </linearGradient>

                <radialGradient
                    id="revealMask"
                    gradientUnits="userSpaceOnUse"
                    cx={maskPosition.cx}
                    cy={maskPosition.cy}
                    r="20%"
                >
                    <stop offset="0%" stopColor="white" />
                    <stop offset="100%" stopColor="black" />
                </radialGradient>
                <mask id="textMask">
                    <rect
                        x="0"
                        y="0"
                        width="100%"
                        height="100%"
                        fill="url(#revealMask)"
                    />
                </mask>
            </defs>
            <text
                x="50%"
                y="50%"
                textAnchor="middle"
                dominantBaseline="middle"
                textLength={280}
                lengthAdjust="spacingAndGlyphs"
                strokeWidth="0.3"
                className="font-[helvetica] font-bold stroke-neutral-800 fill-transparent text-7xl"
                style={{ opacity: hovered ? 0.7 : 0 }}
            >
                {text}
            </text>
            <motion.text
                x="50%"
                y="50%"
                textAnchor="middle"
                dominantBaseline="middle"
                textLength={280}
                lengthAdjust="spacingAndGlyphs"
                strokeWidth="0.3"
                className="font-[helvetica] font-bold fill-transparent text-7xl stroke-neutral-800"
                initial={{ strokeDashoffset: 1000, strokeDasharray: 1000 }}
                animate={{
                    strokeDashoffset: 0,
                    strokeDasharray: 1000,
                }}
                transition={{
                    duration: 4,
                    ease: "easeInOut",
                }}
            >
                {text}
            </motion.text>
            <text
                x="50%"
                y="50%"
                textAnchor="middle"
                dominantBaseline="middle"
                textLength={280}
                lengthAdjust="spacingAndGlyphs"
                stroke="url(#textGradient)"
                strokeWidth="0.3"
                mask="url(#textMask)"
                className="font-[helvetica] font-bold fill-transparent text-7xl"
            >
                {text}
            </text>
        </svg>
    );
};
