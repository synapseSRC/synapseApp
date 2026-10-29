"use client";

import {
    Accordion,
    AccordionContent,
    AccordionItem,
    AccordionTrigger,
} from "@/components/ui/accordion";
import { FAQ } from "@/utils/constants/faq";

const Faq = () => {
    return (
        <Accordion type="single" collapsible defaultValue="item-1" className="w-full">
            {FAQ.map((item) => (
                <AccordionItem key={item.id} id={item.id} value={item.id} className="scroll-mt-28">
                    <AccordionTrigger className="text-left text-base hover:text-foreground">
                        {item.question}
                    </AccordionTrigger>
                    <AccordionContent className="text-sm leading-relaxed text-muted-foreground">
                        {item.answer}
                    </AccordionContent>
                </AccordionItem>
            ))}
        </Accordion>
    );
};

export default Faq;
