# Installation Guide: SmartWaste (Android)

This guide takes you from a fresh clone to running SmartWaste on a phone or emulator with **Android Studio**.

---

## 1. Requirements

| Item | Version / notes |
| --- | --- |
| Android Studio | Ladybug (2024.2) or newer. Any current stable release works. |
| JDK | 17 or newer. Use Android Studio's bundled JBR (default). |
| Android SDK | Platform **35** (Android 15). Android Studio installs it on first sync. |
| Device | Android **8.0 (API 26)+** with Google Play services. A **physical phone** is recommended for the camera, GPS and barcode scanning. |
| Internet | Needed for AI, the map tiles, routing and online barcode verification. Offline fallbacks exist. |
| OpenRouter API key | Get one at <https://openrouter.ai/keys>. |

---

## 2. Get the code

```bash
git clone https://github.com/Howtoplay-Games/SmartWaste-App.git
cd SmartWaste-App
```

Or in Android Studio: **File ▸ New ▸ Project from Version Control…** and paste the repository URL.

---

## 3. Open the project in Android Studio

1. **File ▸ Open…** and select the `SmartWaste-App` folder (the one containing `settings.gradle.kts`).
2. Wait for **Gradle sync** to finish. The first sync downloads Gradle 8.11.1 and all libraries, which takes a few minutes.
3. If Android Studio offers to **upgrade the Android Gradle Plugin**, you can click *Remind me later*. The project builds as-is.
4. If sync complains about the JDK, go to **Settings ▸ Build, Execution, Deployment ▸ Build Tools ▸ Gradle** and set **Gradle JDK** to *jbr-17* (or any JDK 17+).

---

## 4. Add your OpenRouter API key (required for cloud AI)

The key is a **secret**. It goes in `local.properties`, which is git-ignored and never committed.

1. Open `local.properties` in the project root. Android Studio creates it automatically with your `sdk.dir`. If it doesn't exist, copy `local.properties.example` to `local.properties`.
2. Add this line:

   ```properties
   OPENROUTER_API_KEY=sk-or-v1-your-key-here
   ```

3. **Sync Gradle** (elephant icon, or *File ▸ Sync Project with Gradle Files*) and rebuild.

> **Security:**
> - **Never** commit the key or paste it into source files. The repository is public, and OpenRouter automatically revokes keys it finds on GitHub.
> - If your key has ever been shared in chat, email or a screenshot, **rotate it** at <https://openrouter.ai/keys>.
> - Any key compiled into an APK can be extracted. Before a public release, route AI calls through your own backend so the key stays on a server.
> - You can also enter or override the key at runtime in **Profile ▸ Settings ▸ AI API key**.

Without a key, the app still works: detection falls back to the **on-device ML Kit** backup AI and the assistant answers from its offline knowledge base.

---

## 5. Run the app

1. Connect an Android phone with **USB debugging** enabled (*Settings ▸ About phone ▸ tap Build number 7 times ▸ Developer options ▸ USB debugging*), or start an emulator from **Device Manager** using a *Google Play* system image.
2. Select the **app** run configuration and your device, then click **Run ▶**.
3. On first launch, enter your name and allow **Camera** and **Location** when asked.

### Command-line build (optional)

```bash
./gradlew assembleDebug                 # builds app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

On Windows use `gradlew.bat assembleDebug`.

---

## 6. Optional configuration (`gradle.properties`)

| Property | Purpose | Default |
| --- | --- | --- |
| `SMARTWASTE_VISION_MODELS` | Ordered vision-model chain used for e-waste detection and bin-photo checks. | `google/gemini-3.5-flash,google/gemini-2.5-flash,openrouter/free` |
| `SMARTWASTE_CHAT_MODELS` | Ordered model chain for the AI support chat and serial-number checks. | same as above |
| `SMARTWASTE_DATAGOV_EWASTE_DATASET_ID` | Adds NEA's official *E-waste Recycling (GEOJSON)* points from data.gov.sg. | empty (off) |

**Model IDs:** OpenRouter adds and retires models regularly. If a model is retired, the next one in the chain is used automatically. To change models, pick IDs from <https://openrouter.ai/models> (filter by *image input* for the vision chain).

**NEA dataset id:** open <https://data.gov.sg/dataset/e-waste-recycling>, click **API**, copy the dataset id (looks like `d_…`), and set it in `gradle.properties` or `local.properties`.

---

## 7. Testing tips

### Location on an emulator
Open the emulator's **⋯ ▸ Location** panel, search for a Singapore address (e.g. *Ngee Ann City*), and press **Set location**. To earn points via GPS you must be within 150 m of a bin.

### SmartWaste smart-bin QR codes
The pilot smart bins accept a QR code whose text is:

```
SMARTWASTE:BIN:SW-001     (Our Tampines Hub)
SMARTWASTE:BIN:SW-002     (Toa Payoh HDB Hub)
SMARTWASTE:BIN:SW-003     (JEM Jurong East)
SMARTWASTE:BIN:SW-004     (Waterway Point)
SMARTWASTE:BIN:SW-005     (Woodlands Civic Centre)
```

Generate one with any QR generator, then use **Scan ▸ Scan bin QR**. A QR scanned more than 2 km from its bin is rejected (anti-fraud). Set your emulator location nearby when testing.

### Item verification
- **Barcode:** scan the EAN/UPC on the product box. It's checked against UPCitemdb and Open Products Facts.
- **IMEI:** dial `*#06#` on the old phone and scan or type the 15-digit IMEI. It's validated with the Luhn checksum.
- **Serial number:** point the camera at the "S/N" label (on-device OCR), or type it in.

---

## 8. Troubleshooting

| Problem | Fix |
| --- | --- |
| *"SDK location not found"* | Make sure `local.properties` contains `sdk.dir=…` (Android Studio adds it on sync). |
| Gradle sync fails downloading dependencies | Check your internet/proxy, then *File ▸ Invalidate Caches ▸ Restart*. |
| Map is blank | The map needs internet (OpenFreeMap tiles). The app automatically retries with a fallback style. |
| Scans say *"On-device backup AI"* | No key, an invalid key, or no credit. Check `OPENROUTER_API_KEY`, then rebuild, or set the key in Profile ▸ Settings. |
| "Mock locations aren't allowed" | Turn off mock-location apps. GPS spoofing is blocked for earning points. |
| Camera preview is black on an emulator | Set the emulator's back camera to *VirtualScene* or *Webcam* in AVD settings, or use a real device. |
