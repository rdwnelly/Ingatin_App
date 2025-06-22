package com.ridwanelly.ingatin

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ridwanelly.ingatin.models.MataKuliah

class AddJadwalActivity : AppCompatActivity() {

    private lateinit var etNamaMatkul: TextInputEditText
    private lateinit var etDosen: TextInputEditText
    private lateinit var etRuangan: TextInputEditText
    private lateinit var etHari: TextInputEditText
    private lateinit var etJamMulai: TextInputEditText
    private lateinit var etJamSelesai: TextInputEditText
    private lateinit var btnSimpanJadwal: Button

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_jadwal)

        // Inisialisasi Firebase
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Hubungkan view dengan ID
        etNamaMatkul = findViewById(R.id.etNamaMatkul)
        etDosen = findViewById(R.id.etDosen)
        etRuangan = findViewById(R.id.etRuangan)
        etHari = findViewById(R.id.etHari)
        etJamMulai = findViewById(R.id.etJamMulai)
        etJamSelesai = findViewById(R.id.etJamSelesai)
        btnSimpanJadwal = findViewById(R.id.btnSimpanJadwal)

        btnSimpanJadwal.setOnClickListener {
            saveJadwal()
        }
    }

    private fun saveJadwal() {
        val namaMatkul = etNamaMatkul.text.toString().trim()
        val dosen = etDosen.text.toString().trim()
        val ruangan = etRuangan.text.toString().trim()
        val hari = etHari.text.toString().trim()
        val jamMulai = etJamMulai.text.toString().trim()
        val jamSelesai = etJamSelesai.text.toString().trim()
        val userId = auth.currentUser?.uid

        // Validasi input
        if (namaMatkul.isEmpty() || dosen.isEmpty() || ruangan.isEmpty() || hari.isEmpty() || jamMulai.isEmpty() || jamSelesai.isEmpty() || userId == null) {
            Toast.makeText(this, "Semua field harus diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        // Buat objek MataKuliah baru
        val jadwalBaru = MataKuliah(
            namaMatkul = namaMatkul,
            dosen = dosen,
            ruangan = ruangan,
            hari = hari,
            jamMulai = jamMulai,
            jamSelesai = jamSelesai,
            userId = userId
        )

        // Simpan ke Firestore
        // Kita buat struktur: collection "users" -> document (userId) -> collection "jadwal" -> document (jadwalId)
        // Ini memastikan data setiap user terisolasi.
        db.collection("users").document(userId).collection("jadwal")
            .add(jadwalBaru)
            .addOnSuccessListener {
                Toast.makeText(this, "Jadwal berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                finish() // Tutup activity dan kembali ke MainActivity
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}