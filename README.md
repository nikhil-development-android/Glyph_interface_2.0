# Glyph Interface (Nothing Phone LED Controller)

A native Android root application engineered for Nothing Phones to control and synchronize the rear **Glyph LED Interface** via direct hardware sysfs control (`aw20036_led`).

---

## ⚡ Direct Sysfs Hardware Protocol Mapping

| Feature | Sysfs Hardware Command |
| :--- | :--- |
| **Main System ON** | `echo 1 > /sys/class/leds/aw20036_led/operating_mode` |
| **Main System OFF** | `echo 0 > /sys/class/leds/aw20036_led/operating_mode` |
| **Torch / All Brightness ON** | `echo 255 > /sys/class/leds/aw20036_led/all_brightness` |
| **Torch / All Brightness OFF** | `echo 0 > /sys/class/leds/aw20036_led/all_brightness` |
| **Medium Brightness** | `echo 125 > /sys/class/leds/aw20036_led/all_brightness` |
| **Low Brightness** | `echo 50 > /sys/class/leds/aw20036_led/all_brightness` |
| **Single LED Control** | `echo "LED_INDEX BRIGHTNESS" > /sys/class/leds/aw20036_led/single_brightness` |
| **Mini LED No-2** | `echo "20 255" > /sys/class/leds/aw20036_led/single_brightness` |
| **Medium LED No-3** | `echo "33 255" > /sys/class/leds/aw20036_led/single_brightness` |

### 8-Step Ladder LED Mapping (0 to 31):
- **Step 0**: LEDs 0, 12, 24
- **Step 1**: LEDs 1, 13, 25
- **Step 2**: LEDs 2, 14, 26
- **Step 3**: LEDs 3, 15, 27
- **Step 4**: LEDs 4, 16, 28
- **Step 5**: LEDs 5, 17, 29
- **Step 6**: LEDs 6, 18, 30
- **Step 7**: LEDs 7, 19, 31

---

## 📱 Hardware Features Implemented

1. **Master Power Toggle**: Controls `operating_mode` (1 on / 0 off).
2. **Brightness Slider & Presets**: Controls linear PWM on `all_brightness`.
3. **Volume Indicator (Live Steps)**:
   - Scales device volume into 8 steps across LEDs 0–31 (`single_brightness`).
   - Automatically dims after 2.5 seconds.
4. **Glyph Countdown Timer**:
   - Every second, Mini LED 20 blinks (`echo "20 255" > single_brightness`).
   - Remaining time diminishes from Step 7 (31) down to Step 0 (0).
   - On completion: 3x full flash using `all_brightness`.
5. **Call Ringtones (20+ Patterns)**:
   - Plays synchronized patterns across 0-31 ladder, LED 20, and LED 33.
6. **Notification Alert**:
   - Double flash pulse on `all_brightness`.
7. **Flip to Glyph**:
   - Detects face-down placement via accelerometer & proximity.
   - Sets phone to Silent mode and plays 1.5s cascade animation.
   - When a notification arrives while face down: LED 33 blinks for 2 seconds.
8. **Music Progress Bar**:
   - Real-time media playback progress mapped across LEDs 0-31.
9. **3-Band Music Visualizer**:
   - **Bass**: Dynamic response on LEDs 0-31.
   - **Vocal**: Mini LED 20 blinks on vocal/mid frequencies.
   - **Instruments**: Medium LED 33 blinks on treble/high frequencies.
   - Smooth 65ms throttling so hardware LEDs have visible pulse duration.
10. **Charging Indicator**:
    - Connected: Displays battery percentage across LEDs 0-31 for 3 seconds.
    - Disconnected: Blinks 2 times using `all_brightness`.

---

## 🛠️ MT Manager Pro Editing Guide (No Coding Required)

- **Strings & Titles**: `res/values/strings.xml` or `resources.arsc` -> String Pool.
- **Colors**: `res/values/colors.xml` (`glyph_red`, `glyph_bg_black`, `glyph_status_green`, etc.).
- **Card Drawables**: `res/drawable/bg_glyph_card.xml`.
- **CSV Patterns**: `assets/call/` & `assets/notification/`.

---

## 🚀 How to Export or Push to GitHub

1. Click on the **Settings ⚙️** icon in the bottom right corner of the AI Studio screen.
2. Select **"Push to GitHub"** to publish to your GitHub repository.
3. Select **"Download APK"** or **"Export Project ZIP"** to build locally.
