# Kompatibilitas (jujur & detail per generasi)

> Halaman ini ditulis dengan jujur agar tidak ada harapan palsu. **Output langsung ke konsol Xbox TIDAK didukung oleh proyek ini.**

## Ringkasan status

| Target | Status | Keterangan |
|---|---|---|
| **PC Windows** (via virtual gamepad ViGEm/XInput) | ✅ **SUPPORTED** | Satu-satunya target yang benar-benar berfungsi di V1/V2. Game PC melihat "Xbox 360 Controller" dari driver virtual. |
| Xbox One | ❌ **NOT_SUPPORTED** (output langsung) | Butuh autentikasi proprietary per-controller. |
| Xbox Series X/S | ❌ **NOT_SUPPORTED** (output langsung) | Sama seperti One; handshake lebih ketat lagi. |
| Xbox 360 | ❌ **NOT_SUPPORTED** (output langsung) | Protokol/GamepadAPI **berbeda total** dari One/Series (lihat di bawah). |
| Android → PC | ✅ SUPPORTED | Jalur USB + ADB reverse + TCP :8765. |

## Kenapa gamepad virtual TIDAK otomatis dikenali konsol Xbox?

Konsol Xbox tidak memercayai perangkat USB gamepad generik. Saat controller di-pasangkan/dicolokkan:

1. **Autentikasi proprietary per-controller (handshake terenkripsi):** konsol dan controller melakukan challenge-response kriptografi menggunakan **kunci rahasia yang disimpan di hardware controller asli** (per unit). Konsol memverifikasi bahwa perangkat benar-benar controller berlisensi/produksi resmi sebelum mengaktifkan input.
2. **Gamepad virtual (ViGEm, vJoy, dsb.) hanya ada di Windows** — mereka membuat perangkat XInput di level OS PC. Perangkat semacam itu tidak punya kunci rahasia, dan tidak pernah melewati handshake konsol; konsol akan menolaknya sebagai "periferal tidak dikenal".
3. **USB biasa dari HP/PC bukan endpoint gamepad Xbox:** protokol USB gamepad Xbox (mis. XUSB) punya descriptor dan urutan enumerasi khusus, dan tetap diikuti handshake autentikasi. Jadi walau kita "meniru" descriptor pun, konsol tetap meminta autentikasi.

Konsekuensi jujurnya: **tidak ada solusi software-only** untuk membuat Android/PC virtual pad dikenali konsol Xbox.

## Per generasi

### Xbox 360
- Protokol gamepad: **Xbox 360 wired/wireless protocol** (proprietary), tidak sama dengan generasi berikut. Frame paket, endpoint USB, dan skema autentikasi 360 berbeda.
- Aksesori resmi: **Xbox 360 Wireless Gaming Receiver for Windows** (adapter USB untuk controller 360 wireless di PC).
- Controller 360 wired USB dari PC **tidak** bisa dipakai begitu saja di 360 (karena autentikasi/ID perangkat), dan virtual gamepad Windows jelas tidak.
- Status: **NOT_SUPPORTED** untuk output langsung.

### Xbox One / Xbox Series X|S
- Protokol: **GIP (Game Input Protocol)** — juga dikenal terkait XGIP; proprietary Microsoft, dikombinasikan dengan handshake autentikasi terenkripsi per-controller.
- Controller One/Series di PC bekerja sebagai XInput/GameInput via driver Windows — inilah yang diimitasi virtual gamepad, tapi **hanya di Windows**.
- Status: **NOT_SUPPORTED** untuk output langsung.

## Opsi V3 (riset — belum & mungkin tidak diimplementasikan)

Semua opsi berikut ditandai sebagai **riset/dokumentasi**, bukan fitur jadi:

1. **Riset protokol GIP/XGIP**
   - Memahami framing GIP mungkin (komunitas telah mereverse sebagian), **tetapi** handshake autentikasi per-controller memerlukan kunci yang tidak tersedia. Meniru GIP tanpa autentikasi = konsol menolak. Peluang berhasil: sangat rendah; berisiko pelanggaran ToS/keamanan.

2. **Perangkat hardware tambahan**
   - **USB Host Shield (Arduino/ESP32)** + modul nirkabel, mis. **Xbox Wireless Adapter**-turunan atau chip yang mampu pairing radio resmi: jembatan fisik antara Android (serial/BT/USB) dan radio Xbox. Secara teoretis jalur yang paling realistis, tetapi butuh hardware, firmware reverse-engineering, dan biaya.

3. **Controller fisik sebagai proxy**
   - Paling praktis: gunakan **controller Xbox asli** yang terpasang di konsol, dan pasang **microcontroller (mis. ATmega32U4 / RP2040 dengan USB device mode)** yang meniru handshake-nya dengan menyadap/mereplay sesi asli — atau lebih sederhana: inject input di level PC dan mainkan streaming ke Xbox. Alternatif paling "jujur dan mudah": jalankan game di PC (Cloud Gaming / streaming) di mana virtual gamepad Windows langsung valid.

4. **Xbox Cloud Gaming / Remote Play (jalur legal tanpa hardware)**
   - Bukan output langsung ke konsol, tetapi Android app ini bisa menggantikan touch controls saat streaming xCloud dari PC/HP, karena streaming mengkonsumsi input gamepad standar.

## Kesimpulan

- Yang **berfungsi sekarang**: Android → USB/ADB → Windows virtual gamepad → game PC. ✅
- Yang **tidak berfungsi dan tidak dijanjikan**: Android → konsol Xbox apa pun. ❌ (semua generasi NOT_SUPPORTED)
- V3 adalah **riset & dokumentasi**; tidak ada kode output-konsol di repo ini.
