package com.ridwanelly.ingatin

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
    private lateinit var spinnerNotifikasi: Spinner

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var matkulId: String? = null

    private var deadlineCalendar = Calendar.getInstance()

    // Launcher untuk meminta izin notifikasi.
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Izin diberikan, lanjutkan menyimpan tugas.
                Toast.makeText(this, "Izin notifikasi diberikan.", Toast.LENGTH_SHORT).show()
                proceedToSaveTugas()
            } else {
                // Izin ditolak, beri tahu pengguna.
                Toast.makeText(
                    this,
                    "Izin notifikasi ditolak. Pengingat tidak akan muncul.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

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
        setupSpinner()

        btnPilihDeadline.setOnClickListener {
            showDateTimePicker()
        }

        btnSimpanTugas.setOnClickListener {
            checkAndSaveTugas()
        }
    }

    private fun initViews() {
        etNamaTugas = findViewById(R.id.etNamaTugas)
        etDeskripsiTugas = findViewById(R.id.etDeskripsiTugas)
        btnPilihDeadline = findViewById(R.id.btnPilihDeadline)
        tvDeadlineTerpilih = findViewById(R.id.tvDeadlineTerpilih)
        btnSimpanTugas = findViewById(R.id.btnSimpanTugas)
        spinnerNotifikasi = findViewById(R.id.spinnerNotifikasi)
    }

    private fun setupSpinner() {
        ArrayAdapter.createFromResource(
            this,
            R.array.reminder_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerNotifikasi.adapter = adapter
        }
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
        val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
        val deadlineText = getString(R.string.deadline_format, sdf.format(deadlineCalendar.time))
        tvDeadlineTerpilih.text = deadlineText
    }

    private fun checkAndSaveTugas() {
        // Periksa hanya untuk Android 13 (API 33) ke atas
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    proceedToSaveTugas()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    Toast.makeText(this, "Izin notifikasi diperlukan untuk fitur pengingat.", Toast.LENGTH_LONG).show()
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Untuk Android versi di bawah 13, izin tidak diperlukan.
            proceedToSaveTugas()
        }
    }


    private fun proceedToSaveTugas() {
        val namaTugas = etNamaTugas.text.toString().trim()
        val deskripsi = etDeskripsiTugas.text.toString().trim()
        val deadlineTimestamp = Timestamp(deadlineCalendar.time)
        val userId = auth.currentUser?.uid
        val reminderOption = spinnerNotifikasi.selectedItemPosition

        if (namaTugas.isEmpty() || userId == null) {
            Toast.makeText(this, "Nama tugas dan deadline harus diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        if (deadlineCalendar.timeInMillis <= System.currentTimeMillis()) {
            Toast.makeText(this, "Deadline tidak boleh di masa lalu.", Toast.LENGTH_SHORT).show()
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
                scheduleNotification(namaTugas, deskripsi, deadlineCalendar.timeInMillis, reminderOption)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun scheduleNotification(taskName: String, taskDescription: String, deadlineMillis: Long, reminderOption: Int) {
        val currentTime = System.currentTimeMillis()

        val reminderMillis = when (reminderOption) {
            0 -> TimeUnit.HOURS.toMillis(1)
            1 -> TimeUnit.DAYS.toMillis(1)
            2 -> TimeUnit.DAYS.toMillis(2)
            else -> TimeUnit.HOURS.toMillis(1)
        }

        val notificationTime = deadlineMillis - reminderMillis

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
            Toast.makeText(this, "Pengingat diatur!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Waktu pengingat sudah lewat, tidak dapat dijadwalkan.", Toast.LENGTH_LONG).show()
        }
    }
}