# Panduan Pengembangan

## Prasyarat

- Android Studio (Lts terbaru) + JDK 17 — untuk modul `android/`.
- Python 3.10+ di Windows — untuk modul `windows/`.
- Git.

## Build Android

```bash
cd android
./gradlew assembleDebug          # APK debug: app/build/outputs/apk/debug/
./gradlew assembleRelease        # butuh signing config
./gradlew installDebug           # build + install ke device terhubung (butuh adb)
./gradlew lint                   # Android Lint
```

Dari Android Studio: buka folder `android/`, tunggu Gradle Sync, pilih device, Run ▶.

## Test Android (unit)

```bash
cd android
./gradlew testDebugUnitTest      # JUnit + (opsional) MockK/Turbine
```

Cakupan yang diharapkan:
- `InputSerializer`: format JSON stabil, newline terminator, clamp nilai.
- `ConsoleProfile`: status SUPPORTED/NOT_SUPPORTED per target.
- `ConnectionManager`: state machine koneksi (tanpa socket nyata — mock UsbClient).

## Windows: setup & test (pytest)

```powershell
cd windows
python -m venv .venv
.\.venv\Scripts\activate
pip install -r requirements.txt
pip install -r requirements-dev.txt   # pytest, ruff (dev)
pytest                                 # seluruh test
pytest tests/test_input_parser.py -v   # satu modul
```

Cakupan yang diharapkan:
- `input_parser`: JSON valid/rusak, field wajib, clamp stick [-1,1] & trigger [0,1], tipe `console` tidak dikenal.
- `controller_state`: dedupe perubahan state, deadzone.
- `connection_manager`: multi-klien, disconnect bersih.
- `server`: smoke test end-to-end dengan klien socket lokal (kirim baris JSON → state virtual berubah).

Menjalankan aplikasi:

```powershell
python server.py     # server TCP :8765
python monitor.py    # viewer real-time
```

## Kontribusi

1. Fork/buat branch dari `main`: `feat/<topik>` atau `fix/<topik>`.
2. Satu PR = satu topik. sertakan deskripsi + cara reproduce.
3. Sebelum push:
   - Android: `./gradlew lint testDebugUnitTest`
   - Windows: `pytest` + `ruff check windows`
4. Update dokumentasi (`docs/`) bila mengubah format JSON, protokol, atau perilaku koneksi.
5. **Jangan menjanjikan dukungan konsol Xbox** — semua klaim kompatibilitas harus konsisten dengan [compatibility.md](compatibility.md).

## Coding Conventions

### Kotlin / Android
- Paket per fitur sesuai struktur: `connection`, `console`, `data`, `ui`, `viewmodel`, `navigation`.
- Nama class `PascalCase`, fungsi/properti `camelCase`, konstanta `UPPER_SNAKE`.
- UI: Jetpack Compose; state via ViewModel + StateFlow; tidak ada logika socket di Composable.
- Socket I/O hanya di `UsbClient` di dalam coroutine Dispatchers.IO; UI tidak pernah memblokir.
- Format: ktlint (indent 4, import eksplisit).

### Python / Windows
- PEP 8, line length 100; `ruff` untuk lint/format.
- Type hints untuk fungsi publik; docstring satu baris untuk modul.
- Parsing tidak pernah melempar exception mentah ke server: tangkap, log, lanjutkan (server harus tetap hidup).
- Test di `windows/tests/`, nama `test_*.py`, gunakan fixture socket ephemeral (port acak) agar tidak bentrok dengan server dev.
- Tidak menambah dependensi tanpa diskusi; simpan di `requirements.txt` (runtime) / `requirements-dev.txt` (dev).

### Protokol / JSON
- Satu pesan = satu baris (`\n`). Jangan kirim JSON multi-baris.
- Field baru harus opsional (parser lama tetap jalan) dan didokumentasikan di [architecture.md](architecture.md).
- Versi protokol dinaikkan hanya bila backward-compatible broken.
