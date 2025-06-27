package com.ridwanelly.ingatin

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ridwanelly.ingatin.models.MataKuliah
import com.ridwanelly.ingatin.workers.NotificationWorker
import java.util.*
import java.util.concurrent.TimeUnit

class AddJadwalActivity : AppCompatActivity() {

    private lateinit var etNamaMatkul: TextInputEditText
    private lateinit var etDosen: TextInputEditText
    private lateinit var spinnerHari: Spinner
    private lateinit var btnJamMulai: Button
    private lateinit var tvJamMulai: TextView
    private lateinit var btnJamSelesai: Button
    private lateinit var tvJamSelesai: TextView
    private lateinit var etRuangan: TextInputEditText
    private lateinit var btnSimpanJadwal: Button
    private lateinit var swIngatkanSaya: SwitchMaterial // <-- Variabel baru

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_jadwal)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        initViews()
        setupSpinner()

        btnJamMulai.setOnClickListener { showTimePicker(tvJamMulai) }
        btnJamSelesai.setOnClickListener { showTimePicker(tvJamSelesai) }
        btnSimpanJadwal.setOnClickListener { saveJadwal() }
    }

    private fun initViews() {
        etNamaMatkul = findViewById(R.id.etNamaMatkul)
        etDosen = findViewById(R.id.etDosen)
        spinnerHari = findViewById(R.id.spinnerHari)
        btnJamMulai = findViewById(R.id.btnJamMulai)
        tvJamMulai = findViewById(R.id.tvJamMulai)
        btnJamSelesai = findViewById(R.id.btnJamSelesai)
        tvJamSelesai = findViewById(R.id.tvJamSelesai)
        etRuangan = findViewById(R.id.etRuangan)
        btnSimpanJadwal = findViewById(R.id.btnSimpanJadwal)
        swIngatkanSaya = findViewById(R.id.swIngatkanSaya) // <-- Inisialisasi Switch
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.hari_array, android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerHari.adapter = adapter
    }

    private fun showTimePicker(textView: TextView) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        TimePickerDialog(this, { _, h, m ->
            textView.text = String.format(Locale.getDefault(), "%02d:%02d", h, m)
        }, hour, minute, true).show()
    }

    private fun saveJadwal() {
        val userId = auth.currentUser?.uid ?: return
        val jadwalId = db.collection("users").document(userId).collection("jadwal").document().id

        val mataKuliah = MataKuliah(
            id = jadwalId,
            namaMatkul = etNamaMatkul.text.toString(),
            dosen = etDosen.text.toString(),
            hari = spinnerHari.selectedItem.toString(),
            jamMulai = tvJamMulai.text.toString(),
            jamSelesai = tvJamSelesai.text.toString(),
            ruangan = etRuangan.text.toString(),
            userId = userId
        )

        if (mataKuliah.namaMatkul.isNullOrEmpty() || mataKuliah.jamMulai.isNullOrEmpty()) {
            Toast.makeText(this, "Nama mata kuliah dan jam mulai harus diisi", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users").document(userId).collection("jadwal").document(jadwalId)
            .set(mataKuliah)
            .addOnSuccessListener {
                Toast.makeText(this, "Jadwal berhasil disimpan!", Toast.LENGTH_SHORT).show()
                // Jadwalkan atau batalkan notifikasi berdasarkan pilihan user
                scheduleOrCancelJadwalReminder(mataKuliah, swIngatkanSaya.isChecked)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // --- FUNGSI BARU UNTUK MENJADWALKAN NOTIFIKASI ---
    private fun scheduleOrCancelJadwalReminder(mataKuliah: MataKuliah, isEnabled: Boolean) {
        val workManager = WorkManager.getInstance(this)
        val workTag = mataKuliah.id ?: return // Gunakan ID unik dari Firestore sebagai tag

        if (isEnabled) {
            val (dayOfWeek, hour, minute) = getScheduleTime(mataKuliah)
            if (dayOfWeek == -1) return // Waktu tidak valid

            val now = Calendar.getInstance()
            val scheduledTime = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, dayOfWeek)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                // Jika waktu sudah lewat untuk minggu ini, set untuk minggu depan
                if (before(now)) {
                    add(Calendar.WEEK_OF_YEAR, 1)
                }
            }

            // Pengingat 30 menit sebelum kelas dimulai
            scheduledTime.add(Calendar.MINUTE, -30)

            val initialDelay = scheduledTime.timeInMillis - now.timeInMillis

            val data = Data.Builder()
                .putString("TASK_NAME", "Kelas Segera Dimulai: ${mataKuliah.namaMatkul}")
                .putString("TASK_DESCRIPTION", "Di ${mataKuliah.ruangan} pada pukul ${mataKuliah.jamMulai}")
                .build()

            val periodicWorkRequest = PeriodicWorkRequestBuilder<NotificationWorker>(7, TimeUnit.DAYS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag(workTag)
                .build()

            workManager.enqueueUniquePeriodicWork(
                workTag, // Gunakan tag sebagai nama unik
                ExistingPeriodicWorkPolicy.REPLACE, // Ganti pengingat lama jika ada
                periodicWorkRequest
            )

            Toast.makeText(this, "Pengingat mingguan diaktifkan!", Toast.LENGTH_SHORT).show()

        } else {
            // Jika user tidak mau diingatkan, batalkan work request dengan tag yang sesuai
            workManager.cancelUniqueWork(workTag)
            Toast.makeText(this, "Pengingat mingguan dinonaktifkan.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getScheduleTime(mataKuliah: MataKuliah): Triple<Int, Int, Int> {
        val dayMap = mapOf(
            "Senin" to Calendar.MONDAY, "Selasa" to Calendar.TUESDAY, "Rabu" to Calendar.WEDNESDAY,
            "Kamis" to Calendar.THURSDAY, "Jumat" to Calendar.FRIDAY, "Sabtu" to Calendar.SATURDAY,
            "Minggu" to Calendar.SUNDAY
        )
        val dayOfWeek = dayMap[mataKuliah.hari] ?: return Triple(-1, -1, -1)
        val timeParts = mataKuliah.jamMulai?.split(":")?.map { it.toInt() } ?: return Triple(-1, -1, -1)
        return Triple(dayOfWeek, timeParts[0], timeParts[1])
    }
}