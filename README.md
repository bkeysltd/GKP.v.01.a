# GKP — Visible Keep → GPT

Version **v.04.a.00**

This branch changes GKP so the user does not need to copy, paste, share, or save a Google Keep note into GKP.

## Workflow

1. Enable GKP under Android **Accessibility** once.
2. Open Google Keep and open the note.
3. GKP reads only text currently visible in the Google Keep window.
4. Return to GKP.
5. Tap **CHECK VISIBLE NOTE WITH GPT**.
6. GPT's answer appears inside GKP.

The accessibility service is restricted in its Android configuration to the Google Keep package `com.google.android.keep`.

## GPT connection

The OpenAI API key is not stored in the Android APK. GKP connects to the secure backend already included in this repository.

## Build APK

Open **Actions → Build GKP v.04 APK → latest run → Artifacts** and download `GKP-v.04.a.00-APK`.
