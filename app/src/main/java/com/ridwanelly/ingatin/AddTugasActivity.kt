package com.ridwanelly.ingatin

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ridwanelly.ingatin.models.Tugas
import com.ridwanelly.ingatin.workers.NotificationWorker
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class AddTugasActivity : AppCompatActivity() {

    private lateinit var etNamaTugas: TextInputEditText
    private lateinit var etDeskripsiTugas: TextInputEditText
    private lateinit var btnPilihDeadline: Button
    private lateinit var tvDeadlineTerpilih: TextView
    private lateinit var btnSimpanTugas: Button

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var matkulId: String? = null

    private var deadlineCalendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_tugas)

        matkulId = intent.getStringExtra("MATKUL_ID")
        if (matkulId == null) {
            Toast.makeText(this, "Error: ID Mata Kuliah tidak ditemukan.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        initViews()

        btnPilihDeadline.setOnClickListener {
            showDateTimePicker()
        }

        btnSimpanTugas.setOnClickListener {
            saveTugas()
        }
    }

    private fun initViews() {
        etNamaTugas = findViewById(R.id.etNamaTugas)
        etDeskripsiTugas = findViewById(R.id.etDeskripsiTugas)
        btnPilihDeadline = findViewById(R.id.btnPilihDeadline)
        tvDeadlineTerpilih = findViewById(R.id.tvDeadlineTerpilih)
        btnSimpanTugas = findViewById(R.id.btnSimpanTugas)
    }

    private fun showDateTimePicker() {
        val currentCalendar = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            deadlineCalendar.set(Calendar.YEAR, year)
            deadlineCalendar.set(Calendar.MONTH, month)
            deadlineCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            TimePickerDialog(this, { _, hourOfDay, minute ->
                deadlineCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                deadlineCalendar.set(Calendar.MINUTE, minute)
                updateDeadlineTextView()
            }, currentCalendar.get(Calendar.HOUR_OF_DAY), currentCalendar.get(Calendar.MINUTE), true).show()
        }, currentCalendar.get(Calendar.YEAR), currentCalendar.get(Calendar.MONTH), currentCalendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun updateDeadlineTextView() {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        tvDeadlineTerpilih.text = "Deadline: ${sdf.format(deadlineCalendar.time)}"
    }

    private fun saveTugas() {
        val namaTugas = etNamaTugas.text.toString().trim()
        val deskripsi = etDeskripsiTugas.text.toString().trim()
        val deadlineTimestamp = Timestamp(deadlineCalendar.time)
        val userId = auth.currentUser?.uid

        if (namaTugas.isEmpty() || userId == null) {
            Toast.makeText(this, "Nama tugas dan deadline harus diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        val tugasBaru = Tugas(
            namaTugas = namaTugas,
            deskripsi = deskripsi,
            deadline = deadlineTimestamp,
            isCompleted = false,
            matkulId = matkulId,
            userId = userId
        )

        db.collection("users").document(userId).collection("tugas")
            .add(tugasBaru)
            .addOnSuccessListener {
                Toast.makeText(this, "Tugas berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                // Jadwalkan notifikasi setelah berhasil menyimpan
                scheduleNotification(namaTugas, deskripsi, deadlineCalendar.timeInMillis)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun scheduleNotification(taskName: String, taskDescription: String, deadlineMillis: Long) {
        val currentTime = System.currentTimeMillis()
        // Kita ingin notifikasi muncul 1 jam sebelum deadline
        val oneHourInMillis = TimeUnit.HOURS.toMillis(1)
        val notificationTime = deadlineMillis - oneHourInMillis

        // Hanya jadwalkan jika waktu notifikasi masih di masa depan
        if (notificationTime > currentTime) {
            val delay = notificationTime - currentTime

            val data = Data.Builder()
                .putString("TASK_NAME", "Reminder: $taskName")
                .putString("TASK_DESCRIPTION", taskDescription)
                .build()

            val notificationWork = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .build()

            WorkManager.getInstance(this).enqueue(notificationWork)
        }
    }
}