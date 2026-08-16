# Glyph Interface (Nothing Phone LED Controller)

A native Android application engineered for Nothing Phones to control and synchronize the rear **Glyph LED Interface** via direct hardware sysfs control (`aw20036_led`).

---

## 📱 Features

- **Master System Switch**: One-tap toggle for all rear Glyph LEDs.
- **Hardware Brightness Control**: Full-range linear PWM brightness adjustment with automatic ambient light sensor fallback.
- **Interactive Phone Preview**: Live visual representation of active LED zones and states.
- **20+ Built-in Glyph Ringtones & Patterns**: Abra, Anna, Beetle, Clwb, Coded, Crossing, Dolphin, Hammer, Latency, Plot, Pneumatic, Pulse, Radiate, Ripple, Squirrels, Sticks, Tennis, Wings, Wizard, and Woo Yeh.
- **Glyph Visual Timer**: Real-time LED countdown bar with optional alarm ringtone.
- **Dual-Engine Music Visualizer**: 5-frequency-band (Bass to Treble) realtime audio sync via AudioFx and low-latency AudioRecord.
- **Hardware Volume Light**: Volume step indicator lights on device volume change.
- **Flip to Glyph**: Face-down silence mode with custom LED animation trigger.

---

## 🛠️ MT Manager Pro Editing Guide (No Coding Required)

All text, titles, colors, and styling assets are modularized into standard Android XML resource files so anyone can easily modify or customize the APK directly using **MT Manager Pro**:

### 1. Modifying Text, Titles & Labels
Open:
`res/values/strings.xml` (or `resources.arsc` -> `String Pool`)
- Edit app name, feature titles, button labels, and descriptions without modifying any code.

### 2. Modifying Colors & Themes
Open:
`res/values/colors.xml`
- `glyph_red`: Accent color (Default `#FFFF3B30`)
- `glyph_bg_black`: Background pure black (`#FF000000`)
- `glyph_card_dark`: Card surface color (`#FF151515`)
- `glyph_status_green`: Active link status indicator color (`#FF00FF41`)
- `glyph_text_primary`: Main text color (`#FFFFFFFF`)
- `glyph_text_secondary`: Subtitle text color (`#FF888888`)

### 3. Modifying Card Shapes & Borders
Open:
`res/drawable/bg_glyph_card.xml` & `res/drawable/bg_pattern_item.xml`
- Edit `android:radius` for corner roundness.
- Edit `stroke` width and color for card borders.

### 4. Modifying Animation CSV Patterns
Open:
`assets/call/` & `assets/notification/`
- Add or edit CSV frames with 0–4095 brightness values per LED index.

---

## 🚀 How to Export or Push to GitHub

1. Click on the **Settings ⚙️** icon in the bottom right corner of the AI Studio screen.
2. Select **"Push to GitHub"** to publish the full source code directly to your GitHub repository.
3. To generate a release APK or AAB, select **"Download APK"** or **"Export Project ZIP"** to build locally.
