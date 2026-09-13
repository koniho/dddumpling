# iOS listing and media

App Store Connect app: `6811478003` (`com.dddumpling.game.ios`). English text is in
`en-US/`. Private Apple credentials and reviewer contact details do not belong here.

## Saved in App Store Connect

- Version 0.1.0, build 2.1: processed VALID; ready for internal TestFlight testing and
  external beta submission. [Signed CI/upload run](https://github.com/koniho/dddumpling/actions/runs/34732005221).
- Public description, subtitle, keywords, promotional text, support/marketing/privacy URLs.
- Games category with Casual and Action subcategories; source-based age-rating answers.
- Free pricing in all 175 territories and future territories. Regional eligibility still
  depends on Apple's requirements; EU territories report missing trader status.
- Build attached to the draft App Store version; release mode is manual.
- TestFlight beta description, feedback address, and What to Test instructions.
- Reviewer contact, review notes, and no-login-required setting in both App Store and
  TestFlight sections, using the contact the account holder entered in App Store Connect.
- Licensed third-party content declaration, including the bundled font and notices.
- Real Release boss and gameplay screenshots, 1320 × 2868, processed COMPLETE by Apple.
  Apple's API calls the current large-iPhone screenshot set `APP_IPHONE_67`.

## Still required from the account holder

- App Privacy: complete the website questionnaire. The current offline iOS implementation
  does not collect data for the developer; the source audit supports “No, we do not collect data
  from this app.” This questionnaire is not exposed by the public API used here.
- EU trader status: complete Business → Compliance → Digital Services Act in App Store Connect.
  Free pricing alone does not determine trader status.

No public App Store review/release or external tester invitations have been submitted.

## Local video

The ignored `ios/build/videos/` directory contains:

- `DDDumpling-playthrough.mp4`: original automated simulator recording, about 657 MB.
- `DDDumpling-automated-preview-silent.mp4`: shareable H.264 MP4, 720 × 1564,
  4 minutes 11 seconds, about 48 MB, fast-start enabled.
- `DDDumpling-automated-preview-contact-sheet.png`: representative frames.

This is a **partial, silent automated preview**, not a complete playthrough. It shows normal
play, a powerup, Starpath, and Slime; it does not demonstrate all bosses, both minigames, and
the completed collection. The original recording is preserved. A full human-recorded
walkthrough will be recorded by the user; no App Store preview video has been uploaded.

For a phone recording, start Screen Recording from Control Center, leave the microphone off
unless narration is wanted, and confirm the game audio is audible in a short sample first.
Record the intro, regular play, a powerup, both minigames, boss encounters, and the display case.
The video saves to Photos and can be transferred to the Mac for editing. Keep the original;
a separate 15–30 second cut can be made for an App Store preview.

References: [iPhone screen recording](https://support.apple.com/en-ca/guide/iphone/iph52f6e1987/ios),
[App Store preview specifications](https://developer.apple.com/help/app-store-connect/reference/app-information/app-preview-specifications).
