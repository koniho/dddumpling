package com.sram.hexatype;

/**
 * A story for each of the thirty collectibles: where it lives, who its family is, and the
 * daft thing they all do together.
 *
 * Written to a fixed shape — one setting line and exactly {@link #LINES} story lines, none
 * longer than {@link #MAX_LINE} — so the popup in {@link Storybook} is one size for all
 * thirty and nothing has to reflow or scroll. A test holds every string to that.
 *
 * Only characters the harness font knows are used. A glyph `tools/Font` has no bitmap for
 * simply vanishes from the preview, so a line using one would look fine on the device and be
 * missing a letter in every PNG — which is why the apostrophe had to be added to that font
 * before these could be written with possessives. There is an assertion for it either way.
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
        "THE LAUNDRY SINK, DON'T ASK",
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
        "THE FIRST TURN IN THE MILKY WAY",
        "THE TAIL OF A TINY COMET",
        "THE QUIET SIDE OF THE MOON",
        "BETWEEN TWO NORTHERN LIGHTS",
        "THE LAST STAR BEFORE MORNING",
        "THE COOLER, BESIDE THE JUICE",
        "THE JAM SHELF, SQUARED OFF",
        "THE GREENHOUSE, UNDER GLASS",
        "THE SUNBEAM ON THE TILE",
        "THE SODA CRATE, BOTTOM ROW",
        "THE ARCADE, BEHIND THE SCREEN",
        "THE BATHROOM, BY THE BUBBLES",
        "THE PORCH AT SUPPER TIME",
        "THE CROWN SHELF, DEAD CENTRE",
        "THE PICNIC TIN, WOBBLING",
    };

    static final String[][] STORY = {
        // ---- mystery dumplings ----
        {   // CREAM BAO
            "GRANDMA'S OWN TWELVE FOLDS, AND",
            "SHE COUNTED EVERY ONE.",
            "HE TEACHES THE NEW COUSINS,",
            "THEN EATS THEIR PRACTICE DOUGH.",
        },
        {   // PEACH BUN
            "HIS SISTER'S JOB IS THE LID.",
            "HIS JOB IS COUNTING TO THREE.",
            "HE'S NEVER GOT PAST TWO.",
            "SHE HOLDS IT ANYWAY.",
        },
        {   // TIE DYE BAO
            "THE TWINS' SURPRISE WORKED.",
            "HE WAS VERY SURPRISED.",
            "MUM SAYS THE SINK'S STILL PINK.",
            "THEY'RE SHARING THE BLAME.",
        },
        {   // SHERBET BAO
            "HE SWAPS HALVES WITH HIS COUSIN -",
            "ORANGE FOR CREAM, EVERY TIME.",
            "NEITHER LIKES WHAT THEY'VE GOT.",
            "NEITHER'S GOING TO SAY SO.",
        },
        {   // CONFETTI BUN
            "SHE SNEEZED AT HER OWN PARTY AND",
            "REDECORATED GRANDMA'S KITCHEN.",
            "THE FAMILY CALLED IT AN UPGRADE.",
            "NOW THEY ALL SNEEZE ON PURPOSE.",
        },
        {   // SNOW BUN
            "HE'S SO CLEAR YOU CAN SEE HIS",
            "LITTLE BROTHER HIDING INSIDE.",
            "THEY'RE SURE NOBODY'S NOTICED.",
            "NOBODY'S SAID A WORD.",
        },
        {   // PINK GLITTER
            "ONE HUG EACH WAS ALL IT TOOK.",
            "NOW THE WHOLE FAMILY SPARKLES.",
            "DAD'S STILL FINDING IT IN HIS TEA",
            "AND THE PARTY WAS IN MARCH.",
        },
        {   // APPLE HOLO
            "HE SMELLS OF APPLES. THAT'S WHY",
            "HIS COUSINS KEEP LICKING HIM.",
            "HE'S FORGIVEN ALL OF THEM,",
            "AND HE STILL WARNS THE NEXT ONE.",
        },
        {   // PURPLE HOLO
            "HE DOES SEVEN COLOURS AT ONCE,",
            "SO HE'S HIS SISTERS' READING LAMP.",
            "HE CHARGES ONE CRUMB AN HOUR.",
            "THEY'VE NEVER ONCE PAID UP.",
        },
        {   // SEASHELL BAO
            "HER RIBS WHISTLE IN THE WIND.",
            "THE FAMILY LINES UP BY SIZE AND",
            "PLAYS A WHOLE SONG THROUGH.",
            "THE LITTLEST ONE'S ALWAYS FLAT.",
        },
        {   // FIN BAO
            "HE PLAYS SHARK IN THE BROTH.",
            "HIS BROTHERS' JOB IS SCREAMING.",
            "IT'S THE ONLY GAME THEY AGREE ON.",
            "GRANDMA REFEREES FROM THE LID.",
        },
        {   // GALAXY BAO
            "HE AND HIS COUSINS SNEAK UP THERE",
            "TO COUNT THE STARS ON HIS BACK.",
            "THEY'VE NEVER FINISHED - SOMEONE",
            "ALWAYS ARGUES. THEY'LL TRY AGAIN.",
        },
        {   // GOLDEN TICKET
            "THIRTY OF THEM WENT LOOKING.",
            "THIRTY OF THEM FOUND HIM AT ONCE.",
            "SO HE'S THE FAMILY'S, NOT ANY",
            "ONE OF THEIRS. NOBODY MINDS.",
        },
        // ---- squishy fruits ----
        {   // NANA
            "HE'S THE FAMILY'S BRIDGE.",
            "THEY ALL WALK OVER HIM TO GET",
            "TO THE HIGH SHELF.",
            "HE'S NEVER ONCE COMPLAINED.",
        },
        {   // MELON WEDGE
            "SEVEN SIBLINGS, ONE MELON.",
            "DAD'S RULER CAME OUT. THEY",
            "ARGUED FOR A WHOLE HOUR,",
            "THEN ATE THE LOT IN A MINUTE.",
        },
        {   // CHERRY PAIR
            "TWO SISTERS ON ONE STEM.",
            "THEY'VE NEVER BEEN APART, WHICH",
            "IS LOVELY RIGHT UP UNTIL ONE OF",
            "THEM WANTS TO GO LEFT.",
        },
        {   // PEAR DROP
            "HE HOLDS THE LADDER STEADY",
            "WHILE HIS BROTHER PICKS.",
            "HIS BROTHER'S ALSO A PEAR,",
            "AND ALSO ON THE LADDER.",
        },
        {   // MOCHI PEACH
            "SHE'S THE SOFTEST SISTER, SO",
            "THE FAMILY PRACTISES HUGS ON HER.",
            "SHE'S KEEPING A LIST OF WHO",
            "OWES HER ONE. IT'S GETTING LONG.",
        },
        {   // ORANGE POP
            "HE SQUIRTS WHEN HE LAUGHS, SO",
            "HIS COUSINS TELL HIM THEIR JOKES",
            "FROM BEHIND ONE LEAF.",
            "THEY'VE AGREED IT'S WORTH IT.",
        },
        {   // GLITTER GRAPE
            "THE WHOLE BUNCH SPARKLES, SO",
            "NOBODY'S EVER HIDDEN SUCCESSFULLY.",
            "THEY'VE GIVEN UP ON HIDE AND SEEK",
            "AND INVENTED SHOUT AND FIND.",
        },
        {   // LEMON CHROME
            "SHE'S SO SHINY THE FAMILY CHECK",
            "THEIR TEETH IN HER.",
            "SHE PRETENDS SHE MINDS.",
            "SHE'S NEVER MOVED AN INCH.",
        },
        {   // RAINBOW MELON
            "ONE SLICE, EVERY COLOUR, BECAUSE",
            "ALL SEVEN COUSINS PICKED A",
            "FAVOURITE AND WOULDN'T BUDGE.",
            "SO EVERYBODY'S RIGHT.",
        },
        // ---- squeeze globs ----
        {   // GROOVY GLOB
            "OLDEST BROTHER, SQUISHIEST TOO.",
            "WHEN THE LITTLE ONES ARE ARGUING",
            "THEY ALL SQUEEZE HIM AT ONCE",
            "AND FORGET WHAT IT WAS ABOUT.",
        },
        {   // NICE CUBE
            "THE FAMILY'S ONLY SQUARE ONE,",
            "SO HOLDING THE DOOR IS HIS JOB",
            "WHILE THE ROUND COUSINS ROLL IN.",
            "HE COULDN'T BE PROUDER OF IT.",
        },
        {   // GUMDROP
            "SHE'S COVERED IN SUGAR BUMPS.",
            "HER BROTHERS COUNT THEM EVERY",
            "SUNDAY AND NEVER AGREE.",
            "IT'S ALWAYS THE OTHER ONE'S FAULT.",
        },
        {   // DOHNUT
            "HE'S GOT A HOLE IN THE MIDDLE",
            "AND HIS BABY SISTER FITS IT.",
            "THAT'S HOW THE FAMILY CARRIES",
            "HER EVERYWHERE. SHE'S DELIGHTED.",
        },
        {   // NICE CREAM
            "THE SCOOP AND THE CONE ARE",
            "COUSINS, NOT BROTHERS.",
            "THERE'S ONE JOB BETWEEN THEM",
            "AND THEY'VE NEVER DROPPED IT.",
        },
        {   // MARBLE GLOB
            "TWO COLOURS, TWO PARENTS,",
            "ONE VERY STRETCHY KID.",
            "HE'LL REACH THE TOP CUPBOARD",
            "IF SOMEONE HOLDS ONTO HIS FEET.",
        },
        {   // DREAM DROP
            "SHE'S FULL OF GLITTER RAIN.",
            "IT'S THE FAMILY'S BEDTIME -",
            "SHAKE HER, THEN WATCH IT SETTLE.",
            "NOBODY'S EVER ASLEEP FIRST.",
        },
        {   // GLOW GLOB
            "HE GLOWS, SO HIS BROTHERS SEND",
            "HIM IN FIRST. HE ISN'T BRAVE -",
            "HE'S JUST EASY TO FIND.",
            "THEY'RE RIGHT BEHIND HIM.",
        },
        {"HE COUNTS EACH STAR AS IT PASSES,", "THEN STARTS AGAIN AT DAWN.",
            "HIS FAMILY KNOWS HE CAN'T STOP.", "THEY COUNT ALONG ANYWAY."},
        {"SHE RIDES BEHIND EVERY COMET,", "HOLDING ON WITH BOTH LITTLE ARMS.",
            "HER COUSINS WAVE FROM BELOW.", "SHE ALWAYS WAVES BACK."},
        {"THE MOON SAVED HER A SILVER SEAT.", "SHE BRINGS ENOUGH SNACKS FOR FIVE.",
            "HER FOUR COUSINS ARRIVE LATE.", "SHE SAVES THEIR FAVOURITES."},
        {"HE PAINTS THE SKY WHILE IT SLEEPS,", "ONE GREEN RIBBON AT A TIME.",
            "HIS SISTERS ADD THE PURPLE.", "NOBODY SIGNS THEIR WORK."},
        {"SHE KEEPS EVERY FAMILY WISH,", "POLISHED IN A LITTLE POCKET.",
            "WHEN ONE COMES TRUE SHE CHEERS.", "SHE NEVER SAYS WHICH ONE."},
        {"HE JIGGLES WHEN THE LID OPENS,", "THEN HIS BROTHERS JIGGLE TOO.",
            "THE WHOLE FAMILY CALLS IT MUSIC.", "THE JUICE CALLS IT A TIDAL WAVE."},
        {"SHE STACKS HER LITTLE SISTERS", "IN A PERFECT BERRY TOWER.",
            "THE FAMILY HOLDS ITS BREATH.", "THE TOP ONE ALWAYS SNEEZES."},
        {"HE KEEPS THE FAMILY SEEDS", "FLOATING IN HIS CLEAR BELLY.",
            "HIS COUSINS COUNT THEM DAILY.", "THE ANSWER CHANGES WHEN HE WAVES."},
        {"SHE CATCHES SUN FOR HER BROTHER", "AND SAVES IT IN EACH CORNER.",
            "AT BEDTIME THE FAMILY GLOWS.", "NOBODY ADMITS THEY ARE AWAKE."},
        {"HIS DAD TAUGHT HIM TO FIZZ.", "HIS MUM TAUGHT HIM TO WOBBLE.",
            "NOW THE WHOLE FAMILY SHAKES", "BEFORE ANYONE OPENS THE CRATE."},
        {"SHE LIVES BEHIND THE HIGH SCORE", "WITH THREE GLITCHY COUSINS.",
            "THE FAMILY RESETS EACH NIGHT.", "SHE REMEMBERS EVERY GAME."},
        {"HE SAVES BUBBLES FOR HIS SISTER", "IN ALL EIGHT SOFT CORNERS.",
            "THE FAMILY POPS THEM AT BATHS.", "HE MAKES EIGHT MORE BY MORNING."},
        {"SHE TURNS ORANGE AT SUPPER", "SO HER BROTHERS KNOW TO COME IN.",
            "THE WHOLE FAMILY IS ALWAYS", "HOME BEFORE HER LAST CORNER DIMS."},
        {"HE WEARS THE FAMILY CROWN,", "THOUGH IT KEEPS SINKING INSIDE.",
            "HIS COUSINS FISH IT BACK OUT.", "HE BOWS AND LOSES IT AGAIN."},
        {"SHE WOBBLES THE PICNIC TIN", "UNTIL HER COUSINS COME RUNNING.",
            "THE FAMILY BRINGS THE SPOONS.", "SHE BRINGS THE JELLY LAUGH."},
    };

    /** Which vignette plays over each story. */
    static final int[] BEAT = {
        HANDOFF, PUSH, CHEER, HANDOFF, CHEER, PEEK, CHEER, SEEK, PICNIC, STACK,
        SEEK, STACK, CARRY,
        STACK, PICNIC, TUMBLE, STACK, CARRY, PEEK, SEEK, PICNIC, CHEER,
        PUSH, PUSH, PICNIC, CARRY, STACK, CARRY, BOUNCE, SEEK,
        SEEK, CARRY, PICNIC, CHEER, HANDOFF,
        BOUNCE, STACK, PEEK, CARRY, CHEER, SEEK, BOUNCE, HANDOFF, STACK, PICNIC,
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
        31, 30, 34, 32, 33,
        36, 35, 38, 37, 40, 39, 42, 41, 44, 43,
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
        32, 33, 30, 34, 31,
        37, 38, 39, 40, 41, 42, 43, 44, 35, 36,
    };

    static int third(int i) {
        return THIRD[i];
    }
}
