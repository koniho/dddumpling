# DDDUMPLING app icon

The active icon is drawn directly by `Kawaii.draw`, using the game's dumpling,
facial expression and dark purple background, with a slime-green (#9EE65B) body. No generated
illustration is used in the app or store export.

Regenerate after compiling the harness (`./check.sh -q -r`):

```sh
java -cp build/harness com.dddumpling.game.AppIcon
magick app-store/google-play/icon.png -alpha on -strip PNG32:app-store/google-play/icon.png
```

Outputs:
- `app-store/google-play/icon.png`: 512px opaque RGBA Play Store icon.
- `res/drawable-nodpi/dumpling_icon_legacy.png`: 192px legacy launcher icon.
- `res/drawable-nodpi/dumpling_icon_foreground.png`: 432px adaptive layer;
  character fits the central 66/108 safe area, background matches the back layer.

`dumpling-source.png` and `prompt.txt` preserve the rejected generated concept
only; neither is consumed by a build.
