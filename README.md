# SmartWaste: Smart recycling for a sustainable future 🇸🇬

> *"Every recycled item becomes a data point for a cleaner Singapore."*

SmartWaste is an Android app that makes e-waste recycling in Singapore **easy, safe and rewarding**. Snap a photo of an old gadget and AI identifies it. The app tells you how to prepare it safely and finds the nearest bin that isn't full, or a company that will buy it. It then navigates you there turn by turn and rewards you with points once your deposit is verified.

**➡️ Setup instructions: [INSTALLATION.md](INSTALLATION.md)**

Team: Randy Chong Haw, Oh Jun Long, Poh Wei Lun · Advisor: Mary Lay

---

## Features

| | Feature | What it does |
| --- | --- | --- |
| 📷 | **AI e-waste scanner** | Cloud vision AI (OpenRouter, with an automatic multi-model fallback chain) identifies the item, brand, smart-bin compartment, hazard level, battery presence, recoverable materials and resale value. If the cloud is unreachable, an **on-device ML Kit backup AI** takes over and you can correct the category. |
| 🗺️ | **Live recycling map** | Modern vector map (MapLibre + OpenFreeMap). Shows bins and buyers from **every operator**, filtered by item category. Full bins are skipped automatically. |
| 🧭 | **Turn-by-turn navigation** | Walk, cycle or drive routes with ETA, step list, live follow-mode, off-route re-routing and arrival detection. One tap hands off to Google Maps voice navigation. |
| 💰 | **Sell your e-waste** | A researched directory of Singapore buyers and recyclers (trade-in shops, scrap/e-waste recyclers, online buyers with doorstep pickup) with addresses, phone numbers and websites. |
| ✅ | **Verified deposits → points** | At the bin: (1) GPS within 150 m **or** scan the SmartWaste bin QR, (2) AI-checked photo proof, (3) scan the **barcode / serial number / IMEI**. These are checked online, then points are awarded. Verified items earn up to **2×** points. Skipping verification earns fewer. |
| 🎁 | **Rewards** | Balance, tiers, a rewards catalogue, redemption with voucher codes, and a "My vouchers" history. |
| 🌱 | **Impact dashboard** | Items recycled, kg diverted, CO₂e avoided, per-category breakdown and full deposit history. |
| 💬 | **AI support button** | A small floating button in the bottom-right corner opens an AI chat for e-waste and app questions. It falls back to an offline FAQ. |
| 🔒 | **Transparency & anti-fraud** | Points formula shown to users, mock-GPS blocking, one-time code use (hashed), QR distance check, and a daily deposit cap. |

The UI follows the reference designs: soft neutral canvas, rounded 24–32 dp cards, a dark floating pill navigation bar, pill chips, gradient hero cards, and a dark-navy scanner screen.

---

## How it works

```
 Scan item ─▶ AI identifies category & hazards ─▶ Nearest non-full bin / buyer ─▶ Navigate
                                                                                    │
 Rewards ◀─ Points (formula below) ◀─ Verify code online ◀─ Photo proof ◀─ GPS / bin QR
```

### Points formula (shown in-app)

```
base         = category points × quantity        (battery 10 · component 30 · appliance 40 · device 50 · lamp 15)
location     = × 1.2 with SmartWaste bin QR, × 1.0 with GPS
photo        = × 1.0 if AI confirms the bin photo, × 0.6 otherwise
verification = + base × 100% (barcode found online) | 80% (valid IMEI / barcode checksum) | 50% (plausible serial) | 0% (skipped)
```

Example: a phone with an online-verified barcode at a GPS-confirmed bin earns **100 pts**. Skipping verification earns **50 pts**.

### Item verification

| Code | Check |
| --- | --- |
| EAN / UPC barcode | GS1 check digit → [UPCitemdb](https://www.upcitemdb.com/) lookup → [Open Products Facts](https://world.openproductsfacts.org/) lookup |
| IMEI (15 digits) | Luhn checksum (GSMA standard) |
| Serial number | Format check plus AI plausibility check against the detected brand/model |
| Any code | SHA-256 hash stored, so the same item can't be claimed twice |

---

## Data sources

| Source | Used for |
| --- | --- |
| [OpenStreetMap](https://www.openstreetmap.org/copyright) via [Overpass API](https://overpass-api.de/) | Live recycling points tagged for electronics, batteries and bulbs, plus second-hand electronics shops |
| [NEA E-waste Recycling dataset](https://data.gov.sg/dataset/e-waste-recycling) (optional) | Official collection points |
| [OneMap](https://www.onemap.gov.sg/) (SLA) | Geocoding the bundled directory's postal codes |
| [OpenFreeMap](https://openfreemap.org/) | Vector map tiles (© OpenMapTiles, © OpenStreetMap contributors) |
| [FOSSGIS OSRM](https://routing.openstreetmap.de/) | Walking, cycling and driving routes |
| Bundled directory | Researched malls, retailers, supermarkets, SGRecycle bins and buyers (Oct 2026). Coordinates are refined at runtime. |

Places, hours and buyer terms change, so check [recycle.gov.sg](https://www.recycle.gov.sg/) or call ahead.

---

## Tech stack

Kotlin · Jetpack Compose (Material 3) · Navigation Compose · Room · DataStore · CameraX · ML Kit (barcode, text recognition, image labelling) · Fused Location · MapLibre Native · OkHttp · kotlinx.serialization · Coil

```
app/src/main/java/sg/smartwaste/app/
├── data/
│   ├── ai/          OpenRouter client, prompts, on-device backup classifier
│   ├── places/      Multi-source bin & buyer aggregation, curated directory
│   ├── routing/     OSRM turn-by-turn routing
│   ├── verify/      Barcode / IMEI / serial verification
│   ├── rewards/     Points engine, rewards catalogue, tiers
│   ├── db/          Room entities & DAOs
│   └── location/    Fused location + mock detection
└── ui/
    ├── home/ scan/ map/ deposit/ rewards/ profile/ chat/ onboarding/
    ├── camera/      CameraX + ML Kit analyzers
    └── components/  Design-system components
```

---

## Current limitations

- **Smart-bin telemetry** (fill level) for the SmartWaste pilot bins is simulated until the Arduino/IoT backend is connected.
- **Points and vouchers are stored on the device.** A production deployment needs a backend to issue vouchers and run server-side anti-fraud. That backend should also hold the AI key.
- The **rewards catalogue** is a demo. Partner names are placeholders.
