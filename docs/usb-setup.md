# Setup USB / ADB Reverse (Windows)

Panduan lengkap menyambungkan Android → Windows melalui kabel USB menggunakan `adb reverse`, sehingga aplikasi Android bisa mengirim input gamepad ke server Python di Windows tanpa Wi-Fi.

> **Catatan penting:** USB di sini berarti **USB debugging (ADB)** antara HP dan PC Windows. Jalur USB ini **bukan** jalur USB gamepad standar (XUSB/XInput) dan **tidak** bisa dipakai langsung ke konsol Xbox. Lihat [compatibility.md](compatibility.md).

## 1. Install Android Platform-Tools di Windows

1. Unduh [SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools) (ZIP).
2. Ekstrak ke folder tetap, mis. `C:\platform-tools`.
3. (Opsional) Tambahkan ke PATH:
   - Settings → System → About → Advanced system settings → Environment Variables → edit `Path` → tambah `C:\platform-tools`.
4. Verifikasi di **Command Prompt / PowerShell**:
   ```powershell
   adb version
   ```
   Jika `adb` tidak dikenali dan PATH belum diubah, jalankan dari folder ekstraksi: `C:\platform-tools\adb.exe version`.

> Driver USB: sebagian HP (terutama Xiaomi/Oppo/Vivo) butuh OEM USB driver agar terdeteksi. Install driver dari situs vendor bila `adb devices` tidak muncul apa pun.

## 2. Aktifkan Developer Options & USB Debugging di HP

1. **Settings → About phone** → ketuk **Build number** 7× sampai muncul "You are now a developer".
   (Samsung: Settings → About phone → Software information → Build number.)
2. **Settings → System → Developer options** (lokasi bisa berbeda per merek):
   - Aktifkan **USB debugging**.
   - (Disarankan) Aktifkan **Stay awake** agar layar tidak mati saat diuji.
3. Sambungkan HP ke PC dengan **kabel USB yang mendukung data** (bukan kabel charge-only).
4. Di HP, pilih mode USB **File Transfer / MTP** (bukan "Charging only").
5. Muncul dialog **"Allow USB debugging?"** → centang "Always allow from this computer" → **Allow**.

## 3. Verifikasi Koneksi: `adb devices`

```powershell
adb devices
```

Output yang benar:

```
List of devices attached
ABC123XYZ    device
```

- `unauthorized` → belum menyetujui dialog USB debugging di HP (ulangi langkah 5 di atas).
- `offline` / tidak muncul → lihat Troubleshooting.
- Beberapa device terdeteksi → tentukan target: `adb -s ABC123XYZ reverse ...`.

## 4. Pasang Port Forwarding: `adb reverse`

```powershell
adb reverse tcp:8765 tcp:8765
```

Makna: **port 8765 di HP** diteruskan ke **port 8765 di PC**. Aplikasi Android membuka `127.0.0.1:8765` di dirinya sendiri, dan trafiknya mendarat di server Python Windows.

Verifikasi:

```powershell
adb reverse --list
# (host) tcp:8765  (device) tcp:8765
```

Urutan menjalankan sesi lengkap:

```powershell
# Terminal 1 — Windows
cd windows
.\.venv\Scripts\activate
python server.py          # Server listening on 127.0.0.1:8765

# Terminal 2 — Windows
adb devices               # pastikan "device"
adb reverse tcp:8765 tcp:8765
python monitor.py         # (opsional) pantau input

# Di HP — buka aplikasi XboxVirtualController → tekan CONNECT
```

## 5. Troubleshooting Koneksi

| Gejala | Penyebab umum | Solusi |
|---|---|---|
| `adb` bukan perintah dikenali | PATH belum diset | Jalankan dengan path penuh atau tambah `C:\platform-tools` ke PATH. |
| `adb devices` kosong | Kabel charge-only, driver OEM hilang, mode "Charging only" | Ganti kabel data; install driver vendor; pilih MTP; coba port USB lain (hindari hub). |
| Device `unauthorized` | Dialog debugging belum disetujui / RSA berubah | Cabut-pasang kabel, "Revoke USB debugging authorizations" di Developer options lalu approve ulang. |
| Device `offline` | ADB server bentrok (mis. emulator/iTools) | `adb kill-server && adb start-server`, cabut-pasang kabel. |
| `adb reverse` gagal: `cannot bind listener` | Port 8765 di HP dipakai proses lain / server ADB stale | `adb kill-server`, mulai lagi, coba ulang; atau pakai port lain `adb reverse tcp:9000 tcp:8765` (ubah host di aplikasi ke 9000). |
| Reverse terdaftar tapi app gagal connect | Server Windows belum jalan / firewall | Pastikan `python server.py` sudah mencetak "listening"; ADB reverse tidak melewati jaringan eksternal, firewall umumnya tak masalah, tapi pastikan server bind ke `127.0.0.1` sesuai yang diteruskan. |
| Reverse hilang setelah beberapa saat | ADB disconnect ( hemat daya / kabel longgar) | Matikan USB debugging timeout, aktifkan "Stay awake", cek kabel; jalankan ulang `adb reverse` (app punya tombol reconnect). |
| Input terputus-putus | HP sleep / throttling | "Stay awake" ON, matikan battery optimization untuk aplikasi. |
| Latensi tinggi | Terlalu banyak pesan | App mengirim snapshot ~60 Hz; kurangi rate di pengaturan bila tersedia. |

Cek log cepat:

```powershell
adb logcat -s XboxVC        # log aplikasi (tag contoh)
```

Hapus pasangan reverse:

```powershell
adb reverse --remove tcp:8765   # atau: adb reverse --remove-all
```
