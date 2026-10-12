# Monitoring Infrastruktur Desa — Android

## Fitur versi 1.0
- Formulir identitas desa/pengurus dan data paket pekerjaan.
- Empat slot dokumentasi foto.
- Draft formulir disimpan lokal menggunakan SharedPreferences.
- Foto disimpan di penyimpanan privat aplikasi.
- Ekspor laporan PDF ukuran A4 dengan ringkasan data dan dokumentasi foto.
- Tidak membutuhkan akun atau server.

## Cara membuka dan membangun APK
1. Ekstrak ZIP ini.
2. Buka folder `MonitoringInfrastrukturDesa` menggunakan Android Studio versi terbaru.
3. Tunggu Gradle Sync selesai (internet dibutuhkan untuk mengunduh dependensi saat build pertama).
4. Pilih **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. APK debug biasanya tersedia di `app/build/outputs/apk/debug/app-debug.apk`.

## Catatan penting
- Ini adalah proyek sumber Android Studio, bukan APK yang sudah dikompilasi.
- Pengambilan foto menggunakan aplikasi kamera bawaan melalui intent. Pemilihan kamera depan/belakang adalah permintaan ke aplikasi kamera dan tidak dijamin oleh semua perangkat.
- Resolusi 4080 × 1836 bergantung pada kemampuan kamera dan aplikasi kamera perangkat; aplikasi tidak dapat memaksa sensor menghasilkan resolusi yang tidak didukung.
- Versi ini menyimpan satu draft aktif di perangkat. Ekspor PDF menggunakan pemilih lokasi Android.
- Uji di perangkat sasaran sebelum digunakan resmi. Sesuaikan format/identitas instansi bila diperlukan.

## Build APK dari HP saja (GitHub Actions)
File `.github/workflows/android.yml` disertakan untuk build cloud. Namun, build cloud perlu diuji setelah proyek diunggah ke GitHub; bila gagal, periksa log Actions dan sesuaikan versi Gradle/plugin. Untuk menjalankan:
1. Buat akun GitHub dan repository baru dari browser HP.
2. Unggah semua isi folder proyek ke repository, termasuk folder tersembunyi `.github/workflows`.
3. Buka tab **Actions**, pilih **Build APK Android**, lalu **Run workflow** (atau tunggu build setelah push ke branch `main`).
4. Jika berhasil, buka hasil run dan unduh artifact `MonitoringInfrastrukturDesa-debug-apk.zip`; ekstrak untuk mendapatkan `app-debug.apk`.
Catatan: repository publik membuat kode sumber dapat dilihat orang lain. Pilih private jika ingin menjaga kode proyek tidak publik.
