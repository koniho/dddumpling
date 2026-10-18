# Seasonal skins

`PlayerSettings.skin` is a device-local visual preference. The Settings gallery (`SkinSettings`)
selects Default or Halloween immediately, including during paused runs. Bits 19–22 of the existing
player-settings integer store the ID. Old saves decode to Default; unknown skin IDs fall back to
Default without changing volume, mute, kids mode or play mode.

`Skins.painter` carries the selected identity through `Painter.skin()`. Every decorator that wraps
another painter must delegate that method, just as it delegates collectible render contexts.
Do not use global mutable palettes: previews, tutorial practice and multiple game instances can
render different skins in the same process. Tutorial practice copies the preference when created
and follows a selection change while it is open.

Halloween key artwork lives in `HalloweenKeys`, reached through `Kawaii` for normal, surprised,
determined and crying states. Key IDs and input positions remain unchanged: 0 ghost, 1 apple,
2 Jack-o'-lantern, 3 witch, 4 Frankenstein, 5 bat. `Skins.keyColor` supplies the matching key and
prompt palette. Gameplay RNG, matching, collision geometry and progress do not depend on skins.

Boss skins reuse the existing spring-body geometry and animation entry points in `BossScreen`,
`BossCollect` and `AlligatorArt`. Local palette wrappers preserve shading, transparency and
flashes; decorations follow the body pose. Palette wrappers disable cached art stamps where
recoloring each primitive is required. Super Ghost is a display/narration alias: boss IDs,
unlocks and score records stay shared with Super Slime. Both native sound backends support the
skin-aware versus announcement while retaining the older default-skin overload.

To extend: add a stable skin ID/name and key palette, add artwork at these shared entry points,
then extend the gallery layout if more than two choices are needed. Never repurpose an existing
ID or duplicate gameplay rules for a new skin. Check every painter decorator, key expression,
paused selection, tutorial, four/six-key deck, special enemy prompt and boss portrait/death state.
`TestSettings` covers selection, persistence, tutorial inheritance, render-context isolation and
deterministic gameplay parity across Adventure, Survival, Time Attack and all five bosses;
`Preview` exports the `200-` key sheet, gallery, encounters and boss sheet for visual review.
