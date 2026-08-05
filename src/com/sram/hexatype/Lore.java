package com.sram.hexatype;

/**
 * A story for each of the thirty collectibles: where it lives, who its family is, and the
 * daft thing they all do together.
 *
 * Written to a fixed shape — one setting line and exactly {@link #LINES} story lines, none
 * longer than {@link #MAX_LINE} — so the popup in {@link Storybook} is one size for all
 * thirty and nothing has to reflow or scroll. A test holds every string to that.
 *
 * Only characters the harness font knows are used, which rules out apostrophes: a glyph
 * `tools/Font` has no bitmap for simply vanishes from the preview, so a line written with one
 * would look fine on the device and be missing a letter in every PNG. There is an assertion
 * for that too.
 */
final class Lore {

    /** Story lines per entry. */
    static final int LINES = 4;
    /**
     * Longest a line may be. At the popup's text size the harness font puts 36 characters
     * inside the panel with room to spare, and the harness is the wider of the two backends —
     * Quicksand on the device is narrower, so fitting here fits there.
     */
    static final int MAX_LINE = 36;

    // ---- the animated beats -------------------------------------------------
    // Ten reusable vignettes rather than thirty bespoke ones, the same bargain Skits makes
    // for the stage banners. Each is a loop, because the popup stays open until dismissed.
    static final int HANDOFF = 0, STACK = 1, PUSH = 2, PEEK = 3, TUMBLE = 4,
            BOUNCE = 5, PICNIC = 6, CARRY = 7, CHEER = 8, SEEK = 9;
    static final int BEATS = 10;

    private Lore() {}

    /** Where each one lives. Shown under the name, dimmed. */
    static final String[] WHERE = {
        "THE BOTTOM BASKET, BACK ROW",
        "THE WARM SHELF BY THE WINDOW",
        "THE LAUNDRY SINK, DO NOT ASK",
        "THE FREEZER DRAWER, TOP LEFT",
        "EVERY BIRTHDAY, ALL OF THEM",
        "THE BACK OF THE FREEZER",
        "THE CRAFT DRAWER, LID MISSING",
        "THE ORCHARD SHED, TOP SHELF",
        "UNDER THE DISCO LAMP, ALWAYS",
        "THE ROCK POOL, LOW TIDE",
        "THE SOUP, LURKING",
        "THE ROOF, AFTER BEDTIME",
        "THE VERY LAST BASKET",
        "THE FRUIT BOWL, TOP OF THE PILE",
        "THE PICNIC BLANKET, SLICE FOUR",
        "THE TOP BRANCH, TIED TOGETHER",
        "THE GARDEN, UNDER THE NET",
        "THE PAPER BOX, WRAPPED TWICE",
        "THE MARKET CRATE, THIRD FROM LEFT",
        "THE VINE, HIGHEST BUNCH",
        "THE WINDOW LEDGE, FULL SUN",
        "THE PARTY TABLE, CENTRE",
        "THE TOY BOX, VERY BOTTOM",
        "THE DESK DRAWER, BY THE PENS",
        "THE SWEET JAR, LID SLIGHTLY OFF",
        "THE BAKERY BOX, ONE LEFT",
        "THE SEAFRONT, MELTING SLOWLY",
        "THE POCKET, WITH THE FLUFF",
        "THE BEDSIDE TABLE, LIGHT ON",
        "UNDER THE BLANKET FORT",
    };

