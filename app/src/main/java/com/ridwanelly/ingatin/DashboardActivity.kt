package com.ridwanelly.ingatin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ridwanelly.ingatin.adapters.JadwalAdapter
import com.ridwanelly.ingatin.adapters.TugasAdapter
import com.ridwanelly.ingatin.models.MataKuliah
import com.ridwanelly.ingatin.models.Tugas
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class DashboardActivity : AppCompatActivity(), JadwalAdapter.OnItemClickListener {

    private lateinit var tvWelcome: TextView
    private lateinit var tvQuote: TextView
    private lateinit var rvJadwalHariIni: RecyclerView
    private lateinit var tvEmptyJadwal: TextView
    private lateinit var rvTugasMendatang: RecyclerView
    private lateinit var tvEmptyTugas: TextView
    private lateinit var fabLihatJadwal: ExtendedFloatingActionButton

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var jadwalAdapter: JadwalAdapter
    private lateinit var tugasAdapter: TugasAdapter
    private val jadwalList = mutableListOf<Any>()
    private val tugasList = mutableListOf<Tugas>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        initViews()
        setupDashboard()
        setupRecyclerViews()

        fetchJadwalHariIni()
        fetchTugasMendatang()
    }

    private fun initViews() {
        tvWelcome = findViewById(R.id.tvWelcome)
        tvQuote = findViewById(R.id.tvQuote)
        rvJadwalHariIni = findViewById(R.id.rvJadwalHariIni)
        tvEmptyJadwal = findViewById(R.id.tvEmptyJadwal)
        rvTugasMendatang = findViewById(R.id.rvTugasMendatang)
        tvEmptyTugas = findViewById(R.id.tvEmptyTugas)
        fabLihatJadwal = findViewById(R.id.fabLihatJadwal)

        fabLihatJadwal.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    private fun setupDashboard() {
        // Menampilkan nama pengguna jika ada
        val user = auth.currentUser
        val welcomeText = user?.displayName?.let { "Selamat Datang, ${it.split(" ")[0]}!" } ?: "Selamat Datang!"
        tvWelcome.text = welcomeText

        // Menampilkan kutipan motivasi acak
        val quotes = resources.getStringArray(R.array.motivational_quotes)
        val randomIndex = Random.nextInt(quotes.size)
        tvQuote.text = quotes[randomIndex]
    }

    private fun setupRecyclerViews() {
        // Adapter untuk jadwal
        jadwalAdapter = JadwalAdapter(jadwalList, this)
        rvJadwalHariIni.layoutManager = LinearLayoutManager(this)
        rvJadwalHariIni.adapter = jadwalAdapter

        // Adapter untuk tugas
        tugasAdapter = TugasAdapter(tugasList)
        rvTugasMendatang.layoutManager = LinearLayoutManager(this)
        rvTugasMendatang.adapter = tugasAdapter
    }

    private fun fetchJadwalHariIni() {
        val userId = auth.currentUser?.uid ?: return
        val hariIni = SimpleDateFormat("EEEE", Locale("id", "ID")).format(Date())

        db.collection("users").document(userId).collection("jadwal")
            .whereEqualTo("hari", hariIni)
            .orderBy("jamMulai")
            .get()
            .addOnSuccessListener { documents ->
                jadwalList.clear()
                val result = documents.toObjects(MataKuliah::class.java)
                jadwalList.addAll(result)
                jadwalAdapter.updateData(jadwalList, rvJadwalHariIni)
                checkIfJadwalEmpty()
            }
    }

    private fun fetchTugasMendatang() {
        val userId = auth.currentUser?.uid ?: return
        val now = Timestamp.now()
        // Ambil tugas yang deadline-nya dalam 7 hari ke depan
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 7)
        val sevenDaysFromNow = Timestamp(calendar.time)

        db.collection("users").document(userId).collection("tugas")
            .whereGreaterThanOrEqualTo("deadline", now)
            .whereLessThanOrEqualTo("deadline", sevenDaysFromNow)
            .whereEqualTo("completed", false) // Hanya tampilkan yang belum selesai
            .orderBy("deadline", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { documents ->
                tugasList.clear()
                val result = documents.toObjects(Tugas::class.java)
                tugasList.addAll(result)
                tugasAdapter.updateData(tugasList, rvTugasMendatang)
                checkIfTugasEmpty()
            }
    }

    private fun checkIfJadwalEmpty() {
        if (jadwalList.isEmpty()) {
            rvJadwalHariIni.visibility = View.GONE
            tvEmptyJadwal.visibility = View.VISIBLE
        } else {
            rvJadwalHariIni.visibility = View.VISIBLE
            tvEmptyJadwal.visibility = View.GONE
        }
    }

    private fun checkIfTugasEmpty() {
        if (tugasList.isEmpty()) {
            rvTugasMendatang.visibility = View.GONE
            tvEmptyTugas.visibility = View.VISIBLE
        } else {
            rvTugasMendatang.visibility = View.VISIBLE
            tvEmptyTugas.visibility = View.GONE
        }
    }

    override fun onItemClick(mataKuliah: MataKuliah) {
        // Navigasi ke DetailMatkulActivity seperti biasa
        val intent = Intent(this, DetailMatkulActivity::class.java).apply {
            putExtra("MATKUL_ID", mataKuliah.id)
            putExtra("MATKUL_NAMA", mataKuliah.namaMatkul)
            putExtra("MATKUL_DOSEN", mataKuliah.dosen)
            putExtra("MATKUL_HARI", mataKuliah.hari)
            putExtra("MATKUL_JAM_MULAI", mataKuliah.jamMulai)
            putExtra("MATKUL_JAM_SELESAI", mataKuliah.jamSelesai)
            putExtra("MATKUL_RUANGAN", mataKuliah.ruangan)
        }
        startActivity(intent)
    }
}