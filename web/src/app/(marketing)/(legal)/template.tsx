"use client";

import { motion, useReducedMotion } from "framer-motion";

const LegalTemplate = ({ children }: { children: React.ReactNode }) => {
    const reduceMotion = useReducedMotion();

    return (
        <motion.div
            initial={reduceMotion ? false : { opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.28, ease: "easeOut" }}
        >
            {children}
        </motion.div>
    );
};

export default LegalTemplate;
