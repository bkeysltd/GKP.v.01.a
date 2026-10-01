# GKP — Keep → GPT

Version **v.03.a.00**

This branch is a new direction for GKP. The old PDF/printing app remains untouched on `main`.

## Workflow

Google Keep → **Share** → **GKP** → **ASK GPT** → answer shown inside GKP.

GKP accepts plain text from Google Keep (including a different Google account on the same Android phone).

## Security

The Android app does **not** contain an OpenAI API key.

It sends the note to a small HTTPS backend. The backend holds `OPENAI_API_KEY` as a server-side environment variable and calls the OpenAI Responses API.

## First setup

1. Deploy the `backend` folder to any Node.js HTTPS host.
2. Add environment variable `OPENAI_API_KEY`.
3. Start command: `npm start`.
4. Copy the public HTTPS URL and add `/ask`.
5. In GKP, paste that URL under **CONNECTION** and tap **SAVE CONNECTION**.

## Android use

1. Open a note in Google Keep.
2. Tap **Share**.
3. Choose **GKP**.
4. Check/edit the text.
5. Tap **ASK GPT**.
6. The GPT answer appears in the app.

## Build APK

Open **Actions → Build GKP v.03 APK → latest run → Artifacts** and download `GKP-v.03.a.00-APK`.
