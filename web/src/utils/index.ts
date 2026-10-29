// constants
import { LIST_ITEM_VARIANTS, CHILD_VARIANTS, FADE_IN_VARIANTS, MODAL_VARIANTS } from "./constants/animation";
import { APP_DOMAIN, APP_NAME, PROJECT_LINKS, SUPPORT_EMAIL } from "./constants/site";
import { PROCESS, REVIEWS } from "./constants/misc";
import { NAV_LINKS } from "./constants/nav-links";
import { PLATFORMS, getPlatform } from "./constants/platforms";
import { aeonik, inter } from "./constants/fonts";

// functions
import { cn } from "./functions/cn";
import { generateMetadata } from "./functions/metadata";

export {
    // constants
    LIST_ITEM_VARIANTS,
    CHILD_VARIANTS,
    FADE_IN_VARIANTS,
    MODAL_VARIANTS,
    APP_DOMAIN,
    APP_NAME,
    PROJECT_LINKS,
    SUPPORT_EMAIL,
    NAV_LINKS,
    PLATFORMS,
    PROCESS,
    REVIEWS,
    aeonik,
    inter,

    // functions
    cn,
    generateMetadata,
    getPlatform,
};
