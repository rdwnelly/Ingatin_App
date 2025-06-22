package com.ridwanelly.ingatin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ridwanelly.ingatin.adapters.JadwalAdapter
import com.ridwanelly.ingatin.models.MataKuliah

// =====================================================================================
// PERUBAHAN 1: MainActivity sekarang "berjanji" untuk mematuhi kontrak OnItemClickListener
// =====================================================================================
class MainActivity : AppCompatActivity(), JadwalAdapter.OnItemClickListener {

    // --- Deklarasi Variabel untuk Komponen UI dan Adapter ---
    private lateinit var rvJadwal: RecyclerView
    private lateinit var fabAddJadwal: FloatingActionButton
    private lateinit var tvEmptyView: TextView
    private lateinit var jadwalAdapter: JadwalAdapter
    private val jadwalList = mutableListOf<MataKuliah>()

    // --- Deklarasi Variabel untuk Firebase ---
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inisialisasi Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Pengecekan krusial: Jika pengguna belum login, jangan lanjutkan.
        // Kembalikan ke halaman Login.
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return // Hentikan eksekusi lebih lanjut dari fungsi onCreate
        }

        // Menghubungkan variabel dengan komponen di layout XML
        rvJadwal = findViewById(R.id.rvJadwal)
        fabAddJadwal = findViewById(R.id.fabAddJadwal)
        tvEmptyView = findViewById(R.id.tvEmptyView)

        // Memanggil fungsi untuk mempersiapkan RecyclerView
        setupRecyclerView()

        // Memberi aksi pada tombol FAB (+)
        fabAddJadwal.setOnClickListener {
            startActivity(Intent(this, AddJadwalActivity::class.java))
        }

        // Memanggil fungsi untuk mengambil data jadwal dari Firestore
        fetchJadwalFromFirestore()
    }

    private fun setupRecyclerView() {
        // =====================================================================================
        // PERUBAHAN 2: Saat membuat Adapter, kita berikan "this" (MainActivity itu sendiri)
        // sebagai listener-nya.
        // =====================================================================================
        jadwalAdapter = JadwalAdapter(jadwalList, this)
        rvJadwal.layoutManager = LinearLayoutManager(this)
        rvJadwal.adapter = jadwalAdapter
    }

    private fun fetchJadwalFromFirestore() {
        val userId = auth.currentUser?.uid
        if (userId == null) return // Keluar dari fungsi jika user ID tidak ditemukan

        // Menggunakan addSnapshotListener untuk mendapatkan data real-time
        db.collection("users").document(userId).collection("jadwal")
            .orderBy("hari") // Mengurutkan jadwal berdasarkan hari
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Tangani error di sini, misalnya dengan Toast
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    jadwalList.clear() // Kosongkan daftar lama sebelum diisi data baru
                    val result = snapshot.toObjects(MataKuliah::class.java)
                    jadwalList.addAll(result)
                    jadwalAdapter.updateData(jadwalList) // Beritahu adapter bahwa data telah berubah

                    checkIfEmpty() // Periksa apakah daftar sekarang kosong atau tidak
                }
            }
    }

    private fun checkIfEmpty() {
        if (jadwalList.isEmpty()) {
            rvJadwal.visibility = View.GONE
            tvEmptyView.visibility = View.VISIBLE
        } else {
            rvJadwal.visibility = View.VISIBLE
            tvEmptyView.visibility = View.GONE
        }
    }

    // =====================================================================================
    // PERUBAHAN 3: Ini adalah fungsi yang wajib kita buat karena "janji" di atas.
    // Fungsi ini akan dieksekusi ketika sebuah item di adapter diklik.
    // =====================================================================================
    override fun onItemClick(mataKuliah: MataKuliah) {
        // Buat Intent untuk membuka DetailMatkulActivity
        val intent = Intent(this, DetailMatkulActivity::class.java)

        // Lampirkan semua data dari mata kuliah yang diklik ke dalam Intent.
        // Ini seperti memberi "catatan" untuk halaman berikutnya.
        intent.putExtra("MATKUL_ID", mataKuliah.id)
        intent.putExtra("MATKUL_NAMA", mataKuliah.namaMatkul)
        intent.putExtra("MATKUL_DOSEN", mataKuliah.dosen)
        intent.putExtra("MATKUL_HARI", mataKuliah.hari)
        intent.putExtra("MATKUL_JAM_MULAI", mataKuliah.jamMulai)
        intent.putExtra("MATKUL_JAM_SELESAI", mataKuliah.jamSelesai)
        intent.putExtra("MATKUL_RUANGAN", mataKuliah.ruangan)

        // Mulai Activity baru dengan Intent yang sudah berisi data
        startActivity(intent)
    }
}