    static final String[][] STORY = {
        // ---- mystery dumplings ----
        {   // CREAM BAO
            "GRANDMA PLEATED HIM HERSELF -",
            "TWELVE FOLDS, NO SHORTCUTS.",
            "HE SHOWS EVERY NEW COUSIN HOW,",
            "THEN EATS THE PRACTICE DOUGH.",
        },
        {   // PEACH BUN
            "HIS BIG SISTER HOLDS THE LID UP",
            "WHILE HE COUNTS TO THREE.",
            "NOBODY HAS EVER GOT PAST TWO.",
            "SHE HOLDS IT ANYWAY.",
        },
        {   // TIE DYE BAO
            "THE TWINS DYED HIM AS A SURPRISE.",
            "IT WAS A SURPRISE.",
            "MUM SAYS THE SINK IS STILL PINK.",
            "THEY SHARE THE BLAME EVENLY.",
        },
        {   // SHERBET BAO
            "HE AND HIS COUSIN TRADE HALVES -",
            "ORANGE FOR CREAM, EVERY TIME.",
            "NEITHER LIKES THEIR OWN HALF.",
            "BOTH PRETEND THEY DO.",
        },
        {   // CONFETTI BUN
            "SHE SNEEZED AT HER OWN PARTY",
            "AND REDECORATED THE WHOLE ROOM.",
            "THE FAMILY CALLED IT AN UPGRADE",
            "AND NOW THEY SNEEZE ON PURPOSE.",
        },
        {   // SNOW BUN
            "HE IS SO CLEAR YOU CAN SEE",
            "HIS LITTLE BROTHER HIDING INSIDE.",
            "THEY THINK THIS IS UNDETECTABLE.",
            "EVERYONE PLAYS ALONG.",
        },
        {   // PINK GLITTER
            "SHE HUGGED EVERY SIBLING ONCE.",
            "NOW THE WHOLE FAMILY SPARKLES.",
            "DAD FOUND GLITTER IN HIS TEA",
            "IN MARCH. IT WAS AUGUST.",
        },
        {   // APPLE HOLO
            "HE SMELLS LIKE APPLES, WHICH IS",
            "WHY HIS COUSINS KEEP LICKING HIM.",
            "HE FORGIVES THEM EVERY TIME",
            "AND WARNS THE NEXT ONE ANYWAY.",
        },
        {   // PURPLE HOLO
            "HE TURNS SEVEN COLOURS AT ONCE,",
            "SO HIS SISTERS USE HIM AS A LAMP",
            "FOR HOMEWORK. HE CHARGES",
            "ONE CRUMB PER HOUR.",
        },
        {   // SEASHELL BAO
            "HER RIBS WHISTLE IN THE WIND.",
            "THE FAMILY LINES UP BY SIZE",
            "AND PLAYS ONE WHOLE SONG.",
            "THE SMALLEST ONE IS OFF KEY.",
        },
        {   // FIN BAO
            "HE PLAYS SHARK IN THE BROTH",
            "WHILE HIS BROTHERS SCREAM NICELY.",
            "IT IS THE ONLY GAME THEY ALL",
            "AGREE ON. GRANDMA REFEREES.",
        },
        {   // GALAXY BAO
            "HE SNEAKS UP WITH HIS COUSINS",
            "TO COUNT THE STARS ON HIS BACK.",
            "THEY LOSE COUNT ARGUING.",
            "THEY GO UP AGAIN TOMORROW.",
        },
        {   // GOLDEN TICKET
            "EVERYONE LOOKED FOR HIM AT ONCE",
            "AND FOUND HIM AT THE SAME MOMENT.",
            "SO THE FAMILY SPLIT HIM",
            "THIRTY WAYS AND KEPT HIM WHOLE.",
        },
        // ---- squishy fruits ----
        {   // NANA
            "HE IS THE FAMILY BRIDGE.",
            "EVERYONE WALKS OVER HIM",
            "TO REACH THE HIGH SHELF.",
            "HE HAS NEVER ONCE COMPLAINED.",
        },
        {   // MELON WEDGE
            "SEVEN SIBLINGS, ONE MELON.",
            "THEY MEASURED WITH A RULER",
            "AND ARGUED FOR AN HOUR.",
            "THEN ATE IT ALL IN A MINUTE.",
        },
        {   // CHERRY PAIR
            "TWO SISTERS ON ONE STEM.",
            "THEY HAVE NEVER BEEN APART,",
            "WHICH IS SWEET UNTIL ONE",
            "OF THEM WANTS TO GO LEFT.",
        },
        {   // PEAR DROP
            "HE HOLDS THE LADDER STEADY",
            "WHILE HIS BROTHER PICKS.",
            "HIS BROTHER IS ALSO A PEAR",
            "AND ALSO ON THE LADDER.",
        },
        {   // MOCHI PEACH
            "SHE IS THE SOFTEST SISTER,",
            "SO THE FAMILY PRACTISES HUGS",
            "ON HER. SHE KEEPS A LIST",
            "OF WHO OWES HER. IT IS LONG.",
        },
        {   // ORANGE POP
            "HE SQUIRTS WHEN HE LAUGHS.",
            "HIS COUSINS TELL HIM JOKES",
            "FROM A SAFE DISTANCE, TOGETHER,",
            "BEHIND ONE VERY BRAVE LEAF.",
        },
        {   // GLITTER GRAPE
            "THE WHOLE BUNCH SPARKLES,",
            "SO NOBODY CAN SNEAK ANYWHERE.",
            "THEY GAVE UP HIDE AND SEEK",
            "AND INVENTED SHOUT AND FIND.",
        },
        {   // LEMON CHROME
            "SHE IS SO SHINY THE FAMILY",
            "USES HER TO CHECK THEIR TEETH.",
            "SHE PRETENDS TO MIND.",
            "SHE HAS NEVER MOVED AN INCH.",
        },
        {   // RAINBOW MELON
            "ONE SLICE, EVERY COLOUR,",
            "BECAUSE ALL SEVEN COUSINS",
            "PICKED A FAVOURITE AND NOBODY",
            "WOULD BUDGE. SO THEY ALL WON.",
        },
        // ---- squeeze globs ----
        {   // GROOVY GLOB
            "OLDEST BROTHER, SQUISHIEST TOO.",
            "WHEN THE LITTLE ONES ARGUE",
            "THEY ALL SQUEEZE HIM AT ONCE",
            "UNTIL THEY FORGET WHY.",
        },
        {   // NICE CUBE
            "THE ONLY SQUARE ONE IN THE",
            "FAMILY, SO HE HOLDS THE DOOR",
            "WHILE HIS ROUND COUSINS ROLL IN.",
            "HE IS VERY PROUD OF THIS JOB.",
        },
        {   // GUMDROP
            "SHE IS COVERED IN SUGAR BUMPS.",
            "HER BROTHERS COUNT THEM EVERY",
            "SUNDAY AND GET A DIFFERENT",
            "NUMBER. THEY BLAME EACH OTHER.",
        },
        {   // DOHNUT
            "HE HAS A HOLE IN THE MIDDLE",
            "AND HIS BABY SISTER FITS IN IT.",
            "THIS IS HOW THE FAMILY",
            "CARRIES HER EVERYWHERE.",
        },
        {   // NICE CREAM
            "THE SCOOP AND THE CONE",
            "ARE COUSINS, NOT BROTHERS.",
            "THEY HAVE ONE JOB BETWEEN THEM",
            "AND HAVE NEVER DROPPED IT.",
        },
        {   // MARBLE GLOB
            "TWO COLOURS, TWO PARENTS,",
            "ONE VERY STRETCHY KID.",
            "HE CAN REACH THE TOP CUPBOARD",
            "IF SOMEBODY HOLDS HIS FEET.",
        },
        {   // DREAM DROP
            "SHE IS FULL OF GLITTER RAIN.",
            "THE WHOLE FAMILY SHAKES HER",
            "AT BEDTIME AND WATCHES IT SETTLE.",
            "NOBODY IS EVER ASLEEP FIRST.",
        },
        {   // GLOW GLOB
            "HE GLOWS, SO HIS BROTHERS",
            "ALWAYS SEND HIM IN FIRST.",
            "HE IS NOT BRAVE. HE IS JUST",
            "EASY TO FIND. THEY COME TOO.",
        },
    };

