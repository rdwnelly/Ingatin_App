package com.ridwanelly.ingatin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ridwanelly.ingatin.adapters.TugasAdapter
import com.ridwanelly.ingatin.models.Tugas

class DetailMatkulActivity : AppCompatActivity() {

    // --- Variabel untuk Data Mata Kuliah yang Diterima ---
    private var matkulId: String? = null
    private var matkulNama: String? = null
    private var matkulDosen: String? = null
    private var matkulInfo: String? = null

    // --- Variabel untuk Komponen UI ---
    private lateinit var tvDetailNamaMatkul: TextView
    private lateinit var tvDetailDosen: TextView
    private lateinit var tvDetailInfo: TextView
    private lateinit var rvTugas: RecyclerView
    private lateinit var tvEmptyTugas: TextView
    private lateinit var fabAddTask: FloatingActionButton

    // --- Variabel untuk Adapter dan Firebase ---
    private lateinit var tugasAdapter: TugasAdapter
    private val tugasList = mutableListOf<Tugas>()
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_matkul)

        // Langkah 1: Ambil data yang dikirim dari MainActivity
        getIntentData()

        // Inisialisasi Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Langkah 2: Hubungkan variabel dengan komponen di layout
        initViews()

        // Langkah 3: Tampilkan data mata kuliah di header
        setupHeaderViews()

        // Langkah 4: Siapkan RecyclerView untuk menampilkan daftar tugas
        setupRecyclerView()

        // Langkah 5: Ambil data tugas dari Firestore
        fetchTugasFromFirestore()

        // Langkah 6: Atur aksi untuk tombol tambah tugas
        fabAddTask.setOnClickListener {
            // Kita akan membuat AddTugasActivity di langkah berikutnya
            // Untuk sekarang, kita siapkan Intent-nya
            val intent = Intent(this, AddTugasActivity::class.java)
            // Penting: Kirim matkulId agar AddTugasActivity tahu tugas ini milik mata kuliah mana
            intent.putExtra("MATKUL_ID", matkulId)
            startActivity(intent)
        }
    }

    private fun getIntentData() {
        matkulId = intent.getStringExtra("MATKUL_ID")
        matkulNama = intent.getStringExtra("MATKUL_NAMA")
        matkulDosen = intent.getStringExtra("MATKUL_DOSEN")

        // Gabungkan beberapa info untuk ditampilkan dalam satu baris
        val hari = intent.getStringExtra("MATKUL_HARI")
        val jamMulai = intent.getStringExtra("MATKUL_JAM_MULAI")
        val jamSelesai = intent.getStringExtra("MATKUL_JAM_SELESAI")
        val ruangan = intent.getStringExtra("MATKUL_RUANGAN")
        matkulInfo = "$hari, $jamMulai-$jamSelesai di $ruangan"

        // Pengecekan penting, jika tidak ada ID, halaman ini tidak bisa berfungsi
        if (matkulId == null) {
            Toast.makeText(this, "Error: ID Mata Kuliah tidak ditemukan.", Toast.LENGTH_LONG).show()
            finish() // Tutup activity jika tidak ada ID
        }
    }

    private fun initViews() {
        tvDetailNamaMatkul = findViewById(R.id.tvDetailNamaMatkul)
        tvDetailDosen = findViewById(R.id.tvDetailDosen)
        tvDetailInfo = findViewById(R.id.tvDetailInfo)
        rvTugas = findViewById(R.id.rvTugas)
        tvEmptyTugas = findViewById(R.id.tvEmptyTugas)
        fabAddTask = findViewById(R.id.fabAddTask)
    }

    private fun setupHeaderViews() {
        tvDetailNamaMatkul.text = matkulNama
        tvDetailDosen.text = matkulDosen
        tvDetailInfo.text = matkulInfo
    }

    private fun setupRecyclerView() {
        tugasAdapter = TugasAdapter(tugasList)
        rvTugas.layoutManager = LinearLayoutManager(this)
        rvTugas.adapter = tugasAdapter
    }

    private fun fetchTugasFromFirestore() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).collection("tugas")
            // INI BAGIAN PALING KRUSIAL:
            // Kita memfilter dokumen tugas dimana field "matkulId"-nya
            // sama dengan ID mata kuliah yang sedang dibuka saat ini.
            .whereEqualTo("matkulId", matkulId)
            .orderBy("deadline", Query.Direction.ASCENDING) // Urutkan tugas berdasarkan deadline terdekat
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Handle error
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    tugasList.clear()
                    val result = snapshot.toObjects(Tugas::class.java)
                    tugasList.addAll(result)
                    tugasAdapter.updateData(tugasList, rvTugas)
                    checkIfTugasEmpty()
                }
            }
    }

    private fun checkIfTugasEmpty() {
        if (tugasList.isEmpty()) {
            rvTugas.visibility = View.GONE
            tvEmptyTugas.visibility = View.VISIBLE
        } else {
            rvTugas.visibility = View.VISIBLE
            tvEmptyTugas.visibility = View.GONE
        }
    }
}