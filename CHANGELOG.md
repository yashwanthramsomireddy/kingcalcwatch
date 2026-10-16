# Changelog

All notable changes to King Calc Watch are documented here.
Format loosely follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [1.0.0] - 2026-10-16

First release.

### Added
- Standalone Wear OS app (Kotlin + Jetpack Compose for Wear OS), no phone app needed
- **Calculator** — add, subtract, multiply, divide, percent, backspace, sign toggle
  - **Compact mode** (default): the 20-key calculator always fits one screen, no scrolling
  - **Expanded mode**: memory (MC, MR, M−, M+), square root, square and reciprocal; scroll with the rotating bezel/crown
  - **History**: clock button with the last 50 results and a Clear button, kept on the watch
- **Currency converter** (swipe left from the calculator)
  - Full list of world currencies; live rates from ExchangeRate-API's free open endpoint (no API key); last rates cached for offline use
  - Starred favourites pinned at the top, A–Z jump, flags
  - The typed amount is converted into every currency at once; the same currency can't be on both sides
  - INR on the left and USD on the right by default for India, USD and EUR elsewhere
  - Red **C** button clears the amount in one tap
- **Unit converter** (swipe left again): length, weight, temperature, area (including cent, guntha and ground), volume, speed, time and data; every unit shown at once
- **Settings** (gear icon): theme (Pitch Black / White / System), accent color swatches, Compact/Expanded switch, key style (Round or Classic), optional keep-screen-on (off by default, with a battery caution)
- **About** screen with credits, ExchangeRate-API attribution and the teamexykings.in address
- Launcher icon matching King Calc

### Notes
- The calculator and unit converter are fully offline. Only the currency converter uses the network (Internet permission).
- No ads, no tracking, no accounts.