    /** Which vignette plays over each story. */
    static final int[] BEAT = {
        HANDOFF, PUSH, CHEER, HANDOFF, CHEER, PEEK, CHEER, SEEK, PICNIC, STACK,
        SEEK, STACK, CARRY,
        STACK, PICNIC, TUMBLE, STACK, CARRY, PEEK, SEEK, PICNIC, CHEER,
        PUSH, PUSH, PICNIC, CARRY, STACK, CARRY, BOUNCE, SEEK,
    };

    /**
     * Who is in the vignette with them. Always somebody from the same family, so a scene never
     * mixes a bao bun in with the stress balls.
     */
    static final int[] PARTNER = {
        1, 0, 3, 2, 6, 0, 4, 8, 7, 5,
        0, 12, 11,
        14, 13, 16, 13, 15, 20, 15, 18, 14,
        23, 22, 26, 24, 25, 29, 29, 28,
    };

    /**
     * The third figure, for the beats that need one.
     *
     * A table rather than the partner of the partner, which is what this was at first: most
     * pairings here are mutual, so that trick handed back the entry itself and the scene cast
     * the same character twice. It looked like a drawing bug and was a data one.
     */
    static final int[] THIRD = {
        2, 2, 4, 4, 2, 1, 2, 9, 9, 0,
        1, 8, 8,
        15, 15, 17, 14, 16, 19, 21, 21, 13,
        24, 24, 22, 22, 24, 22, 27, 27,
    };

    static int third(int i) {
        return THIRD[i];
    }
}
