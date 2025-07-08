# Ingatin App  студенческий помощник

**Ingatin** adalah aplikasi produktivitas berbasis Android yang dirancang khusus untuk membantu mahasiswa mengelola kegiatan akademik mereka secara efektif dan terorganisir. Aplikasi ini berfungsi sebagai asisten digital pribadi untuk melacak jadwal kuliah, tugas, dan catatan penting, serta dilengkapi fitur motivasi untuk mendorong semangat belajar.

Proyek ini dibuat untuk memenuhi tugas Ujian Akhir Semester.

## 📸 Tangkapan Layar (Screenshots)


| Dasbor Utama | Halaman Jadwal | Detail Mata Kuliah |
| :---: | :---: | :---: |
| ![Dasbor](link-gambar-dasbor.png) | ![Jadwal](link-gambar-jadwal.png) | ![Detail](link-gambar-detail.png) |

## ✨ Fitur Utama

-   **Manajemen Jadwal Kuliah:** Menambah, melihat, dan menghapus jadwal mata kuliah lengkap dengan detail dosen, hari, jam, dan ruangan.
-   **Pelacak Tugas (Task Tracker):** Mencatat tugas per mata kuliah, mengatur tenggat waktu, dan menandai tugas yang sudah selesai.
-   **Sistem Notifikasi Cerdas:** Pengingat otomatis untuk jadwal kelas yang akan datang dan tugas yang mendekati tenggat waktu menggunakan WorkManager.
-   **Gamifikasi untuk Motivasi:** Dapatkan poin dan lencana (badges) setiap kali menyelesaikan tugas tepat waktu untuk meningkatkan semangat belajar.
-   **Dasbor Proaktif:** Tampilan utama yang cerdas, menyajikan jadwal hari ini, tugas terdekat, kutipan motivasi, dan rekomendasi waktu belajar.
-   **Catatan per Mata Kuliah:** Buat dan simpan catatan penting yang terintegrasi langsung di halaman detail setiap mata kuliah.

## 🛠️ Teknologi yang Digunakan

-   **Bahasa:** [Kotlin](https://kotlinlang.org/)
-   **Arsitektur:** ViewModel (MVVM-like)
-   **Backend:** [Firebase](http://firebase.google.com/)
    -   **Firebase Authentication** untuk login dan registrasi pengguna.
    -   **Cloud Firestore** sebagai database NoSQL untuk menyimpan data jadwal, tugas, dan catatan secara real-time.
-   **UI:** Material Design 3
-   **Library Jetpack:**
    -   **WorkManager** untuk penjadwalan notifikasi di background.
    -   ViewModel & LiveData
    -   AppCompat & RecyclerView

## 🚀 Cara Instalasi

Anda dapat langsung meng-install aplikasi ini di perangkat Android Anda tanpa perlu melakukan *build* dari kode sumber.

1.  Kunjungi halaman **[Releases](https://github.com/rdwnelly/Ingatin_App/releases)** di repository ini.
2.  Unduh file `.apk` pada rilis terbaru (contoh: `app-debug.apk`).
3.  Buka file `.apk` yang telah diunduh di perangkat Anda.
4.  Anda mungkin akan diminta untuk memberikan izin instalasi dari sumber yang tidak dikenal. Buka **Setelan (Settings)** -> **Keamanan (Security)**, lalu aktifkan opsi **"Install unknown apps"** atau **"Sumber tidak dikenal"** untuk browser atau file manager yang Anda gunakan.
5.  Lanjutkan proses instalasi hingga selesai.

## 🧑‍💻 Dikembangkan oleh

**Ridwan Elly**

-   [GitHub](https://github.com/rdwnelly)
