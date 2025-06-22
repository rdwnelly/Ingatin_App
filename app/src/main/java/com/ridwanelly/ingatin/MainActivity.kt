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
import com.ridwanelly.ingatin.adapters.JadwalAdapter
import com.ridwanelly.ingatin.models.MataKuliah

class MainActivity : AppCompatActivity(), JadwalAdapter.OnItemClickListener {

    private lateinit var rvJadwal: RecyclerView
    private lateinit var fabAddJadwal: FloatingActionButton
    private lateinit var tvEmptyView: TextView
    private lateinit var jadwalAdapter: JadwalAdapter
    private val itemList = mutableListOf<Any>() // Menggunakan List<Any>

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        rvJadwal = findViewById(R.id.rvJadwal)
        fabAddJadwal = findViewById(R.id.fabAddJadwal)
        tvEmptyView = findViewById(R.id.tvEmptyView)

        setupRecyclerView()

        fabAddJadwal.setOnClickListener {
            startActivity(Intent(this, AddJadwalActivity::class.java))
        }

        fetchJadwalFromFirestore()
    }

    private fun setupRecyclerView() {
        jadwalAdapter = JadwalAdapter(itemList, this)
        rvJadwal.layoutManager = LinearLayoutManager(this)
        rvJadwal.adapter = jadwalAdapter
    }

    private fun fetchJadwalFromFirestore() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).collection("jadwal")
            .orderBy("jamMulai") // Cukup urutkan berdasarkan jam
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Tangani error di sini
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val jadwalFromDb = snapshot.toObjects(MataKuliah::class.java)

                    // --- LOGIKA BARU UNTUK PENGURUTAN HARI ---

                    // 1. Definisikan urutan hari yang benar
                    val urutanHari = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")

                    // 2. Kelompokkan jadwal berdasarkan hari
                    val jadwalPerHari = jadwalFromDb.groupBy { it.hari }

                    // 3. Bangun list akhir sesuai urutan hari yang benar
                    itemList.clear()
                    urutanHari.forEach { hari ->
                        // Ambil daftar jadwal untuk hari ini
                        val jadwalHariIni = jadwalPerHari[hari]

                        // Jika ada jadwal di hari ini, tambahkan header dan jadwalnya
                        if (!jadwalHariIni.isNullOrEmpty()) {
                            itemList.add(hari) // Tambah header hari (String)
                            itemList.addAll(jadwalHariIni) // Tambah semua jadwal di hari itu (MataKuliah)
                        }
                    }
                    // --- AKHIR LOGIKA BARU ---

                    jadwalAdapter.updateData(itemList)
                    checkIfEmpty()
                }
            }
    }

    private fun checkIfEmpty() {
        if (itemList.isEmpty()) {
            rvJadwal.visibility = View.GONE
            tvEmptyView.visibility = View.VISIBLE
        } else {
            rvJadwal.visibility = View.VISIBLE
            tvEmptyView.visibility = View.GONE
        }
    }

    override fun onItemClick(mataKuliah: MataKuliah) {
        val intent = Intent(this, DetailMatkulActivity::class.java)

        intent.putExtra("MATKUL_ID", mataKuliah.id)
        intent.putExtra("MATKUL_NAMA", mataKuliah.namaMatkul)
        intent.putExtra("MATKUL_DOSEN", mataKuliah.dosen)
        intent.putExtra("MATKUL_HARI", mataKuliah.hari)
        intent.putExtra("MATKUL_JAM_MULAI", mataKuliah.jamMulai)
        intent.putExtra("MATKUL_JAM_SELESAI", mataKuliah.jamSelesai)
        intent.putExtra("MATKUL_RUANGAN", mataKuliah.ruangan)

        startActivity(intent)
    }
}