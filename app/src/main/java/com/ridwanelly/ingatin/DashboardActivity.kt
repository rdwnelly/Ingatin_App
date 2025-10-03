package com.ridwanelly.ingatin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ridwanelly.ingatin.adapters.JadwalAdapter
import com.ridwanelly.ingatin.adapters.TugasAdapter
import com.ridwanelly.ingatin.models.MataKuliah
import com.ridwanelly.ingatin.models.Tugas
import com.ridwanelly.ingatin.models.UserGamification
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class DashboardActivity : AppCompatActivity(), JadwalAdapter.OnItemClickListener {

    private lateinit var tvWelcome: TextView
    private lateinit var rvJadwalHariIni: RecyclerView
    private lateinit var tvEmptyJadwal: TextView
    private lateinit var rvTugasMendatang: RecyclerView
    private lateinit var tvEmptyTugas: TextView
    private lateinit var fabLihatJadwal: ExtendedFloatingActionButton

    // --- KARTU PROAKTIF ---
    private lateinit var cardFokusHariIni: CardView
    private lateinit var tvFokusKonten: TextView
    private lateinit var cardAnalisisMingguan: CardView
    private lateinit var tvAnalisisKonten: TextView
    private lateinit var cardRekomendasiBelajar: CardView
    private lateinit var tvRekomendasiKonten: TextView
    private lateinit var cardMotivation: CardView
    private lateinit var tvMotivation: TextView

    // --- Variabel UI BARU untuk Gamifikasi ---
    private lateinit var cardGamifikasi: CardView
    private lateinit var tvUserLevel: TextView
    private lateinit var tvUserPoints: TextView
    private lateinit var chipGroupBadges: ChipGroup

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var jadwalAdapter: JadwalAdapter
    private lateinit var tugasAdapter: TugasAdapter
    private val jadwalList = mutableListOf<MataKuliah>()
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
        fetchAllData()
    }

    private fun initViews() {
        tvWelcome = findViewById(R.id.tvWelcome)
        rvJadwalHariIni = findViewById(R.id.rvJadwalHariIni)
        tvEmptyJadwal = findViewById(R.id.tvEmptyJadwal)
        rvTugasMendatang = findViewById(R.id.rvTugasMendatang)
        tvEmptyTugas = findViewById(R.id.tvEmptyTugas)
        fabLihatJadwal = findViewById(R.id.fabLihatJadwal)

        // Init views untuk kartu proaktif
        cardFokusHariIni = findViewById(R.id.cardFokusHariIni)
        tvFokusKonten = findViewById(R.id.tvFokusKonten)
        cardAnalisisMingguan = findViewById(R.id.cardAnalisisMingguan)
        tvAnalisisKonten = findViewById(R.id.tvAnalisisKonten)
        cardRekomendasiBelajar = findViewById(R.id.cardRekomendasiBelajar)
        tvRekomendasiKonten = findViewById(R.id.tvRekomendasiKonten)
        cardMotivation = findViewById(R.id.cardMotivation)
        tvMotivation = findViewById(R.id.tvMotivation)

        // --- Inisialisasi View BARU untuk Gamifikasi ---
        cardGamifikasi = findViewById(R.id.cardGamifikasi)
        tvUserLevel = findViewById(R.id.tvUserLevel)
        tvUserPoints = findViewById(R.id.tvUserPoints)
        chipGroupBadges = findViewById(R.id.chipGroupBadges)

        fabLihatJadwal.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    private fun setupDashboard() {
        val user = auth.currentUser
        val welcomeText = user?.email?.split("@")?.get(0)?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }?.let { "Halo, $it!" } ?: "Selamat Datang!"
        tvWelcome.text = welcomeText

        // Menampilkan kutipan motivasi secara acak
        val quotes = resources.getStringArray(R.array.motivational_quotes)
        val randomQuote = quotes.random()
        tvMotivation.text = randomQuote
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
        Log.d("DashboardActivity", "Fetching schedule for today: $today")

        fetchGamificationStats(userId)

        // Fetch Jadwal Hari Ini
        db.collection("users").document(userId).collection("jadwal")
            .whereEqualTo("hari", today)
            .orderBy("jamMulai")
            .get()
            .addOnSuccessListener { jadwalDocs ->
                Log.d("DashboardActivity", "Fetched ${jadwalDocs.size()} schedule items for today.")
                jadwalList.clear()
                val resultJadwal = jadwalDocs.toObjects(MataKuliah::class.java)
                jadwalList.addAll(resultJadwal)
                jadwalAdapter.updateData(jadwalList, rvJadwalHariIni)
                checkIfJadwalEmpty()

                // Fetch Tugas Mendatang
                db.collection("users").document(userId).collection("tugas")
                    .whereGreaterThanOrEqualTo("deadline", Timestamp.now())
                    .whereEqualTo("completed", false)
                    .orderBy("deadline", Query.Direction.ASCENDING)
                    .get()
                    .addOnSuccessListener { tugasDocs ->
                        tugasList.clear()
                        val resultTugas = tugasDocs.toObjects(Tugas::class.java)
                        tugasList.addAll(resultTugas)
                        tugasAdapter.updateData(tugasList, rvTugasMendatang)
                        checkIfTugasEmpty()
                        setupFokusHariIni()
                        setupAnalisisMingguan(userId)
                        setupRekomendasiBelajar(userId)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("DashboardActivity", "Error fetching schedule: ${e.message}")
            }
    }

    // --- FUNGSI BARU UNTUK MENGAMBIL DATA GAMIFIKASI ---
    private fun fetchGamificationStats(userId: String) {
        val gamificationRef = db.collection("users").document(userId).collection("gamification").document("summary")

        gamificationRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) {
                cardGamifikasi.visibility = View.GONE
                return@addSnapshotListener
            }

            val gamificationData = snapshot.toObject(UserGamification::class.java)
            if (gamificationData != null) {
                cardGamifikasi.visibility = View.VISIBLE

                // Set Level dan Poin
                tvUserLevel.text = getString(R.string.level_format, gamificationData.level)
                tvUserPoints.text = getString(R.string.points_format, gamificationData.points)

                // Tampilkan Badges
                chipGroupBadges.removeAllViews()
                if (gamificationData.badges.isEmpty()) {
                    val noBadgeChip = Chip(this).apply {
                        text = getString(R.string.no_badge)
                    }
                    chipGroupBadges.addView(noBadgeChip)
                } else {
                    gamificationData.badges.forEach { badgeId ->
                        val chip = Chip(this).apply {
                            text = context.getString(R.string.badge_earned, getBadgeName(badgeId))
                            isClickable = false
                        }
                        chipGroupBadges.addView(chip)
                    }
                }
            }
        }
    }

    private fun getBadgeName(badgeId: String): String {
        return when(badgeId) {
            "FIRST_STEP" -> "Langkah Pertama"
            "FIVE_STAR" -> "Bintang Lima"
            "TEN_MASTER" -> "Master Sepuluh"
            "DILIGENT_25" -> "Rajin Belajar"
            else -> "Badge Spesial"
        }
    }

    private fun setupFokusHariIni() {
        val fokusItems = mutableListOf<String>()

        // 1. Tambahkan info jadwal terdekat jika ada
        if (jadwalList.isNotEmpty()) {
            val jadwalTerdekat = jadwalList.first()
            fokusItems.add("• Ada kelas **${jadwalTerdekat.namaMatkul}** jam ${jadwalTerdekat.jamMulai}.")
        }

        // 2. Filter tugas yang deadline-nya hari ini (dengan aman)
        val todayCalendar = Calendar.getInstance()
        val tugasHariIni = tugasList.filter { tugas ->
            // Cek apakah deadline tidak null sebelum digunakan
            tugas.deadline?.let { deadlineTimestamp ->
                val deadlineCalendar = Calendar.getInstance().apply { time = deadlineTimestamp.toDate() }
                todayCalendar.get(Calendar.YEAR) == deadlineCalendar.get(Calendar.YEAR) &&
                        todayCalendar.get(Calendar.DAY_OF_YEAR) == deadlineCalendar.get(Calendar.DAY_OF_YEAR)
            } ?: false // Jika deadline null, anggap false
        }

        // 3. Tambahkan info tugas terdekat jika ada
        if (tugasHariIni.isNotEmpty()) {
            val tugasTerdekat = tugasHariIni.first()
            fokusItems.add("• Deadline tugas **${tugasTerdekat.namaTugas}** hari ini.")
        }

        // 4. Jika tidak ada fokus sama sekali, tampilkan pesan santai
        if (fokusItems.isEmpty()) {
            // Kartu akan tetap disembunyikan jika tidak ada apa-apa
            cardFokusHariIni.visibility = View.GONE
        } else {
            // Jika ada fokus, gabungkan pesannya dan tampilkan kartunya
            tvFokusKonten.text = fokusItems.joinToString("\n")
            cardFokusHariIni.visibility = View.VISIBLE
        }
    }

    private fun setupAnalisisMingguan(userId: String) {
        val cal = Calendar.getInstance()
        if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
            cardAnalisisMingguan.visibility = View.VISIBLE
            val oneWeekAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
            val startOfThisWeek = Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, Calendar.MONDAY); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0) }

            db.collection("users").document(userId).collection("tugas")
                .whereEqualTo("completed", true)
                .whereGreaterThanOrEqualTo("completedAt", Timestamp(oneWeekAgo.time))
                .whereLessThan("completedAt", Timestamp(startOfThisWeek.time))
                .get()
                .addOnSuccessListener { documents ->
                    val completedCount = documents.size()
                    tvAnalisisKonten.text = if (completedCount > 0) {
                        getString(R.string.weekly_analysis, completedCount)
                    } else {
                        getString(R.string.weekly_analysis_empty)
                    }
                }
        } else {
            cardAnalisisMingguan.visibility = View.GONE
        }
    }

    private fun setupRekomendasiBelajar(userId: String) {
        val now = Calendar.getInstance()
        val todayStr = SimpleDateFormat("EEEE", Locale("id", "ID")).format(now.time)

        db.collection("users").document(userId).collection("jadwal")
            .whereEqualTo("hari", todayStr)
            .orderBy("jamMulai")
            .get()
            .addOnSuccessListener { jadwalDocs ->
                val jadwalHariIni = jadwalDocs.toObjects(MataKuliah::class.java)
                val busySlots = mutableListOf<Pair<Calendar, Calendar>>()
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                for (jadwal in jadwalHariIni) {
                    if (jadwal.jamMulai.isNullOrEmpty() || jadwal.jamSelesai.isNullOrEmpty()) continue
                    val startCal = Calendar.getInstance().apply { time = timeFormat.parse(jadwal.jamMulai) ?: Date() }
                    val endCal = Calendar.getInstance().apply { time = timeFormat.parse(jadwal.jamSelesai) ?: Date() }
                    val busyStart = (now.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, startCal.get(Calendar.HOUR_OF_DAY)); set(Calendar.MINUTE, startCal.get(Calendar.MINUTE)) }
                    val busyEnd = (now.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, endCal.get(Calendar.HOUR_OF_DAY)); set(Calendar.MINUTE, endCal.get(Calendar.MINUTE)) }
                    busySlots.add(Pair(busyStart, busyEnd))
                }

                val freeSlots = mutableListOf<Pair<Calendar, Long>>()
                val dayStart = (now.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, 8); set(Calendar.MINUTE, 0) }
                val dayEnd = (now.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, 22); set(Calendar.MINUTE, 0) }

                var lastBusyTime = dayStart
                for (slot in busySlots.sortedBy { it.first }) {
                    if (slot.first.after(lastBusyTime)) {
                        val duration = TimeUnit.MILLISECONDS.toMinutes(slot.first.timeInMillis - lastBusyTime.timeInMillis)
                        if (duration >= 90) {
                            freeSlots.add(Pair(lastBusyTime.clone() as Calendar, duration))
                        }
                    }
                    lastBusyTime = slot.second
                }

                if (dayEnd.after(lastBusyTime)) {
                    val duration = TimeUnit.MILLISECONDS.toMinutes(dayEnd.timeInMillis - lastBusyTime.timeInMillis)
                    if (duration >= 90) {
                        freeSlots.add(Pair(lastBusyTime.clone() as Calendar, duration))
                    }
                }

                val firstFreeSlot = freeSlots.firstOrNull { it.first.after(now) }
                if (firstFreeSlot != null && tugasList.isNotEmpty()) {
                    val tugasUntukDikerjakan = tugasList.first()
                    val jam = SimpleDateFormat("HH:mm", Locale.getDefault()).format(firstFreeSlot.first.time)
                    val jamDurasi = firstFreeSlot.second / 60
                    val menitDurasi = firstFreeSlot.second % 60
                    var durasiText = if(jamDurasi > 0) "$jamDurasi jam" else ""
                    if(menitDurasi > 0) durasiText += " $menitDurasi menit"

                    tvRekomendasiKonten.text = getString(R.string.study_recommendation, durasiText.trim(), jam, tugasUntukDikerjakan.namaTugas)
                    cardRekomendasiBelajar.visibility = View.VISIBLE
                } else {
                    cardRekomendasiBelajar.visibility = View.GONE
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
        if (auth.currentUser != null) {
            fetchAllData()
        }
    }
}