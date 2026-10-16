<p align="center">
  <img src="assets/icon.png" width="96" height="96" alt="King Calc Watch icon">
</p>

<h1 align="center">King Calc Watch</h1>

<p align="center">
  A calculator and currency converter for Wear OS — built for the Galaxy Watch 8 Classic's round display.<br>
  Part of the <a href="https://teamexykings.in">TeamExyKings</a> app family, from the makers of
  <a href="https://github.com/yashwanthramsomireddy/KingCalc">King Calc</a> for Android.
</p>

---

## What it is

King Calc Watch is a standalone Wear OS app — it installs and runs entirely on the watch,
with no companion phone app required. It has three tools, side by side (swipe left/right):

- **Calculator** — add, subtract, multiply, divide, percent. *Compact* mode (default) fits
  one glance; *Expanded* mode adds memory (MC, MR, M−, M+), √, x² and 1/x and scrolls with the
  rotating bezel/crown. A clock button opens your recent calculations, with a Clear button.
- **Currency converter** — full list of world currencies with starred favourites and an A–Z
  jump, live rates, and the last rates are saved so it still works offline. Type an amount and
  see it converted into every currency at once.
- **Unit converter** — length, weight, temperature, area (including the Indian cent, guntha and
  ground), volume, speed, time and data, with every unit shown at once.

Settings (gear icon) has Pitch Black / White / System themes, accent colors, key style
(Round or Classic), the Compact/Expanded switch, and an optional keep-screen-on setting
(off by default, as it uses more battery).

- **Standalone** — no phone bridge needed
- **Calculator and units work fully offline**; only the currency converter uses the internet, to fetch public rates
- **No backend, no ads, no tracking, no accounts**
- **Free and open source** (MIT)

## Why a separate app?

Wear OS's round UI can't be rendered by the React Native/Expo stack King Calc on
Android is built with, so the watch app is a small, independent native Kotlin +
Jetpack Compose for Wear OS project rather than an update to King Calc itself.

## Screens

- **Calculator** — a running-expression line, the main display, and a 4-column key grid
  (C ± % ⌫ / 7 8 9 ÷ / 4 5 6 × / 1 2 3 − / 0 . = +) sized to the round display, plus History
  and Settings buttons.
- **Currency converter** — pick the From and To currencies, type an amount on the keypad
  (red C clears it), and see the result plus every other currency below.
- **Unit converter** — pick a category and From/To units; every unit in the category is listed.
- **Settings** and **About**.

## Source code

The full source is in this repository, under the MIT license — read it, build it, fork it.
Written in Kotlin with Jetpack Compose for Wear OS. See *Building from source* below.
Issues and pull requests are welcome.

## Device support

Built and tested for the Samsung Galaxy Watch 8 Classic (SM-L500, Wear OS 6 / One UI
Watch 8). Runs on any Wear OS 3+ (API 30+) watch, such as the Galaxy Watch 4 and newer.
It does not run on older Tizen watches (Gear S3, Galaxy Watch 3 and earlier).

## Privacy

See [`privacy-policy.html`](privacy-policy.html) — in short: the app collects nothing.
The only network use is the currency converter downloading public exchange rates
(Internet permission); no personal data is sent.

## License

MIT — see [`LICENSE`](LICENSE).

## Contact

Yashwanth Ram Somireddy — [yashwanthramsomireddy@gmail.com](mailto:yashwanthramsomireddy@gmail.com)

---

## Building from source

1. Install [Android Studio](https://developer.android.com/studio) (JDK 17 is bundled) and open this folder; let Gradle sync.
2. No API keys or accounts are needed — the converter uses ExchangeRate-API's free open endpoint
   (`open.er-api.com`); attribution is shown in About.
3. Run on a Wear OS emulator (API 30+) or on a watch over Wi-Fi debugging.
4. Unit tests (calculator engine, currency and unit maths): `./gradlew testDebugUnitTest`

Kotlin note: the package `in.teamexykings.kingcalcwatch` starts with `in`, a Kotlin keyword,
so every `package`/`import` line spells it with backticks: `` `in`.teamexykings… ``.
