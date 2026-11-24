# XboxVirtualController

Proyek eksperimen: menjadikan **perangkat Android sebagai gamepad virtual** yang mengirim input ke **Windows** melalui **USB (ADB)**, lalu Windows mengonversi input tersebut menjadi **virtual gamepad** (ViGEm/XInput) yang bisa dipakai game PC.

> ⚠️ **DISCLAIMER PENTING (jujur):** Proyek ini **TIDAK** membuat konsol Xbox (One/Series/360) mengenali perangkat Android sebagai controller. Konsol Xbox melakukan autentikasi proprietary per-controller (handshake terenkripsi) sehingga gamepad virtual/sintetis **tidak akan pernah otomatis dikenali** konsol. Target yang benar-benar berfungsi adalah **PC Windows** (via virtual gamepad). Lihat [docs/compatibility.md](docs/compatibility.md) untuk detail per generasi konsol.

## Arsitektur (overview)

```
[Android (Kotlin/Compose)] --USB kabel--> [ADB] --adb reverse tcp:8765--> [Windows Python TCP Server :8765] --> [Virtual Gamepad (Windows)]
```

1. **Android** membaca input dari UI gamepad (Compose) → diserialisasi menjadi JSON (newline-delimited) → dikirim melalui socket TCP.
2. **USB + ADB**: Android dihubungkan ke PC Windows dengan kabel USB. Perintah `adb reverse tcp:8765 tcp:8765` membuat port 8765 di HP diteruskan ke port 8765 di PC, sehingga HP bisa membuka koneksi TCP ke `127.0.0.1:8765`-nya sendiri dan trafiknya sampai ke server di Windows — **tanpa Wi-Fi** dan dengan latensi sangat rendah.
3. **Windows (Python)** menerima JSON → `input_parser` → `controller_state` → menggerakkan virtual gamepad (mis. ViGEm Bus / vJoy) sehingga game PC melihat sebuah "Xbox controller" (perangkat **Windows**, bukan konsol).
4. **monitor.py** menampilkan log input secara real-time untuk debugging.

## Instalasi & Build

### Android (Android Studio, APK debug)

1. Install [Android Studio](https://developer.android.com/studio) (minimal Hedgehog disarankan) + SDK 34.
2. Buka folder `android/` di Android Studio (File → Open).
3. Tunggu Gradle Sync selesai.
4. Aktifkan Developer Options di HP: Settings → About phone → ketuk "Build number" 7×, lalu Settings → Developer options → aktifkan **USB debugging**.
5. Sambungkan HP ke PC dengan kabel USB, pilih **File Transfer (MTP)** bila diminta, izinkan dialog "Allow USB debugging".
6. Build & install APK debug:
   - Dari Android Studio: pilih device → Run ▶ (membangun `app-debug.apk` otomatis), atau
   - Dari terminal:
     ```bash
     cd android
     ./gradlew assembleDebug
     adb install -r app/build/outputs/apk/debug/app-debug.apk
     ```

### Windows (Python venv + server + monitor)

```powershell
cd windows
python -m venv .venv
.\.venv\Scripts\activate
pip install -r requirements.txt
# jalankan server TCP (default port 8765)
python server.py
# di terminal lain, pantau input yang masuk
python monitor.py
```

### Menyambungkan Android → Windows (ADB reverse)

```powershell
adb devices                 # pastikan HP terdeteksi
adb reverse tcp:8765 tcp:8765
```

Setelah itu di aplikasi Android tekan **Connect** — aplikasi membuka `127.0.0.1:8765` yang diteruskan ke server Windows. Detail + troubleshooting: [docs/usb-setup.md](docs/usb-setup.md).

## Struktur Folder

```
XboxVirtualController/
├── README.md
├── docs/
│   ├── architecture.md     # diagram, alur data, komponen, format JSON
│   ├── usb-setup.md        # panduan ADB reverse lengkap Windows
│   ├── compatibility.md    # kompatibilitas per generasi Xbox (jujur)
│   └── development.md      # build, test, kontribusi, conventions
├── android/
│   └── app/src/main/java/com/haikal/xboxcontroller/
│       ├── navigation/     # NavHost & routing layar
│       ├── ui/             # layar & komponen Compose (gamepad UI)
│       ├── connection/     # ConnectionManager, UsbClient (TCP via ADB)
│       ├── console/        # ConsoleProfile (mapping tombol per konsol/target)
│       ├── data/           # repository & model input
│       └── viewmodel/      # ViewModel tiap layar
└── windows/
    ├── server.py           # TCP server newline-delimited JSON
    ├── input_parser.py     # validasi & parsing JSON → struktur input
    ├── controller_state.py # state gamepad + konversi ke virtual gamepad
    ├── connection_manager.py # sesi klien, reconnect, dedupe koneksi
    ├── monitor.py          # CLI real-time viewer
    └── tests/              # pytest
```

## Roadmap

| Versi | Fitur | Status |
|---|---|---|
| **V1** | Android gamepad UI (Compose), koneksi USB via ADB reverse ke Windows, JSON input 11 tombol + 2 stick + 2 trigger, virtual gamepad di Windows, monitor CLI | ✅ Selesai |
| **V2** | Profil tombol per game (ConsoleProfile), rumble haptic feedback, auto-connect saat USB plug-in, konfigurasi sensibilitas stick/deadzone, rotaasi layar landscape | 🚧 Dalam pengembangan |
| **V3** | Output langsung ke konsol Xbox: riset protokol **GIP/XGIP**, opsi hardware tambahan (USB host shield + Xbox Wireless Adapter), atau proxy controller fisik | 🔬 **Riset & dokumentasi saja — TIDAK diimplementasikan** (lihat [docs/compatibility.md](docs/compatibility.md)) |

## Dokumentasi

- [Arsitektur & Format JSON](docs/architecture.md)
- [Setup USB / ADB Reverse](docs/usb-setup.md)
- [Kompatibilitas Konsol (jujur, per generasi)](docs/compatibility.md)
- [Panduan Pengembangan](docs/development.md)

## Lisensi

Untuk penggunaan pribadi / edukasi. Microsoft, Xbox, dan logo terkait adalah merek dagang Microsoft Corporation.
