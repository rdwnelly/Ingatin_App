package com.ridwanelly.ingatin

import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.ridwanelly.ingatin.models.Tugas

class EditTugasActivity : AppCompatActivity() {

    private lateinit var etEditNamaTugas: EditText
    private lateinit var etEditDeskripsiTugas: EditText
    private lateinit var btnUpdateTugas: Button

    private lateinit var db: FirebaseFirestore
    private var tugas: Tugas? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_tugas)

        db = FirebaseFirestore.getInstance()

        // Cara baru untuk mengambil objek Parcelable
        tugas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("TUGAS_EXTRA", Tugas::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("TUGAS_EXTRA")
        }

        if (tugas == null) {
            Toast.makeText(this, "Gagal memuat tugas", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        populateFields()

        btnUpdateTugas.setOnClickListener {
            updateTugas()
        }
    }

    private fun initViews() {
        etEditNamaTugas = findViewById(R.id.etEditNamaTugas)
        etEditDeskripsiTugas = findViewById(R.id.etEditDeskripsiTugas)
        btnUpdateTugas = findViewById(R.id.btnUpdateTugas)
    }

    private fun populateFields() {
        etEditNamaTugas.setText(tugas?.namaTugas)
        etEditDeskripsiTugas.setText(tugas?.deskripsi)
    }

    private fun updateTugas() {
        val newNama = etEditNamaTugas.text.toString().trim()
        val newDeskripsi = etEditDeskripsiTugas.text.toString().trim()

        if (newNama.isEmpty()) {
            Toast.makeText(this, "Nama tugas tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        val tugasRef = db.collection("users").document(tugas!!.userId!!).collection("tugas").document(tugas!!.id!!)
        tugasRef.update("namaTugas", newNama, "deskripsi", newDeskripsi)
            .addOnSuccessListener {
                Toast.makeText(this, "Tugas berhasil diperbarui", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memperbarui tugas", Toast.LENGTH_SHORT).show()
            }
    }
}