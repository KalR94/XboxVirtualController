# Setup Xenia Canary (Emulator Xbox 360) — Forza dkk.

Alur input lengkap:

```
HP (app) → USB → adb reverse → Bridge Windows (--pad) → ViGEm virtual Xbox 360 → Xenia Canary
```

Xenia Canary membaca input via **XInput**, jadi cukup buat controller
virtual Xbox 360 di Windows — tidak butuh Xbox fisik.

## 1. Install driver ViGEmBus di laptop Windows

```
pip install vgamepad
```
Installer ViGEmBus driver otomatis muncul saat instalasi — accept & install.
(Driver ini yang bikin Windows mengenali "controller Xbox 360 virtual".)

Alternatif manual: https://github.com/ViGEm/ViGEmBus/releases

## 2. Jalankan bridge dengan virtual pad

```
cd windows
python server.py --pad
```

Log yang benar: `Virtual Xbox 360 controller ACTIVE (ViGEmBus)`.
Cek di **joy.cpl** (Windows Game Controllers) — harus muncul
"Controller (XBOX 360 For Windows)".

Tanpa `--pad`, bridge tetap jalan sebagai monitor-only (seperti sebelumnya).

## 3. Setup Xenia Canary

1. Download xenia_canary dari https://github.com/xenia-canary/xenia-canary/releases
2. Buka `xenia-canary.config.toml`, bagian HID:
   ```toml
   hid = "xinput"     # default 'any' juga bisa, virtual pad ini XInput
   ```
3. Jalankan game (default.xex / game ISO).

## 4. Setup HP

1. Install APK debug terbaru ke HP
2. `adb reverse tcp:8765 tcp:8765`
3. Buka app → Console Selection (pilih Xbox 360 — Xenia emulasi 360) →
   Open Controller (Local testing OFF)
4. Sentuh kontrol → harus langsung terasa di Xenia.

## Catatan performa & tips

- Latensi: input dikirim ≤60 Hz via USB — biasanya cukup responsif.
- Forza Horizon 1/2 di Xenia butuh setting khusus (cek wiki xenia-canary
  per game). Beberapa title butuh `clearMemoryPageMappings = true` dll.
- Jangan jalanin Steam Input / DS4Windows bersamaan — bisa bikin
  input dobel (Player 1 & 2).
- Trigger analog LT/RT diteruskan penuh (0.0–1.0).
