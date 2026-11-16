# AI Floating Assistant — iOS AssistiveTouch style (v0.2.0)

Android native app (Java) dengan tombol AI melayang bergaya AssistiveTouch iPhone. Proyek membangun APK debug lewat GitHub Actions.

## Tampilan dan fungsi versi ini

- Tombol kaca abu-abu transparan dengan lingkaran putih, ikon AI, dan bayangan lembut.
- Ukuran tombol 58dp; bisa digeser, lalu otomatis menempel ke tepi kiri atau kanan layar.
- Posisi tombol disimpan agar kembali ke tempat sebelumnya.
- Ketuk tombol untuk membuka panel AI di atas aplikasi yang sedang digunakan.
- Pilih Gemini atau DeepSeek dari panel.
- Analisis screenshot melalui dialog persetujuan sistem Android.
- Riwayat chat singkat selama service berjalan, dan tekan lama jawaban untuk menyalin.
- Kolom API key tidak disediakan di APK. Android app hanya mengirim prompt/gambar ke gateway HTTPS; rahasia provider disimpan di Cloudflare Worker.

## Penting: kenapa tidak menanam API key di APK

APK dapat dibongkar dan string rahasia di dalamnya dapat diekstrak. Kunci yang sebelumnya ditempel di percakapan sudah terekspos, jadi jangan gunakan kembali; cabut dan buat kunci pengganti. Kunci baru dimasukkan satu kali ke GitHub Actions Secrets untuk diteruskan sebagai Cloudflare Worker Secrets. Pengguna APK tidak perlu mengisi key manual.

**Tanpa gateway yang dideploy dan kredensial provider yang valid, APK tetap dapat dibangun dan tombol/panel tetap berfungsi, tetapi chat AI akan menampilkan pesan bahwa gateway belum disetel.** Tidak ada cara teknis untuk memanggil Gemini/DeepSeek API tanpa kredensial yang valid. Kuota dan harga tetap mengikuti penyedia API.

## Setup satu kali untuk pemilik repository

### A. Buat Cloudflare API token

Token Cloudflare perlu izin Workers Scripts edit dan account access untuk akun Cloudflare yang akan dipakai. Catat Account ID dari dashboard Cloudflare.

### B. Tambahkan GitHub Actions Secrets

Di repository GitHub buka **Settings → Secrets and variables → Actions → New repository secret**. Tambahkan empat secrets berikut:

- `CLOUDFLARE_API_TOKEN` — token Cloudflare untuk deploy Worker.
- `CLOUDFLARE_ACCOUNT_ID` — Account ID Cloudflare.
- `GEMINI_API_KEY` — API key Gemini pengganti yang baru.
- `DEEPSEEK_API_KEY` — API key DeepSeek pengganti yang baru.

Workflow `Deploy AI Gateway` akan deploy `server/worker.js` dan menyimpan kedua API key sebagai Worker secrets. Key tidak dimasukkan ke aplikasi Android atau dimasukkan ke source code.

### C. URL Worker dan build APK

Tidak perlu menyalin URL Worker secara manual. Workflow build memakai Cloudflare Account ID dan API token yang sama untuk menemukan subdomain `workers.dev` secara otomatis, lalu memasukkan URL Worker ke APK. Bila sudah punya URL gateway sendiri, kamu boleh menggantinya dengan GitHub Actions Variable bernama `AI_GATEWAY_URL`.

### D. Uji gateway

Buka `https://URL-WORKER/health`. Respons `ok: true` menunjukkan Worker hidup. Nilai `providers.gemini` dan `providers.deepseek` harus `true` sebelum kedua AI bisa menjawab.

## Build APK lewat GitHub

1. Upload **seluruh isi folder proyek** ini ke root repository GitHub; jangan hilangkan folder `.github` atau `server`.
2. Tambahkan Secrets seperti bagian setup di atas.
3. Jalankan **Actions → Deploy AI Gateway → Run workflow**.
4. Jalankan **Actions → Build AI Floating Assistant APK → Run workflow**. URL Worker ditemukan otomatis dari akun Cloudflare.
5. Setelah selesai, unduh artifact `AI-Floating-Assistant-APK-v0.2.0`; di dalamnya terdapat `app-debug.apk`.

Jika belum mengisi semua gateway secrets, workflow deploy akan berhenti dengan status skipped yang aman (bukan membeberkan key). Build APK tetap bisa dibuat, tetapi AI belum tersambung sampai gateway dan kredensial server siap.

## Izin Android

- **Tampil di atas aplikasi lain** diperlukan untuk tombol AssistiveTouch dan panel.
- **Notifikasi** digunakan agar foreground service dapat memberi tahu bahwa tombol melayang aktif.
- **Screenshot layar** memerlukan persetujuan dialog Android setiap kali proses capture diminta.
- Beberapa ponsel perlu mengizinkan autostart/tidak membatasi aplikasi di pengaturan baterai.

## Batasan yang belum diklaim selesai

- APK belum dapat diuji pada perangkat Android fisik dari lingkungan build ini.
- Belum dibuat signed release APK; artifact adalah debug APK.
- Tidak ada akses screenshot diam-diam; Android mewajibkan persetujuan pengguna.
- Gateway belum dideploy ke akun Cloudflare pengguna dari sini karena akses akun/token tidak tersedia.
- Tidak ada jaminan kuota gratis atau tanpa batas untuk Gemini/DeepSeek.

## Versi teknis

- Android package: `com.ohli.floatingai`
- minSdk 23 (Android 6.0)
- targetSdk 35
- compileSdk 36
- Android Gradle Plugin 8.13.2
- Gradle 8.13
- JDK 17
- Gemini REST: Interactions API `gemini-3.8-flash` with image input
- DeepSeek REST: `deepseek-flash` chat completions with multimodal image input
