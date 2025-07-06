package com.ridwanelly.ingatin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
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
import java.util.concurrent.TimeUnit

class DashboardActivity : AppCompatActivity(), JadwalAdapter.OnItemClickListener {

    private lateinit var tvWelcome: TextView
    private lateinit var rvJadwalHariIni: RecyclerView
    private lateinit var tvEmptyJadwal: TextView
    private lateinit var rvTugasMendatang: RecyclerView
    private lateinit var tvEmptyTugas: TextView
    private lateinit var fabLihatJadwal: ExtendedFloatingActionButton

    // --- KARTU BARU ---
    private lateinit var cardFokusHariIni: CardView
    private lateinit var tvFokusKonten: TextView
    private lateinit var cardAnalisisMingguan: CardView
    private lateinit var tvAnalisisKonten: TextView
    private lateinit var cardRekomendasiBelajar: CardView
    private lateinit var tvRekomendasiKonten: TextView
    // --- AKHIR KARTU BARU ---

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var jadwalAdapter: JadwalAdapter
    private lateinit var tugasAdapter: TugasAdapter
    private val jadwalList = mutableListOf<MataKuliah>() // Diubah ke tipe spesifik
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

        // Memanggil semua fungsi fetch data
        fetchAllData()
    }

    private fun initViews() {
        tvWelcome = findViewById(R.id.tvWelcome)
        rvJadwalHariIni = findViewById(R.id.rvJadwalHariIni)
        tvEmptyJadwal = findViewById(R.id.tvEmptyJadwal)
        rvTugasMendatang = findViewById(R.id.rvTugasMendatang)
        tvEmptyTugas = findViewById(R.id.tvEmptyTugas)
        fabLihatJadwal = findViewById(R.id.fabLihatJadwal)

        // Init views untuk kartu baru
        cardFokusHariIni = findViewById(R.id.cardFokusHariIni)
        tvFokusKonten = findViewById(R.id.tvFokusKonten)
        cardAnalisisMingguan = findViewById(R.id.cardAnalisisMingguan)
        tvAnalisisKonten = findViewById(R.id.tvAnalisisKonten)
        cardRekomendasiBelajar = findViewById(R.id.cardRekomendasiBelajar)
        tvRekomendasiKonten = findViewById(R.id.tvRekomendasiKonten)

        fabLihatJadwal.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    private fun setupDashboard() {
        val user = auth.currentUser
        val welcomeText = user?.displayName?.let { "Halo, ${it.split(" ")[0]}!" } ?: "Selamat Datang!"
        tvWelcome.text = welcomeText
    }

    private fun setupRecyclerViews() {
        jadwalAdapter = JadwalAdapter(jadwalList, this)
        rvJadwalHariIni.layoutManager = LinearLayoutManager(this)
        rvJadwalHariIni.adapter = jadwalAdapter

        tugasAdapter = TugasAdapter(tugasList)
        rvTugasMendatang.layoutManager = LinearLayoutManager(this)
        rvTugasMendatang.adapter = tugasAdapter
    }

    private fun fetchAllData() {
        val userId = auth.currentUser?.uid ?: return
        val today = SimpleDateFormat("EEEE", Locale("id", "ID")).format(Date())

        // 1. Fetch Jadwal Hari Ini
        db.collection("users").document(userId).collection("jadwal")
            .whereEqualTo("hari", today)
            .orderBy("jamMulai")
            .get()
            .addOnSuccessListener { jadwalDocs ->
                jadwalList.clear()
                val resultJadwal = jadwalDocs.toObjects(MataKuliah::class.java)
                jadwalList.addAll(resultJadwal)
                jadwalAdapter.updateData(jadwalList, rvJadwalHariIni)
                checkIfJadwalEmpty()

                // 2. Fetch Tugas Mendatang (setelah jadwal selesai)
                fetchTugasMendatang(userId) {
                    // 3. Setup Kartu Proaktif (setelah semua data ada)
                    setupFokusHariIni()
                    setupAnalisisMingguan(userId)
                    setupRekomendasiBelajar(userId)
                }
            }
    }

    private fun fetchTugasMendatang(userId: String, onComplete: () -> Unit) {
        val now = Timestamp.now()
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 7)
        val sevenDaysFromNow = Timestamp(calendar.time)

        db.collection("users").document(userId).collection("tugas")
            .whereGreaterThanOrEqualTo("deadline", now)
            .whereLessThanOrEqualTo("deadline", sevenDaysFromNow)
            .whereEqualTo("completed", false)
            .orderBy("deadline", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { tugasDocs ->
                tugasList.clear()
                val resultTugas = tugasDocs.toObjects(Tugas::class.java)
                tugasList.addAll(resultTugas)
                tugasAdapter.updateData(tugasList, rvTugasMendatang)
                checkIfTugasEmpty()
                onComplete() // Panggil callback
            }
    }

    // --- FUNGSI PROAKTIF #1: FOKUS HARI INI ---
    private fun setupFokusHariIni() {
        val fokusItems = mutableListOf<String>()

        // Cek jadwal hari ini
        if (jadwalList.isNotEmpty()) {
            val jadwalTerdekat = jadwalList.first()
            fokusItems.add("Ada kelas **${jadwalTerdekat.namaMatkul}** jam ${jadwalTerdekat.jamMulai}.")
        }

        // Cek deadline tugas hari ini
        val todayCalendar = Calendar.getInstance()
        val tugasHariIni = tugasList.filter {
            val deadlineCalendar = Calendar.getInstance().apply { time = it.deadline!!.toDate() }
            todayCalendar.get(Calendar.YEAR) == deadlineCalendar.get(Calendar.YEAR) &&
                    todayCalendar.get(Calendar.DAY_OF_YEAR) == deadlineCalendar.get(Calendar.DAY_OF_YEAR)
        }

        if (tugasHariIni.isNotEmpty()) {
            val tugasTerdekat = tugasHariIni.first()
            fokusItems.add("Jangan lupa deadline tugas **${tugasTerdekat.namaTugas}** hari ini.")
        }

        if (fokusItems.isEmpty()) {
            fokusItems.add("Hari ini santai! Tidak ada kelas atau deadline tugas mendesak.")
        }

        tvFokusKonten.text = fokusItems.joinToString("\n")
        cardFokusHariIni.visibility = View.VISIBLE
    }

    // --- FUNGSI PROAKTIF #2: ANALISIS PRODUKTIVITAS MINGGUAN ---
    private fun setupAnalisisMingguan(userId: String) {
        val cal = Calendar.getInstance()
        // Cek jika hari ini adalah Senin
        if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
            val oneWeekAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
            val startOfThisWeek = Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, Calendar.MONDAY); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0) }

            db.collection("users").document(userId).collection("tugas")
                .whereEqualTo("completed", true)
                .whereGreaterThanOrEqualTo("completedAt", Timestamp(oneWeekAgo.time))
                .whereLessThan("completedAt", Timestamp(startOfThisWeek.time))
                .get()
                .addOnSuccessListener { documents ->
                    val completedCount = documents.size()
                    if (completedCount > 0) {
                        tvAnalisisKonten.text = "Selamat hari Senin! Minggu lalu kamu berhasil menyelesaikan **$completedCount tugas**. Pertahankan semangatmu minggu ini!"
                        cardAnalisisMingguan.visibility = View.VISIBLE
                    }
                }
        }
    }

    // --- FUNGSI PROAKTIF #3: REKOMENDASI WAKTU BELAJAR ---
    private fun setupRekomendasiBelajar(userId: String) {
        val now = Calendar.getInstance()
        val todayStr = SimpleDateFormat("EEEE", Locale("id", "ID")).format(now.time)

        // 1. Ambil semua jadwal hari ini
        db.collection("users").document(userId).collection("jadwal")
            .whereEqualTo("hari", todayStr)
            .orderBy("jamMulai")
            .get()
            .addOnSuccessListener { jadwalDocs ->
                val jadwalHariIni = jadwalDocs.toObjects(MataKuliah::class.java)

                // 2. Buat daftar slot waktu yang sudah terisi
                val busySlots = mutableListOf<Pair<Calendar, Calendar>>()
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                for (jadwal in jadwalHariIni) {
                    val startCal = Calendar.getInstance().apply { time = timeFormat.parse(jadwal.jamMulai!!)!! }
                    val endCal = Calendar.getInstance().apply { time = timeFormat.parse(jadwal.jamSelesai!!)!! }
                    val busyStart = (now.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, startCal.get(Calendar.HOUR_OF_DAY)); set(Calendar.MINUTE, startCal.get(Calendar.MINUTE)) }
                    val busyEnd = (now.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, endCal.get(Calendar.HOUR_OF_DAY)); set(Calendar.MINUTE, endCal.get(Calendar.MINUTE)) }
                    busySlots.add(Pair(busyStart, busyEnd))
                }

                // 3. Cari slot waktu luang (antara jam 8 pagi - 10 malam)
                val freeSlots = mutableListOf<Pair<Calendar, Long>>() // Pair of start time and duration in minutes
                val dayStart = (now.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, 8); set(Calendar.MINUTE, 0) }
                val dayEnd = (now.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, 22); set(Calendar.MINUTE, 0) }

                var lastBusyTime = dayStart
                for (slot in busySlots.sortedBy { it.first }) {
                    if (slot.first.after(lastBusyTime)) {
                        val duration = TimeUnit.MILLISECONDS.toMinutes(slot.first.timeInMillis - lastBusyTime.timeInMillis)
                        if (duration >= 90) { // Cari waktu luang minimal 1.5 jam
                            freeSlots.add(Pair(lastBusyTime, duration))
                        }
                    }
                    lastBusyTime = slot.second
                }
                // Cek waktu luang setelah jadwal terakhir hingga akhir hari
                if (dayEnd.after(lastBusyTime)) {
                    val duration = TimeUnit.MILLISECONDS.toMinutes(dayEnd.timeInMillis - lastBusyTime.timeInMillis)
                    if (duration >= 90) {
                        freeSlots.add(Pair(lastBusyTime, duration))
                    }
                }

                // 4. Jika ada waktu luang dan ada tugas, buat rekomendasi
                val firstFreeSlot = freeSlots.firstOrNull { it.first.after(now) }
                if (firstFreeSlot != null && tugasList.isNotEmpty()) {
                    val tugasUntukDikerjakan = tugasList.first() // Ambil tugas dengan deadline terdekat
                    val jam = SimpleDateFormat("HH:mm", Locale.getDefault()).format(firstFreeSlot.first.time)
                    tvRekomendasiKonten.text = "Ada waktu luang **${firstFreeSlot.second / 60} jam** mulai pukul **$jam**. Waktu yang pas untuk mencicil tugas **${tugasUntukDikerjakan.namaTugas}**!"
                    cardRekomendasiBelajar.visibility = View.VISIBLE
                }
            }
    }


    private fun checkIfJadwalEmpty() {
        rvJadwalHariIni.visibility = if (jadwalList.isEmpty()) View.GONE else View.VISIBLE
        tvEmptyJadwal.visibility = if (jadwalList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun checkIfTugasEmpty() {
        rvTugasMendatang.visibility = if (tugasList.isEmpty()) View.GONE else View.VISIBLE
        tvEmptyTugas.visibility = if (tugasList.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onItemClick(mataKuliah: MataKuliah) {
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

    override fun onResume() {
        super.onResume()
        // Muat ulang data saat kembali ke activity ini untuk memastikan data selalu terbaru
        if (auth.currentUser != null) {
            fetchAllData()
        }
    }
}